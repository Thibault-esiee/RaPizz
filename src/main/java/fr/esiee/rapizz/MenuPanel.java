package fr.esiee.rapizz;

import java.awt.Image;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.JButton;

public class MenuPanel extends JPanel {

    private JTextField searchField;
    private JTable pizzaTable;
    private PizzaTableModel tableModel;
    private JTextArea detailArea;

    private JLabel imageLabel;

    private List<Pizza> pizzas = new ArrayList<>();
    private Map<Integer, Double> sizeAdjustments = new HashMap<>();
    private Map<Integer, String> sizeNames = new HashMap<>();

    public MenuPanel() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        topPanel.add(new JLabel("Recherche (nom ou ingrédient) : "), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new PizzaTableModel();
        pizzaTable = new JTable(tableModel);
        pizzaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(pizzaTable), BorderLayout.CENTER);

        JPanel detailPanel = new JPanel(new BorderLayout());
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setPreferredSize(new Dimension(0, 150));
        detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setPreferredSize(new Dimension(200, 150));
        detailPanel.add(imageLabel, BorderLayout.EAST);

        JButton printButton = new JButton("Imprimer le menu");
        printButton.addActionListener(e -> PrintMenuPDF.generate(pizzas, sizeAdjustments, sizeNames));
        detailPanel.add(printButton, BorderLayout.SOUTH);

        add(detailPanel, BorderLayout.SOUTH);

        loadSizeAdjustments();
        loadPizzas();

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterPizzas(); }
            public void removeUpdate(DocumentEvent e) { filterPizzas(); }
            public void changedUpdate(DocumentEvent e) { filterPizzas(); }
        });

        pizzaTable.getSelectionModel().addListSelectionListener(e -> {
            int row = pizzaTable.getSelectedRow();
            if (row >= 0) {
                Pizza pizza = tableModel.getPizzaAt(row);
                showPizzaDetails(pizza);
            }
        });
    }

    private void loadSizeAdjustments() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, size, price_adjust FROM pizza_sizes")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("size");
                double adjust = rs.getDouble("price_adjust");
                sizeNames.put(id, name);
                sizeAdjustments.put(id, adjust);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement tailles : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPizzas() {
        pizzas.clear();

        String sql = """
            SELECT p.id, p.name, p.price,
                   GROUP_CONCAT(i.name SEPARATOR ', ') AS ingredients
            FROM pizzas p
            JOIN ingredients_products ip ON ip.product_id = p.id
            JOIN ingredients i ON i.id = ip.ingredients_id
            GROUP BY p.id
            ORDER BY p.name
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                pizzas.add(new Pizza(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("ingredients")
                ));
            }

            tableModel.setPizzas(pizzas);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement pizzas : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterPizzas() {
        String text = searchField.getText().toLowerCase().trim();
        if (text.isEmpty()) {
            tableModel.setPizzas(pizzas);
            detailArea.setText("");
            return;
        }
        List<Pizza> filtered = new ArrayList<>();
        for (Pizza p : pizzas) {
            if (p.getName().toLowerCase().contains(text) || p.getIngredients().toLowerCase().contains(text)) {
                filtered.add(p);
            }
        }
        tableModel.setPizzas(filtered);
        detailArea.setText("");
    }

    private void showPizzaDetails(Pizza pizza) {
        StringBuilder sb = new StringBuilder();
        sb.append("🍕 ").append(pizza.getName()).append("\n\n");
        sb.append("Ingrédients : ").append(pizza.getIngredients()).append("\n\n");
        sb.append("Prix par taille :\n");

        List<String> orderedSizes = List.of("naine", "humaine", "ogresse");

        for (String sizeName : orderedSizes) {
            int sizeId = -1;
            for (Map.Entry<Integer, String> entry : sizeNames.entrySet()) {
                if (entry.getValue().equals(sizeName)) {
                    sizeId = entry.getKey();
                    break;
                }
            }
            if (sizeId != -1) {
                double adjust = sizeAdjustments.getOrDefault(sizeId, 0.0);
                double price = pizza.getBasePrice() * (1 + adjust);
                sb.append(String.format("  - %-7s : %.2f €\n", sizeName, price));
            }
        }

        detailArea.setText(sb.toString());
        String imagePath = "src/images/" + pizza.getName().replaceAll("\\s+", "") + ".jpeg";
        ImageIcon originalIcon = new ImageIcon(imagePath);
        int maxWidth = 200;
        int maxHeight = 150;

        int imgWidth = originalIcon.getIconWidth();
        int imgHeight = originalIcon.getIconHeight();

        double widthRatio = (double) maxWidth / imgWidth;
        double heightRatio = (double) maxHeight / imgHeight;
        double scale = Math.min(widthRatio, heightRatio);

        int scaledWidth = (int) (imgWidth * scale);
        int scaledHeight = (int) (imgHeight * scale);

        Image scaledImage = originalIcon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
    }

    private static class PizzaTableModel extends AbstractTableModel {
        private List<Pizza> pizzas = new ArrayList<>();
        private final String[] cols = {"Nom", "Prix de base (€)", "Ingrédients"};

        public void setPizzas(List<Pizza> pizzas) {
            this.pizzas = pizzas;
            fireTableDataChanged();
        }

        public Pizza getPizzaAt(int row) {
            return pizzas.get(row);
        }

        @Override
        public int getRowCount() {
            return pizzas.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int col) {
            return cols[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Pizza pizza = pizzas.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> pizza.getName();
                case 1 -> pizza.getBasePrice();
                case 2 -> pizza.getIngredients();
                default -> "";
            };
        }
    }

    public static class Pizza {
        private final int id;
        private final String name;
        private final double basePrice;
        private final String ingredients;

        public Pizza(int id, String name, double basePrice, String ingredients) {
            this.id = id;
            this.name = name;
            this.basePrice = basePrice;
            this.ingredients = ingredients;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public double getBasePrice() { return basePrice; }
        public String getIngredients() { return ingredients; }
    }
}