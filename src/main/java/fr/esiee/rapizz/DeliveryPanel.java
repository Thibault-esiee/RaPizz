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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import fr.esiee.rapizz.OrderDetailsDialog;
import fr.esiee.rapizz.Order;

public class DeliveryPanel extends JPanel {

    private static class DeliveryRecord {
        int orderId;
        String customer;
        String deliverer;
        String vehicle;
        LocalDateTime orderTime;
        LocalDateTime deliveryTime;
        double orderPrice;
        boolean isFree;
        int delayMinutes;
        String pizza;

        public DeliveryRecord(ResultSet rs) throws SQLException {
            this.orderId = rs.getInt("ID");
            this.customer = rs.getString("customer_name");
            this.deliverer = rs.getString("deliverer_name");
            this.vehicle = rs.getString("vehicle_type");
            
            Timestamp orderTs = rs.getTimestamp("order_time");
            this.orderTime = orderTs != null ? orderTs.toLocalDateTime() : null;
            
            Timestamp deliveryTs = rs.getTimestamp("delivery_time");
            this.deliveryTime = deliveryTs != null ? deliveryTs.toLocalDateTime() : null;
            
            this.orderPrice = rs.getDouble("order_price");
            this.isFree = rs.getBoolean("is_free");
            this.delayMinutes = rs.getInt("delay_minutes");
            this.pizza = rs.getString("pizza_name");
        }
    }

    private class DeliveryTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            "ID", "Client", "Livreur", "Véhicule", "Heure commande", 
            "Heure livraison", "Prix", "Gratuit", "Delai de livraison", "Pizza"
        };
        private List<DeliveryRecord> records = new ArrayList<>();
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        @Override
        public int getRowCount() {
            return records.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DeliveryRecord record = records.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> record.orderId;
                case 1 -> record.customer;
                case 2 -> record.deliverer;
                case 3 -> record.vehicle;
                case 4 -> record.orderTime != null ? record.orderTime.format(formatter) : "N/A";
                case 5 -> record.deliveryTime != null ? record.deliveryTime.format(formatter) : "En cours";
                case 6 -> String.format("%.2f €", record.orderPrice);
                case 7 -> record.isFree ? "Oui" : "Non";
                case 8 -> record.delayMinutes;
                case 9 -> record.pizza;
                default -> null;
            };
        }

        public void setRecords(List<DeliveryRecord> records) {
            this.records = records;
            fireTableDataChanged();
        }
    }

    private JTable table;
    private DeliveryTableModel tableModel;
    private JComboBox<String> delivererFilter;
    private DatePicker datePicker;
    private JButton refreshButton;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel pageLabel;
    private JButton detailsButton;
    private int currentPage = 1;
    private final int pageSize = 10;
    private int totalRows = 0;

    public DeliveryPanel() {
        setLayout(new BorderLayout());

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
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        prevPageButton = new JButton("← Précédent");
        nextPageButton = new JButton("Suivant →");
        pageLabel = new JLabel("Page 1");

        paginationPanel.add(prevPageButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextPageButton);

        detailsButton = new JButton("Détail commande");
        detailsButton.setEnabled(false);
        paginationPanel.add(detailsButton);

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

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean selected = table.getSelectedRow() != -1;
                detailsButton.setEnabled(selected);
            }
        });

        detailsButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) return;
            int modelRow = table.convertRowIndexToModel(selectedRow);
            DeliveryRecord record = tableModel.records.get(modelRow);
            Order order = new Order(
                record.orderId,
                record.orderTime != null ? Timestamp.valueOf(record.orderTime) : null,
                record.deliveryTime != null ? Timestamp.valueOf(record.deliveryTime) : null,
                record.orderPrice,
                !record.isFree,
                record.pizza,
                record.deliverer,
                record.vehicle,
                record.customer,
                record.delayMinutes,
                record.orderPrice
            );
            OrderDetailsDialog.show(this, order);
        });
    }

    public void refreshData() {
        currentPage = 1;
        String deliverer = delivererFilter.getSelectedItem().toString();
        if (deliverer.equals("Tous")) deliverer = null;
        LocalDate date = datePicker.getDate();
        loadData(deliverer, date, currentPage);
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
            SELECT o.ID, c.name as customer_name, d.name as deliverer_name, 
                   v.type as vehicle_type, o.order_time, o.delivery_time, 
                   o.order_price, o.is_free, o.delay_minutes,
                   p.name as pizza_name
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
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            int paramIndex = 1;
            if (delivererFilter != null) {
                stmt.setString(paramIndex++, delivererFilter);
            }
            if (dateFilter != null) {
                stmt.setDate(paramIndex++, java.sql.Date.valueOf(dateFilter));
            }
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex, (page - 1) * pageSize);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new DeliveryRecord(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Erreur lors du chargement des données : " + e.getMessage(),
                "Erreur", 
                JOptionPane.ERROR_MESSAGE);
        }

        tableModel.setRecords(records);
        updatePaginationControls();

        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune donnée trouvée pour ces filtres.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updatePaginationControls() {
        int maxPage = (int) Math.ceil((double) totalRows / pageSize);
        pageLabel.setText("Page " + currentPage + " / " + maxPage);
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < maxPage);
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