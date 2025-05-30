import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class MenuPanel extends JPanel {
    public MenuPanel() {
        setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT p.name AS pizza, p.price, GROUP_CONCAT(i.name SEPARATOR ', ') AS ingredients " +
                 "FROM pizzas p " +
                 "JOIN ingredients_products ip ON ip.product_id = p.id " +
                 "JOIN ingredients i ON i.id = ip.ingredients_id " +
                 "GROUP BY p.id")) {

            StringBuilder sb = new StringBuilder("🍕 Menu des pizzas 🍕\n\n");
            while (rs.next()) {
                sb.append("• ").append(rs.getString("pizza")).append(" - ")
                  .append(rs.getDouble("price")).append("€\n")
                  .append("  Ingrédients : ").append(rs.getString("ingredients")).append("\n\n");
            }

            area.setText(sb.toString());

        } catch (SQLException e) {
            area.setText("Erreur lors du chargement du menu : " + e.getMessage());
        }
    }
}