package fr.esiee.rapizz;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class DeliveryPanel extends JPanel {
    private JTextArea area;

    public DeliveryPanel() {
        setLayout(new BorderLayout());
        area = new JTextArea();
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);
        loadDeliveryData();
    }

    private void loadDeliveryData() {
        String query = """
            SELECT d.name AS deliverer, v.type AS vehicle, c.name AS customer, 
                   o.order_time, o.delivery_time, o.delay_minutes,
                   p.name AS pizza, p.price AS base_price
            FROM orders o
            JOIN deliverers d ON o.deliverer_id = d.ID
            JOIN vehicles v ON o.vehicle_id = v.ID
            JOIN customers c ON o.customer_id = c.ID
            JOIN orders_products op ON op.order_id = o.ID
            JOIN pizzas p ON op.product_id = p.id
            """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            StringBuilder sb = new StringBuilder("📦 Fiches de livraison 📦\n\n");
            while (rs.next()) {
                sb.append("Livreur : ").append(rs.getString("deliverer")).append("\n")
                  .append("Véhicule : ").append(rs.getString("vehicle")).append("\n")
                  .append("Client : ").append(rs.getString("customer")).append("\n")
                  .append("Date commande : ").append(rs.getTimestamp("order_time")).append("\n")
                  .append("Retard : ").append(rs.getInt("delay_minutes")).append(" min\n")
                  .append("Pizza : ").append(rs.getString("pizza")).append("\n")
                  .append("Prix base : ").append(rs.getDouble("base_price")).append(" €\n\n");
            }

            area.setText(sb.toString());

        } catch (SQLException e) {
            area.setText("Erreur lors du chargement des fiches de livraison : " + e.getMessage());
        }
    }
}