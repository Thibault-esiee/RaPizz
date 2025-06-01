package fr.esiee.rapizz;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class StatsPanel extends JPanel {
    public StatsPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Statistiques");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(5, 1, 10, 10));

        grid.add(createStatCard("Meilleur client", getBestClient()));
        grid.add(createStatCard("Livreurs avec le plus/moins de retards", getWorstDeliverers()));
        grid.add(createStatCard("Véhicules avec le plus/moins de retards", getVehicleDelays()));
        grid.add(createStatCard("Pizza la plus/moins demandée", getPizzaDemand()));
        grid.add(createStatCard("Ingrédient le plus populaire", getMostPopularIngredient()));

        JScrollPane scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, String placeholder) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            new EmptyBorder(10, 15, 10, 15)
        ));
        JLabel labelTitle = new JLabel(title);
        labelTitle.setFont(labelTitle.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(labelTitle, BorderLayout.NORTH);

        JTextArea content = new JTextArea(placeholder);
        content.setEditable(false);
        content.setOpaque(false);
        content.setFont(content.getFont().deriveFont(14f));
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private String getBestClient() {
        String sql = """
            SELECT c.name, COUNT(o.ID) AS nb, COALESCE(SUM(o.order_price),0) AS total
            FROM customers c
            LEFT JOIN orders o ON o.customer_id = c.ID
            GROUP BY c.ID
            ORDER BY nb DESC, total DESC
            LIMIT 1
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("name") + " (" + rs.getInt("nb") + " commandes / " + rs.getDouble("total") + " €)";
            }
        } catch (SQLException e) {
            return "Erreur: " + e.getMessage();
        }
        return "Aucun client";
    }

    private String getWorstDeliverers() {
        StringBuilder sb = new StringBuilder();
        String sqlMost = """
            SELECT name, nbrRetards 
            FROM deliverers 
            WHERE nbrRetards > 0
            ORDER BY nbrRetards DESC
            LIMIT 1
        """;
        String sqlLeast = """
            SELECT name, nbrRetards 
            FROM deliverers 
            WHERE nbrRetards > 0
            ORDER BY nbrRetards ASC
            LIMIT 1
        """;
        try (Connection conn = DBConnection.getConnection()) {
            // Livreur avec le plus de retards
            try (PreparedStatement ps = conn.prepareStatement(sqlMost);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Plus de retards: ")
                      .append(rs.getString("name"))
                      .append(" (")
                      .append(rs.getInt("nbrRetards"))
                      .append(" retards)\n");
                }
            }
            // Livreur avec le moins de retards (parmi ceux qui en ont)
            try (PreparedStatement ps = conn.prepareStatement(sqlLeast);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Moins de retards: ")
                      .append(rs.getString("name"))
                      .append(" (")
                      .append(rs.getInt("nbrRetards"))
                      .append(" retards)");
                }
            }
        } catch (SQLException e) {
            return "Erreur: " + e.getMessage();
        }
        return sb.length() > 0 ? sb.toString().trim() : "Aucun retard";
    }

    private String getVehicleDelays() {
        StringBuilder sb = new StringBuilder();
        String sqlMost = """
            SELECT type, nbrRetard 
            FROM vehicles 
            WHERE nbrRetard > 0
            ORDER BY nbrRetard DESC
            LIMIT 1
        """;
        String sqlLeast = """
            SELECT type, nbrRetard 
            FROM vehicles 
            WHERE nbrRetard > 0
            ORDER BY nbrRetard ASC
            LIMIT 1
        """;
        try (Connection conn = DBConnection.getConnection()) {
            // Véhicule avec le plus de retards
            try (PreparedStatement ps = conn.prepareStatement(sqlMost);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Plus de retards: ")
                      .append(rs.getString("type"))
                      .append(" (")
                      .append(rs.getInt("nbrRetard"))
                      .append(" retards)\n");
                }
            }
            // Véhicule avec le moins de retards (parmi ceux qui en ont)
            try (PreparedStatement ps = conn.prepareStatement(sqlLeast);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Moins de retards: ")
                      .append(rs.getString("type"))
                      .append(" (")
                      .append(rs.getInt("nbrRetard"))
                      .append(" retards)");
                }
            }
        } catch (SQLException e) {
            return "Erreur: " + e.getMessage();
        }
        return sb.length() > 0 ? sb.toString().trim() : "Aucun retard";
    }

    private String getPizzaDemand() {
        StringBuilder sb = new StringBuilder();
        String sqlMost = """
            SELECT p.name, COUNT(op.ID) AS nb
            FROM pizzas p
            LEFT JOIN orders_products op ON op.product_id = p.id
            GROUP BY p.id
            ORDER BY nb DESC
            LIMIT 1
        """;
        String sqlLeast = """
            SELECT p.name, COUNT(op.ID) AS nb
            FROM pizzas p
            LEFT JOIN orders_products op ON op.product_id = p.id
            GROUP BY p.id
            ORDER BY nb ASC
            LIMIT 1
        """;
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlMost);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("La plus demandée: ")
                      .append(rs.getString("name"))
                      .append(" (")
                      .append(rs.getInt("nb"))
                      .append(" fois)\n");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlLeast);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("La moins demandée: ")
                      .append(rs.getString("name"))
                      .append(" (")
                      .append(rs.getInt("nb"))
                      .append(" fois)");
                }
            }
        } catch (SQLException e) {
            return "Erreur: " + e.getMessage();
        }
        return sb.length() > 0 ? sb.toString().trim() : "Aucune commande";
    }

    private String getMostPopularIngredient() {
        String sql = """
            SELECT i.name, COUNT(ip.ID) AS nb
            FROM ingredients i
            LEFT JOIN ingredients_products ip ON ip.ingredients_id = i.id
            GROUP BY i.id
            ORDER BY nb DESC
            LIMIT 1
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("name") + " (" + rs.getInt("nb") + " utilisations)";
            }
        } catch (SQLException e) {
            return "Erreur: " + e.getMessage();
        }
        return "Aucun ingrédient";
    }
}