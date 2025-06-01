package fr.esiee.rapizz;

import java.awt.BorderLayout;
import java.io.IOException;
import java.awt.Image;
import java.sql.Connection;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.JOptionPane;

import com.formdev.flatlaf.FlatLightLaf;

public class MainWindow extends JFrame {
    private Connection connection;
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
        
        try {
            connection = DBConnection.getConnection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erreur de connexion à la base de données : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Menu", new MenuPanel());
        tabs.addTab("Fiches de livraison", new DeliveryPanel());
        tabs.addTab("Créer commande", new OrderForm(connection, (DeliveryPanel) tabs.getComponentAt(1)));
        tabs.addTab("Clients", new ClientManagementPanel());
        tabs.addTab("Livreurs", new DelivererManagementPanel());

        tabs.addTab("Statistiques", new StatsPanel());
        tabs.addTab("Recherche", new SearchPanel());
        tabs.add("Admin", new AdminPanel());
        
        tabs.addChangeListener(e -> {
            int selectedIndex = tabs.getSelectedIndex();
            
            if (tabs.getComponentAt(3) instanceof ClientManagementPanel clientPanel) {
                clientPanel.refreshData();
            }
            if (tabs.getSelectedComponent() instanceof DelivererManagementPanel delivererManagementPanel) {
                delivererManagementPanel.refreshData();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void dispose() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.dispose();
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