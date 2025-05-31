package fr.esiee.rapizz;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;

public class PrintMenuPDF {

    public static void generate(List<MenuPanel.Pizza> pizzas, Map<Integer, Double> sizeAdjustments, Map<Integer, String> sizeNames) {
        try {
            Document document = new Document();
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Enregistrer le menu PDF");
            fileChooser.setSelectedFile(new java.io.File("menu_pizzeria.pdf"));
            int userSelection = fileChooser.showSaveDialog(null);

            if (userSelection != JFileChooser.APPROVE_OPTION) {
                return;
            }

            PdfWriter.getInstance(document, new FileOutputStream(fileChooser.getSelectedFile()));
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font textFont = new Font(Font.FontFamily.HELVETICA, 12);

            for (MenuPanel.Pizza pizza : pizzas) {
                document.add(new Paragraph("🍕 " + pizza.getName(), titleFont));
                document.add(new Paragraph("Ingrédients : " + pizza.getIngredients(), textFont));
                document.add(Chunk.NEWLINE);

                PdfPTable table = new PdfPTable(2);
                table.setWidthPercentage(50);
                table.addCell("Taille");
                table.addCell("Prix (€)");

                for (String sizeName : List.of("naine", "humaine", "ogresse")) {
                    int sizeId = sizeNames.entrySet().stream()
                        .filter(e -> e.getValue().equals(sizeName))
                        .map(Map.Entry::getKey).findFirst().orElse(-1);
                    if (sizeId != -1) {
                        double adjust = sizeAdjustments.getOrDefault(sizeId, 0.0);
                        double price = pizza.getBasePrice() * (1 + adjust);
                        table.addCell(sizeName);
                        table.addCell(String.format("%.2f", price));
                    }
                }

                document.add(table);

                try {
                    String imagePath = "src/images/" + pizza.getName().replaceAll("\\s+", "") + ".jpeg";
                    ImageIcon icon = new ImageIcon(imagePath);
                    Image awtImage = icon.getImage();
                    BufferedImage bufferedImage = new BufferedImage(
                        awtImage.getWidth(null),
                        awtImage.getHeight(null),
                        BufferedImage.TYPE_INT_RGB
                    );
                    Graphics2D g = bufferedImage.createGraphics();
                    g.drawImage(awtImage, 0, 0, null);
                    g.dispose();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "jpg", baos);
                    baos.flush();
                    com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(baos.toByteArray());
                    pdfImage.scaleToFit(200, 150);
                    document.add(pdfImage);
                    baos.close();
                } catch (Exception e) {
                    document.add(new Paragraph("[Image non disponible]", textFont));
                }

                document.add(Chunk.NEWLINE);
                document.add(new LineSeparator());
                document.add(Chunk.NEWLINE);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
