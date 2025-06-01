package fr.esiee.rapizz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.List;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.zip.*;

public class AdminPanel extends JPanel {

    public AdminPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Administration / Paramètres");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        JPanel pizzaPanel = new JPanel(new BorderLayout(10, 10));
        String[] columns = {"ID", "Nom", "Taille", "Prix (€)"};
        DefaultTableModel pizzaModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };
        JTable pizzaTable = new JTable(pizzaModel);
        pizzaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane pizzaScroll = new JScrollPane(pizzaTable);

        Map<Integer, String> sizeIdToName = new HashMap<>();
        Map<String, Integer> sizeNameToId = new HashMap<>();
        loadPizzaSizes(sizeIdToName, sizeNameToId);

        JComboBox<String> sizeCombo = new JComboBox<>(sizeIdToName.values().toArray(new String[0]));
        pizzaTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(sizeCombo));

        JButton btnReload = new JButton("Rafraîchir");
        JButton btnAdd = new JButton("Ajouter");
        JButton btnDelete = new JButton("Supprimer");
        JButton btnSave = new JButton("Enregistrer");

        JPanel pizzaBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pizzaBtnPanel.add(btnReload);
        pizzaBtnPanel.add(btnAdd);
        pizzaBtnPanel.add(btnDelete);
        pizzaBtnPanel.add(btnSave);

        pizzaPanel.add(new JLabel("Gestion du catalogue pizzas, tailles, prix"), BorderLayout.NORTH);
        pizzaPanel.add(pizzaScroll, BorderLayout.CENTER);
        pizzaPanel.add(pizzaBtnPanel, BorderLayout.SOUTH);

        tabs.addTab("Catalogue pizzas", pizzaPanel);

        JPanel bonusPanel = new JPanel(new BorderLayout(10, 10));
        bonusPanel.add(new JLabel("Gestion des règles de bonification (configurable) (placeholder)"),
                BorderLayout.CENTER);
        tabs.addTab("Règles de bonification", bonusPanel);

        JPanel backupPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        JButton btnBackup = new JButton("Sauvegarder les données");
        JButton btnRestore = new JButton("Restaurer les données");
        backupPanel.add(btnBackup);
        backupPanel.add(btnRestore);
        JPanel backupWrapper = new JPanel(new BorderLayout());
        backupWrapper.add(new JLabel("Sauvegarde / restauration des données", SwingConstants.CENTER),
                BorderLayout.NORTH);
        backupWrapper.add(backupPanel, BorderLayout.CENTER);
        tabs.addTab("Sauvegarde / Restauration", backupWrapper);

        add(tabs, BorderLayout.CENTER);

        btnBackup.addActionListener(
                e -> JOptionPane.showMessageDialog(this, this.backup(), "Sauvegarde", JOptionPane.INFORMATION_MESSAGE));
        btnRestore.addActionListener(e -> JOptionPane.showMessageDialog(this, this.restore(), "Restauration",
                JOptionPane.INFORMATION_MESSAGE));
        btnReload.addActionListener(e -> loadPizzas(pizzaModel, sizeIdToName));
        btnAdd.addActionListener(e -> pizzaModel.addRow(new Object[]{"", "", sizeIdToName.values().iterator().next(), ""}));
        btnDelete.addActionListener(e -> {
            int row = pizzaTable.getSelectedRow();
            if (row != -1) pizzaModel.removeRow(row);
        });
        btnSave.addActionListener(e -> savePizzas(pizzaModel, sizeNameToId));

        loadPizzas(pizzaModel, sizeIdToName);
    }

    private String backup() {

        ArrayList<String> tables = getTables();

        String bDir = "backup_" + new SimpleDateFormat("dd-MM-yyyy_HH-ss").format(new Date());
        File dir = new File(bDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return "Erreur : impossible de créer le dossier de sauvegarde.";
        }
        ArrayList<String> files = new ArrayList<String>();

        for (String table : tables) {
            String csvFile = bDir + File.separator + table + ".csv";
            exportToCsv(table, csvFile);
            files.add(csvFile);
        }

        String zFile = bDir + ".zip";

        try {
            FileOutputStream fos = new FileOutputStream(zFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (String filePath : files) {
                File file = new File(filePath);
                FileInputStream fis = new FileInputStream(file);
                ZipEntry zEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zEntry);

                byte[] buff = new byte[1024];
                int len;

                while ((len = fis.read(buff)) > 0) {
                    zos.write(buff, 0, len);
                }
                zos.closeEntry();
                fis.close();
            }

            zos.close();

            for (String filePath : files) {
                new File(filePath).delete();
            }

            dir.delete();

            return "Sauvegarde terminée, fichier enregistré dans: " + zFile;
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
            return "Erreur";
        }
    }

    private void exportToCsv(String table, String fileName) {
        try {
            BufferedWriter fw = new BufferedWriter(new FileWriter(fileName));

            Connection c = DBConnection.getConnection();
            String sql = "SELECT * FROM " + table;
            Statement s = c.createStatement();

            ResultSet r = s.executeQuery(sql);
            ResultSetMetaData md = r.getMetaData();

            int noCol = md.getColumnCount();

            for (int i = 1; i <= noCol; i++) {
                fw.write(md.getColumnName(i));
                if (i < noCol) {
                    fw.write(",");
                }
            }

            fw.newLine();

            while (r.next()) {
                for (int i = 1; i <= noCol; i++) {
                    Object valObj = r.getObject(i);
                    String valStr = valObj == null ? "" : valObj.toString();

                    if (valObj instanceof String) {
                        valStr = "\"" + valStr.replace("\"", "\"\"") + "\"";
                    }

                    fw.write(valStr);

                    if (i < noCol) {
                        fw.write(",");
                    }
                }
                fw.newLine();
            }

            fw.close();

        } catch (SQLException | IOException e) {
            System.err.println(e.getStackTrace());
        }
    }

    private ArrayList<String> getTables() {
        try {
            Connection c = DBConnection.getConnection();
            DatabaseMetaData md = c.getMetaData();
            ResultSet rs = md.getTables(null, "projet_bdd", "%", null);

            ArrayList<String> tables = new ArrayList<String>();

            while (rs.next()) {
                if (rs.getString(1).equals("projet_bdd")) {
                    tables.add(rs.getString(3));
                }
            }

            return tables;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<String>();
        }
    }

    private String restore() {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Sélectionnez la sauvegarde");

        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            return "Restauration annulée.";
        }

        File zfile = chooser.getSelectedFile();
        String eDir = "restore_" + new SimpleDateFormat("dd-MM-yyyy_HH-ss").format(new Date());
        File dir = new File(eDir);

        if (!dir.exists() != !dir.mkdirs()) {
            return "Impossible de créer le dossier";
        }

        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zfile));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                File oFile = new File(dir, entry.getName());
                FileOutputStream fos = new FileOutputStream(oFile);
                byte[] buff = new byte[1024];
                int len;

                while ((len = zis.read(buff)) > 0) {
                    fos.write(buff, 0, len);
                }
            }
            zis.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
            return "Erreur de décompression: " + e.getMessage();
        }

        try {
            Connection c = DBConnection.getConnection();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));
            if (files == null) {
                return "Aucun CSV";
            }

            for (File csv : files) {
                importCsv(c, csv);
            }

            return "Restauration terminée.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur :" + e.getMessage();
        }
    }

    private void importCsv(Connection c, File file) {
        String table = file.getName().replace(".csv", "");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String header = br.readLine();
            if (header == null) {
                br.close();
                return;
            };
            String[] columns = header.split(",");
            String placeholders = String.join(",", Collections.nCopies(columns.length, "?"));
            String insertSql = "INSERT INTO " + table + " (" + String.join(",", columns) + ") VALUES (" + placeholders + ")";
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = parseCsvLine(line, columns.length);
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    for (int i = 0; i < columns.length; i++) {
                        ps.setString(i + 1, values[i]);
                    }
                    ps.executeUpdate();
                }
            }

            br.close();
        } catch (SQLException | IOException e) {

        }
    }

    private String[] parseCsvLine(String line, int columnCount) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        int col = 0;
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().replace("\"\"", "\""));
                sb.setLength(0);
                col++;
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().replace("\"\"", "\""));
        while (values.size() < columnCount)
            values.add("");
        return values.toArray(new String[0]);
    }

    private void loadPizzaSizes(Map<Integer, String> idToName, Map<String, Integer> nameToId) {
        idToName.clear();
        nameToId.clear();
        String sql = "SELECT id, size FROM pizza_sizes";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("size");
                idToName.put(id, name);
                nameToId.put(name, id);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement tailles : " + e.getMessage());
        }
    }

    private void loadPizzas(DefaultTableModel model, Map<Integer, String> sizeIdToName) {
        model.setRowCount(0);
        String sql = "SELECT id, name, price, size_id FROM pizzas";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                int sizeId = rs.getInt("size_id");
                String sizeName = sizeIdToName.getOrDefault(sizeId, "inconnu");
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    sizeName,
                    rs.getDouble("price")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement pizzas : " + e.getMessage());
        }
    }

    private void savePizzas(DefaultTableModel model, Map<String, Integer> sizeNameToId) {
        try (Connection c = DBConnection.getConnection()) {
            Statement s = c.createStatement();
            s.executeUpdate("DELETE FROM pizzas");
            String sql = "INSERT INTO pizzas (id, name, price, size_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    Object id = model.getValueAt(i, 0);
                    Object name = model.getValueAt(i, 1);
                    Object size = model.getValueAt(i, 2);
                    Object price = model.getValueAt(i, 3);
                    ps.setObject(1, id.equals("") ? null : id);
                    ps.setString(2, name == null ? "" : name.toString());
                    ps.setDouble(3, price == null || price.toString().isEmpty() ? 0.0 : Double.parseDouble(price.toString()));
                    ps.setInt(4, sizeNameToId.getOrDefault(size == null ? "" : size.toString(), 1));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            JOptionPane.showMessageDialog(this, "Catalogue pizzas sauvegardé !");
            loadPizzas(model, invertMap(sizeNameToId));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur sauvegarde pizzas : " + e.getMessage());
        }
    }

    private Map<Integer, String> invertMap(Map<String, Integer> map) {
        Map<Integer, String> inv = new HashMap<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            inv.put(e.getValue(), e.getKey());
        }
        return inv;
    }
}