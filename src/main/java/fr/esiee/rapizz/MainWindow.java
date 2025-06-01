package fr.esiee.rapizz;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.formdev.flatlaf.FlatLightLaf;

public class MainWindow extends JFrame {
    public MainWindow() {
        setTitle("RaPizz - Application");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Menu", new MenuPanel());
        tabs.addTab("Fiches de livraison", new DeliveryPanel());
        tabs.addTab("Créer commande", new OrderForm());
        tabs.addTab("Clients", new ClientManagementPanel());
        tabs.addTab("Livreurs", new DelivererManagementPanel());
        
        add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}