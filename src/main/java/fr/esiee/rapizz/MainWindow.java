package fr.esiee.rapizz;

import java.awt.BorderLayout;
import java.sql.Connection;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.JOptionPane;

import com.formdev.flatlaf.FlatLightLaf;

public class MainWindow extends JFrame {
    private Connection connection;
    private JTabbedPane tabs;
    private ClientManagementPanel clientPanel;
    private DeliveryPanel deliveryPanel;
    private DelivererManagementPanel delivererManagementPanel;

    public MainWindow() {
        setTitle("RaPizz - Application");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        try {
            connection = DBConnection.getConnection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erreur de connexion à la base de données : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        tabs = new JTabbedPane();
        tabs.addTab("Menu", new MenuPanel());
        
        deliveryPanel = new DeliveryPanel();
        tabs.addTab("Fiches de livraison", deliveryPanel);
        
        tabs.addTab("Créer commande", new OrderForm(connection, deliveryPanel));
        
        clientPanel = new ClientManagementPanel();
        tabs.addTab("Clients", clientPanel);
        
        delivererManagementPanel = new DelivererManagementPanel();
        tabs.addTab("Livreurs", delivererManagementPanel);
        
        tabs.addChangeListener(e -> {
            int selectedIndex = tabs.getSelectedIndex();
            
            if (selectedIndex == 3) {
                clientPanel.refreshData();
            }
            if (tabs.getSelectedComponent() == delivererManagementPanel) {
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