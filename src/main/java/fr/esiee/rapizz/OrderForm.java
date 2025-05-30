package fr.esiee.rapizz;

import java.awt.GridLayout;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class OrderForm extends JPanel {

    private JComboBox<ComboItem> customerCombo;
    private JComboBox<ComboItem> pizzaCombo;
    private JComboBox<ComboItem> sizeCombo;
    private JComboBox<ComboItem> delivererCombo;
    private JComboBox<ComboItem> vehicleCombo;
    private JButton submitButton;

    private OrderManager orderManager = new OrderManager();

    public OrderForm() {
        JPanel panel = new JPanel(new GridLayout(7, 2, 5, 5));

        panel.add(new JLabel("Client :"));
        customerCombo = new JComboBox<>();
        loadCustomers();
        panel.add(customerCombo);

        panel.add(new JLabel("Pizza :"));
        pizzaCombo = new JComboBox<>();
        loadPizzas();
        panel.add(pizzaCombo);

        panel.add(new JLabel("Taille :"));
        sizeCombo = new JComboBox<>();
        loadSizes();
        panel.add(sizeCombo);

        panel.add(new JLabel("Livreur :"));
        delivererCombo = new JComboBox<>();
        loadDeliverers();
        panel.add(delivererCombo);

        panel.add(new JLabel("Véhicule :"));
        vehicleCombo = new JComboBox<>();
        loadVehicles();
        panel.add(vehicleCombo);

        submitButton = new JButton("Créer commande");
        panel.add(submitButton);

        panel.add(new JLabel());

        add(panel);

        submitButton.addActionListener(e -> submitOrder());
    }

    private void submitOrder() {
        try {
            int customerId = ((ComboItem) customerCombo.getSelectedItem()).getId();
            int pizzaId = ((ComboItem) pizzaCombo.getSelectedItem()).getId();
            int sizeId = ((ComboItem) sizeCombo.getSelectedItem()).getId();
            int delivererId = ((ComboItem) delivererCombo.getSelectedItem()).getId();
            int vehicleId = ((ComboItem) vehicleCombo.getSelectedItem()).getId();

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            Timestamp deliveryTime = new Timestamp(now.getTime() + 20 * 60 * 1000);

            boolean success = orderManager.createOrder(
                customerId,
                pizzaId,
                sizeId,
                delivererId,
                vehicleId,
                now,
                deliveryTime
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "Commande créée avec succès !");
            } else {
                JOptionPane.showMessageDialog(this, "Solde insuffisant, commande refusée.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la création : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void loadCustomers() {
        try (var conn = DBConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT ID, name FROM customers")) {
            while (rs.next()) {
                customerCombo.addItem(new ComboItem(rs.getInt("ID"), rs.getString("name")));
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void loadPizzas() {
        try (var conn = DBConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id, name FROM pizzas")) {
            while (rs.next()) {
                pizzaCombo.addItem(new ComboItem(rs.getInt("id"), rs.getString("name")));
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void loadSizes() {
        try (var conn = DBConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id, size FROM pizza_sizes")) {
            while (rs.next()) {
                sizeCombo.addItem(new ComboItem(rs.getInt("id"), rs.getString("size")));
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void loadDeliverers() {
        try (var conn = DBConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT ID, name FROM deliverers")) {
            while (rs.next()) {
                delivererCombo.addItem(new ComboItem(rs.getInt("ID"), rs.getString("name")));
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void loadVehicles() {
        try (var conn = DBConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT ID, type FROM vehicles")) {
            while (rs.next()) {
                vehicleCombo.addItem(new ComboItem(rs.getInt("ID"), rs.getString("type")));
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}