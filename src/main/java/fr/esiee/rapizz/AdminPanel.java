package fr.esiee.rapizz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminPanel extends JPanel {

    public AdminPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Administration / Paramètres");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        // Catalogue pizzas
        JPanel pizzaPanel = new JPanel(new BorderLayout(10, 10));
        pizzaPanel.add(new JLabel("Gestion du catalogue pizzas, tailles, prix (placeholder)"), BorderLayout.CENTER);
        tabs.addTab("Catalogue pizzas", pizzaPanel);

        // Règles de bonification
        JPanel bonusPanel = new JPanel(new BorderLayout(10, 10));
        bonusPanel.add(new JLabel("Gestion des règles de bonification (configurable) (placeholder)"), BorderLayout.CENTER);
        tabs.addTab("Règles de bonification", bonusPanel);

        // Sauvegarde / restauration
        JPanel backupPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        JButton btnBackup = new JButton("Sauvegarder les données");
        JButton btnRestore = new JButton("Restaurer les données");
        backupPanel.add(btnBackup);
        backupPanel.add(btnRestore);
        JPanel backupWrapper = new JPanel(new BorderLayout());
        backupWrapper.add(new JLabel("Sauvegarde / restauration des données", SwingConstants.CENTER), BorderLayout.NORTH);
        backupWrapper.add(backupPanel, BorderLayout.CENTER);
        tabs.addTab("Sauvegarde / Restauration", backupWrapper);

        add(tabs, BorderLayout.CENTER);

        // Placeholders for button actions
        btnBackup.addActionListener(e -> JOptionPane.showMessageDialog(this, "Fonction de sauvegarde à implémenter.", "Sauvegarde", JOptionPane.INFORMATION_MESSAGE));
        btnRestore.addActionListener(e -> JOptionPane.showMessageDialog(this, "Fonction de restauration à implémenter.", "Restauration", JOptionPane.INFORMATION_MESSAGE));
    }
}