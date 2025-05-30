package fr.esiee.rapizz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

public class DeliveryPanel extends JPanel {

    private JTable table;
    private DeliveryTableModel tableModel;

    private JComboBox<String> delivererFilter;
    private DatePicker datePicker;
    private JButton refreshButton;

    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel pageLabel;

    private int currentPage = 1;
    private final int pageSize = 10;
    private int totalRows = 0;

    public DeliveryPanel() {
        setLayout(new BorderLayout());

        // === Filters Panel ===
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filterPanel.add(new JLabel("Livreur :"));
        delivererFilter = new JComboBox<>();
        delivererFilter.addItem("Tous");
        loadDeliverers();
        filterPanel.add(delivererFilter);

        filterPanel.add(new JLabel("Date :"));
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setAllowEmptyDates(true);
        datePicker = new DatePicker(dateSettings);
        filterPanel.add(datePicker);

        refreshButton = new JButton("Actualiser");
        filterPanel.add(refreshButton);

        add(filterPanel, BorderLayout.NORTH);

        tableModel = new DeliveryTableModel();
        table = new JTable(tableModel);

        table.setAutoCreateRowSorter(true);

        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new AlternatingRowColorRenderer());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        prevPageButton = new JButton("← Précédent");
        nextPageButton = new JButton("Suivant →");
        pageLabel = new JLabel("Page 1");

        paginationPanel.add(prevPageButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextPageButton);

        add(paginationPanel, BorderLayout.SOUTH);

        loadData(null, null, 1);

        refreshButton.addActionListener(e -> {
            currentPage = 1;
            String deliverer = delivererFilter.getSelectedItem().toString();
            if (deliverer.equals("Tous")) deliverer = null;
            LocalDate date = datePicker.getDate();

            loadData(deliverer, date, currentPage);
        });

        prevPageButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                reloadCurrentPage();
            }
        });

        nextPageButton.addActionListener(e -> {
            int maxPage = (int) Math.ceil((double) totalRows / pageSize);
            if (currentPage < maxPage) {
                currentPage++;
                reloadCurrentPage();
            }
        });
    }

    private void reloadCurrentPage() {
        String deliverer = delivererFilter.getSelectedItem().toString();
        if (deliverer.equals("Tous")) deliverer = null;
        LocalDate date = datePicker.getDate();
        loadData(deliverer, date, currentPage);
    }

    private void loadDeliverers() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM deliverers ORDER BY name")) {
            while (rs.next()) {
                delivererFilter.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement livreurs : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadData(String delivererFilter, LocalDate dateFilter, int page) {
        List<DeliveryRecord> records = new ArrayList<>();

        totalRows = countTotalRows(delivererFilter, dateFilter);

        StringBuilder query = new StringBuilder("""
            SELECT d.name AS deliverer, v.type AS vehicle, c.name AS customer, 
                   o.order_time, o.delivery_time, o.delay_minutes,
                   p.name AS pizza, p.price AS base_price
            FROM orders o
            JOIN deliverers d ON o.deliverer_id = d.ID
            JOIN vehicles v ON o.vehicle_id = v.ID
            JOIN customers c ON o.customer_id = c.ID
            JOIN orders_products op ON op.order_id = o.ID
            JOIN pizzas p ON op.product_id = p.id
            WHERE 1=1
            """);

        if (delivererFilter != null) {
            query.append(" AND d.name = ?");
        }
        if (dateFilter != null) {
            query.append(" AND DATE(o.order_time) = ?");
        }

        query.append(" ORDER BY o.order_time DESC LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query.toString())) {

            int idx = 1;
            if (delivererFilter != null) {
                ps.setString(idx++, delivererFilter);
            }
            if (dateFilter != null) {
                ps.setDate(idx++, java.sql.Date.valueOf(dateFilter));
            }
            ps.setInt(idx++, pageSize);
            ps.setInt(idx, (page - 1) * pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new DeliveryRecord(
                            rs.getString("deliverer"),
                            rs.getString("vehicle"),
                            rs.getString("customer"),
                            rs.getTimestamp("order_time"),
                            rs.getTimestamp("delivery_time"),
                            rs.getInt("delay_minutes"),
                            rs.getString("pizza"),
                            rs.getDouble("base_price")
                    ));
                }
            }

            tableModel.setRecords(records);

            int maxPage = (int) Math.ceil((double) totalRows / pageSize);
            pageLabel.setText("Page " + currentPage + " / " + maxPage);
            prevPageButton.setEnabled(currentPage > 1);
            nextPageButton.setEnabled(currentPage < maxPage);

            if (records.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Aucune donnée trouvée pour ces filtres.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement fiches livraison : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int countTotalRows(String delivererFilter, LocalDate dateFilter) {
        StringBuilder countQuery = new StringBuilder("""
            SELECT COUNT(*)
            FROM orders o
            JOIN deliverers d ON o.deliverer_id = d.ID
            WHERE 1=1
            """);
        if (delivererFilter != null) {
            countQuery.append(" AND d.name = ?");
        }
        if (dateFilter != null) {
            countQuery.append(" AND DATE(o.order_time) = ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(countQuery.toString())) {

            int idx = 1;
            if (delivererFilter != null) {
                ps.setString(idx++, delivererFilter);
            }
            if (dateFilter != null) {
                ps.setDate(idx++, java.sql.Date.valueOf(dateFilter));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur comptage total : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        return 0;
    }

    private static class DeliveryRecord {
        final String deliverer;
        final String vehicle;
        final String customer;
        final Timestamp orderTime;
        final Timestamp deliveryTime;
        final int delayMinutes;
        final String pizza;
        final double basePrice;

        DeliveryRecord(String deliverer, String vehicle, String customer, Timestamp orderTime, Timestamp deliveryTime, int delayMinutes, String pizza, double basePrice) {
            this.deliverer = deliverer;
            this.vehicle = vehicle;
            this.customer = customer;
            this.orderTime = orderTime;
            this.deliveryTime = deliveryTime;
            this.delayMinutes = delayMinutes;
            this.pizza = pizza;
            this.basePrice = basePrice;
        }
    }

    private static class DeliveryTableModel extends AbstractTableModel {
        private List<DeliveryRecord> records = new ArrayList<>();
        private final String[] columns = {"Livreur", "Véhicule", "Client", "Date commande", "Date livraison", "Retard (min)", "Pizza", "Prix (€)"};

        public void setRecords(List<DeliveryRecord> records) {
            this.records = records;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return records.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DeliveryRecord rec = records.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rec.deliverer;
                case 1 -> rec.vehicle;
                case 2 -> rec.customer;
                case 3 -> rec.orderTime;
                case 4 -> rec.deliveryTime;
                case 5 -> rec.delayMinutes;
                case 6 -> rec.pizza;
                case 7 -> rec.basePrice;
                default -> null;
            };
        }
    }

    private static class AlternatingRowColorRenderer extends DefaultTableCellRenderer {
        private static final Color EVEN_COLOR = new Color(240, 240, 240);
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                if (row % 2 == 0) {
                    c.setBackground(EVEN_COLOR);
                } else {
                    c.setBackground(Color.WHITE);
                }
            }
            return c;
        }
    }
}