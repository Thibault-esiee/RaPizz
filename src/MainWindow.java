import java.awt.*;
import javax.swing.*;

public class MainWindow extends JFrame {
    public MainWindow() {
        setTitle("RaPizz - Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Menu", new MenuPanel());
        tabs.addTab("Fiches de livraison", new DeliveryPanel());

        add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}