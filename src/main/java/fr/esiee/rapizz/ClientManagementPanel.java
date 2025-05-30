package fr.esiee.rapizz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import fr.esiee.rapizz.OrderDetailsDialog;
import fr.esiee.rapizz.Order;

public class ClientManagementPanel extends JPanel {

    private JTable clientTable;
    private ClientTableModel clientTableModel;

    private JTextField nameField;
    private JTextField addressField;
    private JTextField balanceField;

    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;

    private JTable ordersTable;
    private OrdersTableModel ordersTableModel;

    private JButton detailsButton;

    private int selectedClientId = -1;

    public ClientManagementPanel() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        clientTableModel = new ClientTableModel();
        clientTable = new JTable(clientTableModel);
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topPanel.add(new JScrollPane(clientTable), BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Gestion Client"));

        formPanel.add(new JLabel("Nom :"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Adresse :"));
        addressField = new JTextField();
        formPanel.add(addressField);

        formPanel.add(new JLabel("Solde (€) :"));
        balanceField = new JTextField();
        formPanel.add(balanceField);

        addButton = new JButton("Ajouter");
        updateButton = new JButton("Modifier");
        deleteButton = new JButton("Supprimer");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(formPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        ordersTableModel = new OrdersTableModel();
        ordersTable = new JTable(ordersTableModel);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        detailsButton = new JButton("Voir plus");
        detailsButton.setEnabled(false);
        JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        detailsPanel.add(detailsButton);
        add(detailsPanel, BorderLayout.SOUTH);

        loadClients();

        clientTable.getSelectionModel().addListSelectionListener(e -> onClientSelected());
        addButton.addActionListener(e -> addClient());
        updateButton.addActionListener(e -> updateClient());
        deleteButton.addActionListener(e -> deleteClient());

        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = ordersTable.getSelectedRow() >= 0;
            detailsButton.setEnabled(selected);
        });

        detailsButton.addActionListener(e -> {
            int selectedRow = ordersTable.getSelectedRow();
            if (selectedRow >= 0) {
                Order order = ordersTableModel.getOrderAt(selectedRow);
                OrderDetailsDialog.show(this, order);
            }
        });
    }

    private void loadClients() {
        List<Client> clients = new ArrayList<>();
        String sql = """
            SELECT c.ID, c.name, c.address, c.balance, s.name AS subscription
            FROM customers c
            LEFT JOIN customers_subscriptions cs ON c.ID = cs.customer_id
            LEFT JOIN subscription s ON cs.subscription_id = s.ID
            ORDER BY c.name
            """;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clients.add(new Client(
                        rs.getInt("ID"),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getDouble("balance"),
                        rs.getString("subscription")
                ));
            }
            clientTableModel.setClients(clients);
            clearForm();
            ordersTableModel.setOrders(new ArrayList<>());
        } catch (SQLException e) {
            showError("Erreur chargement clients : " + e.getMessage());
        }
    }

    private void onClientSelected() {
        int selectedRow = clientTable.getSelectedRow();
        if (selectedRow < 0) {
            selectedClientId = -1;
            clearForm();
            ordersTableModel.setOrders(new ArrayList<>());
            detailsButton.setEnabled(false);
            return;
        }
        Client client = clientTableModel.getClientAt(selectedRow);
        selectedClientId = client.getId();
        nameField.setText(client.getName());
        addressField.setText(client.getAddress());
        balanceField.setText(String.valueOf(client.getBalance()));

        loadOrdersForClient(selectedClientId);
    }

    private void loadOrdersForClient(int clientId) {
        List<Order> orders = new ArrayList<>();
        String sql = """
            SELECT o.ID, o.order_time, o.delivery_time, o.order_price, o.is_free,
                   p.name AS pizza, p.price AS base_price,
                   d.name AS deliverer_name,
                   v.type AS vehicle_type,
                   c.name AS client_name,
                   o.delay_minutes
            FROM orders o
            JOIN orders_products op ON o.ID = op.order_id
            JOIN pizzas p ON op.product_id = p.id
            JOIN customers c ON o.customer_id = c.ID
            LEFT JOIN deliverers d ON o.deliverer_id = d.ID
            LEFT JOIN vehicles v ON o.vehicle_id = v.ID
            WHERE o.customer_id = ?
            ORDER BY o.order_time DESC
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(new Order(
                        rs.getInt("ID"),
                        rs.getTimestamp("order_time"),
                        rs.getTimestamp("delivery_time"),
                        rs.getDouble("order_price"),
                        rs.getBoolean("is_free"),
                        rs.getString("pizza"),
                        rs.getString("deliverer_name"),
                        rs.getString("vehicle_type"),
                        rs.getString("client_name"),
                        rs.getInt("delay_minutes"),
                        rs.getDouble("base_price")
                    ));
                }
            }
            ordersTableModel.setOrders(orders);
            detailsButton.setEnabled(false);
        } catch (SQLException e) {
            showError("Erreur chargement commandes : " + e.getMessage());
        }
    }

    private void addClient() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String balanceStr = balanceField.getText().trim();

        if (name.isEmpty() || address.isEmpty() || balanceStr.isEmpty()) {
            showError("Tous les champs doivent être remplis.");
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            showError("Solde invalide.");
            return;
        }

        String sql = "INSERT INTO customers (name, address, balance) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setDouble(3, balance);
            ps.executeUpdate();
            loadClients();
            showInfo("Client ajouté avec succès.");
        } catch (SQLException e) {
            showError("Erreur ajout client : " + e.getMessage());
        }
    }

    private void updateClient() {
        if (selectedClientId < 0) {
            showError("Sélectionnez un client.");
            return;
        }
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String balanceStr = balanceField.getText().trim();

        if (name.isEmpty() || address.isEmpty() || balanceStr.isEmpty()) {
            showError("Tous les champs doivent être remplis.");
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            showError("Solde invalide.");
            return;
        }

        String sql = "UPDATE customers SET name = ?, address = ?, balance = ? WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setDouble(3, balance);
            ps.setInt(4, selectedClientId);
            ps.executeUpdate();
            loadClients();
            showInfo("Client modifié avec succès.");
        } catch (SQLException e) {
            showError("Erreur modification client : " + e.getMessage());
        }
    }

    private void deleteClient() {
        if (selectedClientId < 0) {
            showError("Sélectionnez un client.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Supprimer ce client ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM customers WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedClientId);
            ps.executeUpdate();
            loadClients();
            showInfo("Client supprimé.");
        } catch (SQLException e) {
            showError("Erreur suppression client : " + e.getMessage());
        }
    }

    private void clearForm() {
        nameField.setText("");
        addressField.setText("");
        balanceField.setText("");
        selectedClientId = -1;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class Client {
        private final int id;
        private final String name;
        private final String address;
        private final double balance;
        private final String subscriptionName;

        public Client(int id, String name, String address, double balance, String subscriptionName) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.balance = balance;
            this.subscriptionName = subscriptionName;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public double getBalance() { return balance; }
        public String getSubscriptionName() { return subscriptionName; }
    }

    private static class OrdersTableModel extends AbstractTableModel {
        private List<Order> orders = new ArrayList<>();
        private final String[] columns = {"Date commande", "Date livraison", "Pizza", "Prix (€)", "Gratuite"};

        public void setOrders(List<Order> orders) {
            this.orders = orders;
            fireTableDataChanged();
        }

        public Order getOrderAt(int row) {
            return orders.get(row);
        }

        @Override
        public int getRowCount() {
            return orders.size();
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
            Order o = orders.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> o.getOrderTime();
                case 1 -> o.getDeliveryTime();
                case 2 -> o.getPizza();
                case 3 -> o.getTotalPrice();
                case 4 -> !o.isPaid() ? "Oui" : "Non";
                default -> "";
            };
        }
    }

    private static class ClientTableModel extends AbstractTableModel {
        private List<Client> clients = new ArrayList<>();
        private final String[] columns = {"ID", "Nom", "Adresse", "Solde", "Abonnement"};

        public void setClients(List<Client> clients) {
            this.clients = clients;
            fireTableDataChanged();
        }

        public Client getClientAt(int row) {
            return clients.get(row);
        }

        @Override
        public int getRowCount() {
            return clients.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            Client c = clients.get(row);
            return switch (col) {
                case 0 -> c.getId();
                case 1 -> c.getName();
                case 2 -> c.getAddress();
                case 3 -> c.getBalance();
                case 4 -> c.getSubscriptionName();
                default -> "";
            };
        }
    }
}