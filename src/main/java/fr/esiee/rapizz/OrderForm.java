package fr.esiee.rapizz;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderForm extends JPanel {
    private JComboBox<CustomerItem> customerCombo;
    private JComboBox<PizzaItem> pizzaCombo;
    private JComboBox<SizeItem> sizeCombo;
    private JButton orderButton;
    private Connection connection;
    private DeliveryPanel deliveryPanel;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public OrderForm(Connection connection, DeliveryPanel deliveryPanel) {
        this.connection = connection;
        this.deliveryPanel = deliveryPanel;
        setLayout(new GridLayout(4, 2, 10, 10));
        initializeComponents();
        restorePendingOrders();
    }

    private void initializeComponents() {
        customerCombo = new JComboBox<>();
        pizzaCombo = new JComboBox<>();
        sizeCombo = new JComboBox<>();
        orderButton = new JButton("Passer la commande");

        loadCustomers();
        loadPizzas();
        loadSizes();

        pizzaCombo.addActionListener(e -> loadSizes());

        add(new JLabel("Client :"));
        add(customerCombo);
        add(new JLabel("Pizza :"));
        add(pizzaCombo);
        add(new JLabel("Taille :"));
        add(sizeCombo);
        add(new JLabel(""));
        add(orderButton);

        orderButton.addActionListener(e -> processOrder());
    }

    private void restorePendingOrders() {
        try (PreparedStatement stmt = connection.prepareStatement("""
                SELECT o.ID as order_id, o.customer_id, o.deliverer_id, o.vehicle_id,
                       o.order_time, o.expected_delivery_time, o.order_price,
                       c.name as customer_name, d.name as deliverer_name, v.type as vehicle_type
                FROM orders o
                JOIN customers c ON o.customer_id = c.ID
                JOIN deliverers d ON o.deliverer_id = d.ID
                JOIN vehicles v ON o.vehicle_id = v.ID
                WHERE o.delivery_time IS NULL
                """)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp orderTimeStamp = rs.getTimestamp("order_time");
                Timestamp expectedDeliveryTimeStamp = rs.getTimestamp("expected_delivery_time");
                
                if (orderTimeStamp == null || expectedDeliveryTimeStamp == null) {
                    System.err.println("Commande avec date manquante, ID: " + rs.getInt("order_id"));
                    continue;
                }
                
                LocalDateTime orderTime = orderTimeStamp.toLocalDateTime();
                LocalDateTime expectedDeliveryTime = expectedDeliveryTimeStamp.toLocalDateTime();
                
                long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), expectedDeliveryTime).toMinutes();
                
                if (remainingMinutes > 0) {
                    int orderId = rs.getInt("order_id");
                    int delivererId = rs.getInt("deliverer_id");
                    int customerId = rs.getInt("customer_id");
                    double price = rs.getDouble("order_price");
                    
                    scheduleDelivery(orderId, delivererId, customerId, price, orderTime, 
                                   (int) remainingMinutes);
                    
                    System.out.printf("Commande #%d restaurée - Livraison dans %d minutes%n", 
                                    orderId, remainingMinutes);
                } else {
                    finishDelivery(
                        rs.getInt("order_id"),
                        rs.getInt("deliverer_id"),
                        rs.getInt("customer_id"),
                        rs.getDouble("order_price"),
                        orderTime,
                        (int) java.time.Duration.between(orderTime, expectedDeliveryTime).toMinutes()
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Erreur lors de la restauration des commandes en cours : " + e.getMessage(),
                "Erreur", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scheduleDelivery(int orderId, int delivererId, int customerId, 
                                double price, LocalDateTime orderTime, int deliveryTime) {
        int[] remainingTime = {deliveryTime};
        scheduler.scheduleAtFixedRate(() -> {
            if (remainingTime[0] > 0) {
                System.out.printf("Commande #%d - Temps restant : %d minutes%n", 
                    orderId, remainingTime[0]);
                remainingTime[0]--;
            }
        }, 0, 1, TimeUnit.MINUTES);

        scheduler.schedule(() -> {
            try {
                System.out.printf("Commande #%d - Finalisation de la livraison...%n", orderId);
                finishDelivery(orderId, delivererId, customerId, price, orderTime, deliveryTime);
                System.out.printf("Commande #%d - Livraison finalisée avec succès !%n", orderId);
            } catch (SQLException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "Erreur lors de la finalisation de la livraison : " + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }, deliveryTime, TimeUnit.MINUTES);
    }

    private DelivererVehiclePair findAvailableDelivererAndVehicle() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("""
                SELECT d.ID as deliverer_id, d.name as deliverer_name, 
                       v.ID as vehicle_id, v.type as vehicle_type
                FROM deliverers d
                CROSS JOIN vehicles v
                WHERE d.occupe = 0 
                AND v.taken = 0
                ORDER BY RAND()
                LIMIT 1
            """)) {
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int vehicleId = rs.getInt("vehicle_id");
                
                try (PreparedStatement updateVehicle = connection.prepareStatement(
                        "UPDATE vehicles SET taken = 1 WHERE ID = ?")) {
                    updateVehicle.setInt(1, vehicleId);
                    updateVehicle.executeUpdate();
                }
                
                return new DelivererVehiclePair(
                    rs.getInt("deliverer_id"),
                    rs.getString("deliverer_name"),
                    vehicleId,
                    rs.getString("vehicle_type")
                );
            }
        }
        return null;
    }

    private void processOrder() {
        CustomerItem customer = (CustomerItem) customerCombo.getSelectedItem();
        PizzaItem pizza = (PizzaItem) pizzaCombo.getSelectedItem();
        SizeItem size = (SizeItem) sizeCombo.getSelectedItem();
        LocalDateTime orderTime = LocalDateTime.now();
        int deliveryTime = generateDeliveryTime();
        LocalDateTime expectedDeliveryTime = orderTime.plusMinutes(deliveryTime);
        boolean willBeFree = deliveryTime >= 31;

        double finalPrice = pizza.price * (1 + size.priceAdjust / 100.0);

        if (customer.balance < finalPrice) {
            JOptionPane.showMessageDialog(this, 
                "Solde insuffisant ! Solde actuel : " + customer.balance + "€, Prix : " + finalPrice + "€");
            return;
        }

        try {
            connection.setAutoCommit(false);

            DelivererVehiclePair pair = findAvailableDelivererAndVehicle();
            if (pair == null) {
                throw new SQLException("Aucun livreur ou véhicule disponible pour le moment");
            }

            try (PreparedStatement updateDeliverer = connection.prepareStatement(
                "UPDATE deliverers SET occupe = 1 WHERE ID = ?")) {
                updateDeliverer.setInt(1, pair.delivererId);
                updateDeliverer.executeUpdate();
            }

            int orderId;
            try (PreparedStatement orderStmt = connection.prepareStatement(
                """
                INSERT INTO orders (customer_id, deliverer_id, vehicle_id, 
                                  order_time, is_free, delivery_time, delay_minutes, expected_delivery_time)
                VALUES (?, ?, ?, ?, ?, NULL, NULL, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                
                orderStmt.setInt(1, customer.id);
                orderStmt.setInt(2, pair.delivererId);
                orderStmt.setInt(3, pair.vehicleId);
                orderStmt.setTimestamp(4, Timestamp.valueOf(orderTime));
                orderStmt.setBoolean(5, willBeFree);
                orderStmt.setTimestamp(6, Timestamp.valueOf(expectedDeliveryTime));
                
                orderStmt.executeUpdate();
                
                try (ResultSet rs = orderStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    } else {
                        throw new SQLException("Impossible de récupérer l'ID de la commande");
                    }
                }
            }

            try (PreparedStatement pizzaStmt = connection.prepareStatement(
                "INSERT INTO orders_products (order_id, product_id) VALUES (?, ?)")) {
                pizzaStmt.setInt(1, orderId);
                pizzaStmt.setInt(2, pizza.id);
                pizzaStmt.executeUpdate();
            }

            connection.commit();

            scheduleDelivery(orderId, pair.delivererId, customer.id, finalPrice, orderTime, deliveryTime);

            String message = String.format("""
                Commande créée avec succès!
                Livreur : %s
                Véhicule : %s
                Temps de livraison estimé : %d minutes
                %s""",
                pair.delivererName,
                pair.vehicleType,
                deliveryTime,
                willBeFree ? "La commande sera gratuite (>30min)!" : "Prix : " + finalPrice + "€"
            );
            JOptionPane.showMessageDialog(this, message);

            customerCombo.removeAllItems();
            loadCustomers();

            if (deliveryPanel != null) {
                deliveryPanel.refreshData();
            }

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Erreur lors de la création de la commande : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void finishDelivery(int orderId, int delivererId, int customerId, 
                               double price, LocalDateTime orderTime, int deliveryTime) 
            throws SQLException {
        try {
            connection.setAutoCommit(false);

            LocalDateTime deliveryDateTime = LocalDateTime.now();
            boolean isLate = deliveryTime >= 31;

            long actualDelayMinutes = Duration.between(orderTime, deliveryDateTime).toMinutes();

            int vehicleId;
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT vehicle_id FROM orders WHERE ID = ?")) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Commande non trouvée: " + orderId);
                    }
                    vehicleId = rs.getInt("vehicle_id");
                }
            }


            try (PreparedStatement updateOrder = connection.prepareStatement("""
                    UPDATE orders 
                    SET delivery_time = ?, 
                        order_price = ?,
                        delay_minutes = ?
                    WHERE ID = ?
                    """)) {
                updateOrder.setTimestamp(1, Timestamp.valueOf(deliveryDateTime));
                updateOrder.setDouble(2, isLate ? 0 : price);
                updateOrder.setLong(3, actualDelayMinutes);
                updateOrder.setInt(4, orderId);
                updateOrder.executeUpdate();
            }

            if (!isLate) {
                try (PreparedStatement updateCustomer = connection.prepareStatement(
                    "UPDATE customers SET balance = balance - ? WHERE ID = ?")) {
                    updateCustomer.setDouble(1, price);
                    updateCustomer.setInt(2, customerId);
                    updateCustomer.executeUpdate();
                }
            }

            try (PreparedStatement updateDeliverer = connection.prepareStatement("""
                    UPDATE deliverers 
                    SET occupe = 0,
                        nbrRetards = nbrRetards + ?,
                        minutesRetards = minutesRetards + ?
                    WHERE ID = ?
                    """)) {
                updateDeliverer.setInt(1, isLate ? 1 : 0);
                updateDeliverer.setInt(2, isLate ? deliveryTime - 1 : 0);
                updateDeliverer.setInt(3, delivererId);
                updateDeliverer.executeUpdate();
            }

            try (PreparedStatement updateVehicle = connection.prepareStatement("""
                    UPDATE vehicles 
                    SET uses = uses + 1,
                        nbrRetard = nbrRetard + ?,
                        taken = 0
                    WHERE ID = ?
                    """)) {
                updateVehicle.setInt(1, isLate ? 1 : 0);
                updateVehicle.setInt(2, vehicleId);
                updateVehicle.executeUpdate();
            }

            connection.commit();

            SwingUtilities.invokeLater(() -> {
                if (deliveryPanel != null) {
                    deliveryPanel.refreshData();
                }
            });


        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void loadCustomers() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, balance FROM customers")) {
            while (rs.next()) {
                customerCombo.addItem(new CustomerItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("balance")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des clients");
        }
    }

    private void loadPizzas() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price FROM pizzas")) {
            while (rs.next()) {
                pizzaCombo.addItem(new PizzaItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des pizzas");
        }
    }

    private void loadSizes() {
        sizeCombo.removeAllItems();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT size, price_adjust FROM pizza_sizes")) {
            while (rs.next()) {
                double basePrice = ((PizzaItem) pizzaCombo.getSelectedItem()).price;
                sizeCombo.addItem(new SizeItem(
                    rs.getString("size"),
                    rs.getDouble("price_adjust"),
                    basePrice
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des tailles");
        }
    }

    private int generateDeliveryTime() {
        Random random = new Random();
        if (random.nextDouble() < 0.7) {
            return random.nextInt(19) + 12; // 12 à 30 minutes
        } else {
            return random.nextInt(12) + 31; // 31 à 42 minutes
        }
    }

    private static class CustomerItem {
        final int id;
        final String name;
        final double balance;

        CustomerItem(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        @Override
        public String toString() {
            return name + " (Solde: " + balance + "€)";
        }
    }

    private static class PizzaItem {
        final int id;
        final String name;
        final double price;

        PizzaItem(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return name + " (" + price + "€)";
        }
    }

    private static class SizeItem {
        final String size;
        final double priceAdjust;
        final String displayText;

        SizeItem(String size, double priceAdjust, double basePrice) {
            this.size = size;
            this.priceAdjust = priceAdjust;
            double finalPrice = basePrice * (1 + priceAdjust / 100.0);
            this.displayText = String.format("%s (%.2f€)", size, finalPrice);
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    private static class DelivererVehiclePair {
        final int delivererId;
        final String delivererName;
        final int vehicleId;
        final String vehicleType;

        DelivererVehiclePair(int delivererId, String delivererName, 
                            int vehicleId, String vehicleType) {
            this.delivererId = delivererId;
            this.delivererName = delivererName;
            this.vehicleId = vehicleId;
            this.vehicleType = vehicleType;
        }
    }
}
