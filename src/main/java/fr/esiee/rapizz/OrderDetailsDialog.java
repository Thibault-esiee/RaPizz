package fr.esiee.rapizz;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class OrderDetailsDialog {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void show(Component parent, Order order) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent) instanceof Frame f ? f : null, 
                                   "Détails de la commande " + order.getId(), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(parent);

        JLabel titleLabel = new JLabel("Détails de la commande #" + order.getId(), SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
        dialog.add(titleLabel, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        addInfoRow(infoPanel, "Client:", order.getCustomer());
        addInfoRow(infoPanel, "Pizza:", order.getPizza());
        addInfoRow(infoPanel, "Date commande:", formatTimestamp(order.getOrderTime()));
        addInfoRow(infoPanel, "Date livraison:", formatTimestamp(order.getDeliveryTime()));
        addInfoRow(infoPanel, "Prix de base (€):", order.getBasePrice() > 0 ? String.format("%.2f", order.getBasePrice()) : "Gratuit");
        addInfoRow(infoPanel, "Prix total (€):", String.format("%.2f", order.getTotalPrice()));
        addInfoRow(infoPanel, "Gratuite:", !order.isPaid() ? "Oui" : "Non");
        addInfoRow(infoPanel, "Livreur:", order.getDeliverer());
        addInfoRow(infoPanel, "Type de véhicule:", order.getVehicle());
        addInfoRow(infoPanel, "Délai (minutes):", order.getDelayMinutes() > 0 ? String.valueOf(order.getDelayMinutes()) : "N/A");

        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Fermer");
        JButton printButton = new JButton("Exporter en PDF");

        printButton.addActionListener(e -> printPDF(order, dialog));
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.add(printButton);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setResizable(true);
        dialog.setVisible(true);
    }

    private static void addInfoRow(JPanel panel, String label, Object value) {
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(labelComp.getFont().deriveFont(Font.BOLD));
        panel.add(labelComp);
        
        JLabel valueComp = new JLabel(value != null ? value.toString() : "N/A");
        panel.add(valueComp);
    }

    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "En attente";
        return timestamp.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private static void printPDF(Order order, Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("commande_" + order.getId() + ".pdf"));
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                Document document = new Document();
                PdfWriter.getInstance(document, fos);
                document.open();

                Paragraph title = new Paragraph("Détails de la commande n°" + order.getId());
                title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                document.add(title);
                
                document.add(new Paragraph(" "));
                
                addPdfField(document, "Client", order.getCustomer());
                addPdfField(document, "Pizza", order.getPizza());
                addPdfField(document, "Date commande", formatTimestamp(order.getOrderTime()));
                addPdfField(document, "Date livraison", formatTimestamp(order.getDeliveryTime()));
                addPdfField(document, "Prix de base (€)", order.getBasePrice() > 0 ? String.format("%.2f", order.getBasePrice()) : "Gratuit");
                addPdfField(document, "Prix total (€)", String.format("%.2f", order.getTotalPrice()));
                addPdfField(document, "Gratuite", !order.isPaid() ? "Oui" : "Non");
                addPdfField(document, "Livreur", order.getDeliverer());
                addPdfField(document, "Type de véhicule", order.getVehicle());
                addPdfField(document, "Délai (minutes)", order.getDelayMinutes() > 0 ? String.valueOf(order.getDelayMinutes()) : "N/A");

                document.close();
                
                JOptionPane.showMessageDialog(parent, 
                    "Le fichier a été enregistré avec succès :\n" + file.getAbsolutePath(),
                    "Export réussi", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, 
                    "Erreur lors de l'export PDF : " + ex.getMessage(),
                    "Erreur d'export", 
                    JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
    
    private static void addPdfField(Document document, String label, String value) throws com.lowagie.text.DocumentException {
        if (value == null) value = "N/A";
        document.add(new Paragraph(label + ": " + value));
    }
}