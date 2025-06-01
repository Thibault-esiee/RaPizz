package fr.esiee.rapizz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.sql.*;

public class SearchPanel extends JPanel {

    private JTextArea resultArea;

    public SearchPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Recherche & Requêtes personnalisées");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        JButton btnUnusedVehicles = new JButton("Véhicules jamais utilisés");
        JButton btnClientsAboveAvg = new JButton("Clients > moyenne commandes");
        JButton btnExport = new JButton("Exporter résultats");

        buttonPanel.add(btnUnusedVehicles);
        buttonPanel.add(btnClientsAboveAvg);
        buttonPanel.add(btnExport);

        add(buttonPanel, BorderLayout.SOUTH);

        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setFont(resultArea.getFont().deriveFont(14f));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        btnUnusedVehicles.addActionListener(e -> showUnusedVehicles());
        btnClientsAboveAvg.addActionListener(e -> showClientsAboveAverage());
        btnExport.addActionListener(this::exportResults);
    }

    private void showUnusedVehicles() {
        StringBuilder sb = new StringBuilder();
        String sql = """
            SELECT v.type, v.plate
            FROM vehicles v
            LEFT JOIN orders o ON o.vehicle_id = v.ID
            WHERE o.ID IS NULL
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            sb.append("Véhicules jamais utilisés :\n");
            while (rs.next()) {
                sb.append("- ").append(rs.getString("type"))
                  .append(" (").append(rs.getString("plate")).append(")\n");
            }
            if (sb.toString().endsWith(":\n")) sb.append("Aucun véhicule trouvé.");
        } catch (SQLException e) {
            sb.append("Erreur: ").append(e.getMessage());
        }
        resultArea.setText(sb.toString());
    }

    private void showClientsAboveAverage() {
        StringBuilder sb = new StringBuilder();
        String sql = """
            SELECT c.name, COUNT(o.ID) AS nb
            FROM customers c
            JOIN orders o ON o.customer_id = c.ID
            GROUP BY c.ID
            HAVING nb > (
                SELECT AVG(cnt) FROM (
                    SELECT COUNT(*) AS cnt FROM orders GROUP BY customer_id
                ) AS sub
            )
            ORDER BY nb DESC
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            sb.append("Clients ayant commandé plus que la moyenne :\n");
            while (rs.next()) {
                sb.append("- ").append(rs.getString("name"))
                  .append(" (").append(rs.getInt("nb")).append(" commandes)\n");
            }
            if (sb.toString().endsWith(":\n")) sb.append("Aucun client trouvé.");
        } catch (SQLException e) {
            sb.append("Erreur: ").append(e.getMessage());
        }
        resultArea.setText(sb.toString());
    }

    private void exportResults(ActionEvent e) {
        String text = resultArea.getText();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun résultat à exporter.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exporter les résultats");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
                out.print(text);
                JOptionPane.showMessageDialog(this, "Export réussi.", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'export : " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}