package fr.esiee.rapizz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class OrderManager {

    public boolean createOrder(int customerId, int pizzaId, int sizeId, int delivererId, int vehicleId, Timestamp orderTime, Timestamp deliveryTime) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                double basePrice = getPizzaBasePrice(pizzaId, conn);
                double sizeAdjust = getSizePriceAdjust(sizeId, conn);
                double finalPrice = basePrice * (1 + sizeAdjust);

                Customer customer = getCustomerById(customerId, conn)
                    .orElseThrow(() -> new SQLException("Client introuvable"));
                int nbrOrders = customer.getNbrOrders();

                boolean isFree = false;
                if ((nbrOrders + 1) % 11 == 0) {
                    isFree = true;
                    finalPrice = 0;
                }

                long delayMinutes = (deliveryTime.getTime() - orderTime.getTime()) / (60 * 1000);
                if (delayMinutes > 30) {
                    isFree = true;
                    finalPrice = 0;
                }

                if (!isFree && customer.getBalance() < finalPrice) {
                    conn.rollback();
                    return false;
                }

                int orderId = insertOrder(customerId, delivererId, vehicleId, finalPrice, orderTime, deliveryTime, isFree, conn);

                insertOrderProduct(orderId, pizzaId, conn);

                if (!isFree) {
                    updateCustomerBalance(customerId, customer.getBalance() - finalPrice, conn);
                }

                updateCustomerNbrOrders(customerId, nbrOrders + 1, conn);

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private double getPizzaBasePrice(int pizzaId, Connection conn) throws SQLException {
        String sql = "SELECT price FROM pizzas WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pizzaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                } else {
                    throw new SQLException("Pizza introuvable");
                }
            }
        }
    }

    private double getSizePriceAdjust(int sizeId, Connection conn) throws SQLException {
        String sql = "SELECT price_adjust FROM pizza_sizes WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sizeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price_adjust");
                } else {
                    throw new SQLException("Taille pizza introuvable");
                }
            }
        }
    }

    private Optional<Customer> getCustomerById(int customerId, Connection conn) throws SQLException {
        String sql = "SELECT name, address, balance, nbrOrders FROM customers WHERE ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Customer c = new Customer(
                        customerId,
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getDouble("balance"),
                        rs.getInt("nbrOrders")
                    );
                    return Optional.of(c);
                }
                return Optional.empty();
            }
        }
    }

    private int insertOrder(int customerId, int delivererId, int vehicleId, double price, Timestamp orderTime, Timestamp deliveryTime, boolean isFree, Connection conn) throws SQLException {
        String sql = "INSERT INTO orders (customer_id, deliverer_id, vehicle_id, order_price, order_time, delivery_time, is_free) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, delivererId);
            ps.setInt(3, vehicleId);
            ps.setDouble(4, price);
            ps.setTimestamp(5, orderTime);
            ps.setTimestamp(6, deliveryTime);
            ps.setBoolean(7, isFree);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    throw new SQLException("Erreur récupération ID commande");
                }
            }
        }
    }

    private void insertOrderProduct(int orderId, int pizzaId, Connection conn) throws SQLException {
        String sql = "INSERT INTO orders_products (order_id, product_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, pizzaId);
            ps.executeUpdate();
        }
    }

    private void updateCustomerBalance(int customerId, double newBalance, Connection conn) throws SQLException {
        String sql = "UPDATE customers SET balance = ? WHERE ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
    }

    private void updateCustomerNbrOrders(int customerId, int nbrOrders, Connection conn) throws SQLException {
        String sql = "UPDATE customers SET nbrOrders = ? WHERE ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nbrOrders);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
    }

    public static class Customer {
        private final int id;
        private final String name;
        private final String address;
        private final double balance;
        private final int nbrOrders;

        public Customer(int id, String name, String address, double balance, int nbrOrders) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.balance = balance;
            this.nbrOrders = nbrOrders;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public double getBalance() { return balance; }
        public int getNbrOrders() { return nbrOrders; }
    }
}