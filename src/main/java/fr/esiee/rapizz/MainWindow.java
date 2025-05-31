package fr.esiee.rapizz;

import java.awt.BorderLayout;
import java.io.IOException;
import java.awt.Image;

import javax.imageio.ImageIO;
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

        try {
            Image icon = ImageIO.read(getClass().getResource("/icon.png"));
            setIconImage(icon);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load icon / not present");
        } 

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Menu", new MenuPanel());
        tabs.addTab("Fiches de livraison", new DeliveryPanel());
        tabs.addTab("Créer commande", new OrderForm());
        tabs.addTab("Clients", new ClientManagementPanel());

        tabs.addTab("Statistiques", new StatsPanel());
        tabs.addTab("Recherche", new SearchPanel());
        tabs.add("Admin", new AdminPanel());
        
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