package fr.esiee.rapizz;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import fr.esiee.rapizz.Order;

public class OrderDetailsDialog {

    public static void show(Component parent, Order order) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent) instanceof Frame f ? f : null, "Détails de la commande", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(parent);

        JLabel titleLabel = new JLabel("Détails de la commande", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
        dialog.add(titleLabel, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Informations de la commande"));

        infoPanel.add(new JLabel("ID Commande:"));
        infoPanel.add(new JLabel(String.valueOf(order.getId())));

        infoPanel.add(new JLabel("Client:"));
        infoPanel.add(new JLabel(value(order.getCustomer())));

        infoPanel.add(new JLabel("Pizza:"));
        infoPanel.add(new JLabel(value(order.getPizza())));

        infoPanel.add(new JLabel("Date commande:"));
        infoPanel.add(new JLabel(order.getOrderTime().toString()));

        infoPanel.add(new JLabel("Date livraison:"));
        infoPanel.add(new JLabel(order.getDeliveryTime().toString()));

        infoPanel.add(new JLabel("Prix de base (€):"));
        infoPanel.add(new JLabel(String.format("%.2f", order.getBasePrice())));

        infoPanel.add(new JLabel("Prix total (€):"));
        infoPanel.add(new JLabel(String.format("%.2f", order.getTotalPrice())));

        infoPanel.add(new JLabel("Gratuite:"));
        infoPanel.add(new JLabel(!order.isPaid() ? "Oui" : "Non"));

        infoPanel.add(new JLabel("Livreur:"));
        infoPanel.add(new JLabel(value(order.getDeliverer())));

        infoPanel.add(new JLabel("Type de véhicule:"));
        infoPanel.add(new JLabel(value(order.getVehicle())));

        infoPanel.add(new JLabel("Délai (minutes):"));
        infoPanel.add(new JLabel(String.valueOf(order.getDelayMinutes())));

        JButton closeButton = new JButton("Fermer");
        JButton printButton = new JButton("Imprimer PDF");

        printButton.addActionListener(e -> printPDF(order, dialog));
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);
        buttonPanel.add(printButton);

        dialog.add(infoPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static String value(Object val) {
        return val != null ? val.toString() : "N/A";
    }

    private static void printPDF(Order order, Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("commande_" + order.getId() + ".pdf"));
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
                document.open();

                document.add(new Paragraph("Détails de la commande n°" + order.getId()));
                document.add(new Paragraph("---------------------------"));
                document.add(new Paragraph("Client: " + order.getCustomer()));
                document.add(new Paragraph("Pizza: " + order.getPizza()));
                document.add(new Paragraph("Date commande: " + order.getOrderTime()));
                document.add(new Paragraph("Date livraison: " + order.getDeliveryTime()));
                document.add(new Paragraph("Prix de base (€): " + order.getBasePrice()));
                document.add(new Paragraph("Prix total (€): " + order.getTotalPrice()));
                document.add(new Paragraph("Gratuite: " + (!order.isPaid() ? "Oui" : "Non")));
                document.add(new Paragraph("Livreur: " + order.getDeliverer()));
                document.add(new Paragraph("Type de véhicule: " + order.getVehicle()));
                document.add(new Paragraph("Délai (minutes): " + order.getDelayMinutes()));

                document.close();
                JOptionPane.showMessageDialog(parent, "PDF enregistré avec succès !");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Erreur : " + ex.getMessage());
            }
        }
    }
}