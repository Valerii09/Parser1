package org.vc.app;

import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfAddressEnlarger {

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(?m)^\\s*(Адрес:\\s*.+?)\\s*$");

    /**
     * Насколько весь документ опускаем вниз.
     * Если снизу начнёт обрезаться текст/пунктир — уменьшить до 20-24.
     * Если сверху мало места для адреса — увеличить до 30-34.
     */
    private static final float DOCUMENT_SHIFT_DOWN = 28f;

    /**
     * Координаты крупного дубля адреса.
     * Y задаётся от верхнего края страницы.
     */
    private static final float DUPLICATE_ADDRESS_X = 25f;
    private static final float DUPLICATE_ADDRESS_Y_FROM_TOP = 10f;

    /**
     * Примерно +25% к исходному размеру.
     */
    private static final float DUPLICATE_ADDRESS_FONT_SIZE = 10.5f;

    private static final String FONT_PATH = "C:\\Windows\\Fonts\\arial.ttf";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java PdfAddressEnlarger <input.pdf>");
            return;
        }

        File inputPdf = new File(args[0]);

        if (!inputPdf.exists()) {
            System.out.println("Input file not found: " + inputPdf.getAbsolutePath());
            return;
        }

        File outputPdf = buildOutputPdfFile(inputPdf);
        File fontFile = new File(FONT_PATH);

        if (!fontFile.exists()) {
            System.out.println("Font file not found: " + fontFile.getAbsolutePath());
            return;
        }

        try (PDDocument sourceDocument = PDDocument.load(inputPdf);
             PDDocument resultDocument = new PDDocument()) {

            PDType0Font font = PDType0Font.load(resultDocument, fontFile);
            LayerUtility layerUtility = new LayerUtility(resultDocument);

            for (int pageIndex = 0; pageIndex < sourceDocument.getNumberOfPages(); pageIndex++) {
                PDPage sourcePage = sourceDocument.getPage(pageIndex);
                PDPage resultPage = createSameSizePage(sourcePage);
                resultDocument.addPage(resultPage);

                PDFormXObject pageForm = layerUtility.importPageAsForm(sourceDocument, pageIndex);
                String address = extractAddress(sourceDocument, pageIndex);

                if (isBlank(address)) {
                    drawOriginalPageShiftedDown(resultDocument, resultPage, pageForm);
                    System.out.println("Address not found on page " + (pageIndex + 1));
                    continue;
                }

                drawPageWithDuplicateAddress(resultDocument, resultPage, pageForm, font, address);
                System.out.println("Page " + (pageIndex + 1) + ": " + address);
            }

            resultDocument.save(outputPdf);
            System.out.println("Result saved to: " + outputPdf.getAbsolutePath());
        }
    }

    private static PDPage createSameSizePage(PDPage sourcePage) {
        PDRectangle mediaBox = sourcePage.getMediaBox();

        PDPage resultPage = new PDPage(new PDRectangle(mediaBox.getWidth(), mediaBox.getHeight()));
        resultPage.setResources(new PDResources());

        return resultPage;
    }

    private static String extractAddress(PDDocument document, int pageIndex) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);

        String pageText = stripper.getText(document);
        Matcher matcher = ADDRESS_PATTERN.matcher(pageText);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim();
    }

    private static void drawPageWithDuplicateAddress(
        PDDocument document,
        PDPage page,
        PDFormXObject pageForm,
        PDType0Font font,
        String address
    ) throws IOException {
        PDRectangle pageBox = page.getMediaBox();
        float pageHeight = pageBox.getHeight();

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.saveGraphicsState();

            // В PDFBox координаты идут снизу вверх.
            // Отрицательный Y визуально двигает страницу вниз.
            contentStream.transform(Matrix.getTranslateInstance(0, -DOCUMENT_SHIFT_DOWN));
            contentStream.drawForm(pageForm);

            contentStream.restoreGraphicsState();

            drawDuplicateAddress(contentStream, font, address, pageHeight);
        }
    }

    private static void drawOriginalPageShiftedDown(
        PDDocument document,
        PDPage page,
        PDFormXObject pageForm
    ) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.saveGraphicsState();
            contentStream.transform(Matrix.getTranslateInstance(0, -DOCUMENT_SHIFT_DOWN));
            contentStream.drawForm(pageForm);
            contentStream.restoreGraphicsState();
        }
    }

    private static void drawDuplicateAddress(
        PDPageContentStream contentStream,
        PDType0Font font,
        String address,
        float pageHeight
    ) throws IOException {
        contentStream.setNonStrokingColor(Color.BLACK);

        contentStream.beginText();
        contentStream.setFont(font, DUPLICATE_ADDRESS_FONT_SIZE);
        contentStream.newLineAtOffset(
            DUPLICATE_ADDRESS_X,
            pageHeight - DUPLICATE_ADDRESS_Y_FROM_TOP - DUPLICATE_ADDRESS_FONT_SIZE
        );
        contentStream.showText(address);
        contentStream.endText();
    }

    private static File buildOutputPdfFile(File inputPdf) {
        String fileName = inputPdf.getName();

        if (fileName.toLowerCase().endsWith(".pdf")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }

        return new File(inputPdf.getParentFile(), fileName + "_адрес_крупнее.pdf");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}