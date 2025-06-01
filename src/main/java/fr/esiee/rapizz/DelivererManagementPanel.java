package fr.esiee.rapizz;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DelivererManagementPanel extends JPanel {
    private JTable table;
    private DelivererTableModel tableModel;
    private JButton addButton, updateButton, deleteButton;
    private JTextField nameField;

    private JTable vehicleTable;
    private VehicleTableModel vehicleTableModel;

    public DelivererManagementPanel() {
        setLayout(new BorderLayout(10, 10));

        tableModel = new DelivererTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel form = new JPanel(new GridLayout(2, 2));
        form.setBorder(BorderFactory.createTitledBorder("Livreur"));
        form.add(new JLabel("Nom :"));
        nameField = new JTextField();
        form.add(nameField);

        addButton = new JButton("Ajouter");
        updateButton = new JButton("Modifier");
        deleteButton = new JButton("Supprimer");

        JPanel buttons = new JPanel();
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);

        JPanel left = new JPanel(new BorderLayout());
        left.add(scrollPane, BorderLayout.CENTER);
        left.add(form, BorderLayout.NORTH);
        left.add(buttons, BorderLayout.SOUTH);

        vehicleTableModel = new VehicleTableModel();
        vehicleTable = new JTable(vehicleTableModel);
        JScrollPane vehicleScroll = new JScrollPane(vehicleTable);
        vehicleScroll.setBorder(BorderFactory.createTitledBorder("Véhicules disponibles"));

        add(left, BorderLayout.CENTER);
        add(vehicleScroll, BorderLayout.EAST);

        loadDeliverers();
        loadVehicles();

        addButton.addActionListener(e -> addDeliverer());
        updateButton.addActionListener(e -> updateDeliverer());
        deleteButton.addActionListener(e -> deleteDeliverer());

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                Deliverer d = tableModel.getDelivererAt(row);
                nameField.setText(d.name);
            }
        });
    }

    public void refreshData() {
        loadDeliverers();
        loadVehicles();
    }

    private void loadDeliverers() {
        List<Deliverer> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 """
                 SELECT ID, name, nbrRetards, minutesRetards, occupe 
                 FROM deliverers 
                 ORDER BY name
                 """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Deliverer(
                    rs.getInt("ID"),
                    rs.getString("name"),
                    rs.getInt("nbrRetards"),
                    rs.getInt("minutesRetards"),
                    rs.getBoolean("occupe")
                ));
            }
            tableModel.setDeliverers(list);
        } catch (SQLException ex) {
            showError("Erreur chargement livreurs : " + ex.getMessage());
        }
    }

    private void loadVehicles() {
        List<Vehicle> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 """
                 SELECT ID, type, uses, taken, acquisition_date, nbrRetard 
                 FROM vehicles 
                 ORDER BY type
                 """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp acquisitionTs = rs.getTimestamp("acquisition_date");
                LocalDateTime acquisitionDate = acquisitionTs != null ? 
                    acquisitionTs.toLocalDateTime() : 
                    LocalDateTime.of(2004, 9, 10, 0, 0);

                list.add(new Vehicle(
                    rs.getInt("ID"),
                    rs.getString("type"),
                    rs.getInt("uses"),
                    rs.getBoolean("taken"),
                    acquisitionDate,
                    rs.getInt("nbrRetard")
                ));
            }
            vehicleTableModel.setVehicles(list);
        } catch (SQLException e) {
            showError("Erreur chargement véhicules : " + e.getMessage());
        }
    }

    private void addDeliverer() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Nom requis");
            return;
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO deliverers (name, nbrRetards, minutesRetards, occupe) VALUES (?, 0, 0, false)");) {
            ps.setString(1, name);
            ps.executeUpdate();
            loadDeliverers();
        } catch (SQLException e) {
            showError("Erreur ajout : " + e.getMessage());
        }
    }

    private void updateDeliverer() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Deliverer d = tableModel.getDelivererAt(row);
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE deliverers SET name = ? WHERE ID = ?")) {
            ps.setString(1, name);
            ps.setInt(2, d.id);
            ps.executeUpdate();
            loadDeliverers();
        } catch (SQLException e) {
            showError("Erreur modif : " + e.getMessage());
        }
    }

    private void deleteDeliverer() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Deliverer d = tableModel.getDelivererAt(row);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM deliverers WHERE ID = ?")) {
            ps.setInt(1, d.id);
            ps.executeUpdate();
            loadDeliverers();
        } catch (SQLException e) {
            showError("Erreur suppression : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private static class Deliverer {
        int id;
        String name;
        int nbrRetards;
        int minutesRetards;
        boolean occupe;

        Deliverer(int id, String name, int nbrRetards, int minutesRetards, boolean occupe) {
            this.id = id;
            this.name = name;
            this.nbrRetards = nbrRetards;
            this.minutesRetards = minutesRetards;
            this.occupe = occupe;
        }
    }

    private static class Vehicle {
        int id;
        String type;
        int uses;
        boolean taken;
        LocalDateTime acquisitionDate;
        int nbrRetard;

        Vehicle(int id, String type, int uses, boolean taken, LocalDateTime acquisitionDate, int nbrRetard) {
            this.id = id;
            this.type = type;
            this.uses = uses;
            this.taken = taken;
            this.acquisitionDate = acquisitionDate;
            this.nbrRetard = nbrRetard;
        }
    }

    private class DelivererTableModel extends AbstractTableModel {
        private List<Deliverer> deliverers = new ArrayList<>();
        private final String[] columns = {
            "ID", "Nom", "Nombre de retards", "Minutes de retard", "Occupé"
        };

        public void setDeliverers(List<Deliverer> deliverers) {
            this.deliverers = deliverers;
            fireTableDataChanged();
        }

        public Deliverer getDelivererAt(int row) {
            return deliverers.get(row);
        }

        @Override
        public int getRowCount() {
            return deliverers.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Deliverer d = deliverers.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> d.id;
                case 1 -> d.name;
                case 2 -> d.nbrRetards;
                case 3 -> d.minutesRetards;
                case 4 -> d.occupe ? "Oui" : "Non";
                default -> null;
            };
        }
    }

    private static class VehicleTableModel extends AbstractTableModel {
        private final String[] cols = {
            "ID", "Type", "Utilisations", "Occupé", "Date acquisition", "Retards"
        };
        private List<Vehicle> list = new ArrayList<>();
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        public void setVehicles(List<Vehicle> list) {
            this.list = list;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return list.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override public Object getValueAt(int row, int col) {
            Vehicle v = list.get(row);
            return switch (col) {
                case 0 -> v.id;
                case 1 -> v.type;
                case 2 -> v.uses;
                case 3 -> v.taken ? "Oui" : "Non";
                case 4 -> v.acquisitionDate.format(formatter);
                case 5 -> v.nbrRetard;
                default -> "";
            };
        }
    }
}