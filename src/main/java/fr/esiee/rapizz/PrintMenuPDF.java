package fr.esiee.rapizz;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.Map;

public class PrintMenuPDF {

    
    private static final Color PRIMARY_COLOR = new Color(200, 40, 40);
    private static final Color SECONDARY_COLOR = new Color(100, 100, 100);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);

    public static void generate(List<MenuPanel.Pizza> pizzas, Map<Integer, Double> sizeAdjustments, Map<Integer, String> sizeNames) {
        try {
            
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Enregistrer le menu PDF");
            fileChooser.setSelectedFile(new java.io.File("menu_pizzeria.pdf"));
            int userSelection = fileChooser.showSaveDialog(null);

            if (userSelection != JFileChooser.APPROVE_OPTION) {
                return;
            }

            
            PdfWriter.getInstance(document, new FileOutputStream(fileChooser.getSelectedFile()));
            document.open();

            
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, com.itextpdf.text.Font.BOLD, new BaseColor(PRIMARY_COLOR.getRGB()));
            com.itextpdf.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, com.itextpdf.text.Font.BOLD, new BaseColor(SECONDARY_COLOR.getRGB()));
            com.itextpdf.text.Font pizzaNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font priceFont = FontFactory.getFont(FontFactory.HELVETICA, 12, com.itextpdf.text.Font.BOLD, new BaseColor(PRIMARY_COLOR.getRGB()));
            com.itextpdf.text.Font ingredientsFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            com.itextpdf.text.Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, new BaseColor(100, 100, 100));

            
            Paragraph title = new Paragraph("CARTE DES PIZZAS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            
            Paragraph date = new Paragraph("Menu valable au " + new java.util.Date().toString(), footerFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(30);
            document.add(date);

            
            LineSeparator separator = new LineSeparator();
            separator.setLineWidth(1);
            document.add(separator);
            document.add(Chunk.NEWLINE);

            
            PdfPTable sizeTable = new PdfPTable(3);
            sizeTable.setWidthPercentage(80);
            sizeTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            sizeTable.setSpacingBefore(10);
            sizeTable.setSpacingAfter(20);

            
            Paragraph pizzasTitle = new Paragraph("NOS PIZZAS", sectionFont);
            pizzasTitle.setSpacingBefore(20);
            pizzasTitle.setSpacingAfter(10);
            document.add(pizzasTitle);

            
            for (MenuPanel.Pizza pizza : pizzas) {
                
                PdfPTable pizzaTable = new PdfPTable(2);
                pizzaTable.setWidthPercentage(100);
                pizzaTable.setSpacingAfter(20);

                
                PdfPCell imageCell = new PdfPCell();
                imageCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                imageCell.setPaddingRight(15);
                imageCell.setVerticalAlignment(Element.ALIGN_TOP);
                
                try {
                    
                    String imagePath = "src/images/" + pizza.getName().toLowerCase().replaceAll("\\s+", "") + ".jpeg";
                    File imageFile = new File(imagePath);
                    
                    if (imageFile.exists()) {
                        
                        com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(imageFile.getAbsolutePath());
                        image.scaleToFit(150, 150);
                        image.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
                        
                        
                        image.setBorder(com.itextpdf.text.Rectangle.BOX);
                        image.setBorderWidth(1);
                        image.setBorderColor(new BaseColor(200, 200, 200));
                        
                        imageCell.addElement(image);
                    } else {
                        imageCell.addElement(new Paragraph("[Image non disponible]", ingredientsFont));
                    }
                } catch (Exception e) {
                    imageCell.addElement(new Paragraph("Erreur de chargement de l'image", ingredientsFont));
                }

                
                PdfPCell detailsCell = new PdfPCell();
                detailsCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                detailsCell.setPadding(5);
                
                
                Paragraph name = new Paragraph(pizza.getName().toUpperCase(), pizzaNameFont);
                name.setSpacingAfter(5);
                detailsCell.addElement(name);

                
                Paragraph ingredients = new Paragraph(pizza.getIngredients(), ingredientsFont);
                ingredients.setSpacingAfter(10);
                detailsCell.addElement(ingredients);

                
                PdfPTable priceTable = new PdfPTable(3);
                priceTable.setWidthPercentage(100);
                
                for (Map.Entry<Integer, String> entry : sizeNames.entrySet()) {
                    double adjust = sizeAdjustments.getOrDefault(entry.getKey(), 0.0);
                    double finalPrice = pizza.getBasePrice() * (1 + adjust);
                    
                    priceTable.addCell(createCell(entry.getValue(), Element.ALIGN_LEFT, 1, false));
                    priceTable.addCell(createCell("→", Element.ALIGN_CENTER, 1, false));
                    priceTable.addCell(createCell(String.format("%.2f€", finalPrice), Element.ALIGN_RIGHT, 1, true));
                }
                
                detailsCell.addElement(priceTable);
                
                
                pizzaTable.addCell(imageCell);
                pizzaTable.addCell(detailsCell);
                
                
                document.add(pizzaTable);

                
                if (pizzas.indexOf(pizza) < pizzas.size() - 1) {
                    LineSeparator pizzaSeparator = new LineSeparator();
                    pizzaSeparator.setLineWidth(0.5f);
                    pizzaSeparator.setLineColor(new BaseColor(200, 200, 200));
                    document.add(pizzaSeparator);
                    document.add(Chunk.NEWLINE);
                }
            }

            
            Paragraph footer = new Paragraph("Service de livraison disponible - Pizza gratuite si livrée en plus de 30 minutes", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            
            
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fileChooser.getSelectedFile());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erreur lors de la génération du PDF : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    
    private static PdfPCell createCell(String text, int alignment, int colspan, boolean highlight) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        if (highlight) {
            cell.setBackgroundColor(new BaseColor(255, 240, 240));
        }
        return cell;
    }
}
