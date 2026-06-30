package org.vc.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.vc.payment.PaymentDocument;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Формирует PDF-платёжки из XML через HTML-шаблон.
 * В отличие от ручной PDFBox-верстки, здесь Java собирает данные, а внешний вид находится
 * в `resources/templates/payment`, поэтому таблицы и отступы можно править через CSS.
 */
public class XmlPaymentHtmlPdfWriter implements Closeable {

    private static final int DEFAULT_MAX_DOCUMENTS_PER_FILE = 1000;
    private static final String RESULT_FILE_PREFIX = "payment-documents";
    private static final String CSS_RESOURCE = "/templates/payment/payment-document.css";

    private final Path outputFolder;
    private final int maxDocumentsPerFile;
    private final HtmlPaymentDocumentRenderer documentRenderer;
    private final List<String> pendingDocuments = new ArrayList<>();

    private int fileNumber;
    private int writtenDocuments;

    /**
     * Создает writer с обычным лимитом платёжек на один PDF-файл.
     */
    public XmlPaymentHtmlPdfWriter(Path outputFolder) throws IOException {
        this(outputFolder, DEFAULT_MAX_DOCUMENTS_PER_FILE);
    }

    /**
     * Создает writer с заданным лимитом платёжек, чтобы большие XML можно было делить на несколько PDF.
     */
    public XmlPaymentHtmlPdfWriter(Path outputFolder, int maxDocumentsPerFile) throws IOException {
        this.outputFolder = outputFolder;
        this.maxDocumentsPerFile = maxDocumentsPerFile;
        this.documentRenderer = new HtmlPaymentDocumentRenderer();

        Files.createDirectories(outputFolder);
    }

    /**
     * Добавляет одну платёжку в HTML-пачку и сохраняет PDF, когда достигнут лимит.
     */
    public void write(PaymentDocument paymentDocument) throws IOException {
        pendingDocuments.add(documentRenderer.render(paymentDocument));
        writtenDocuments++;

        if (pendingDocuments.size() >= maxDocumentsPerFile) {
            saveCurrentDocument();
        }
    }

    /**
     * Показывает, сколько платёжек было принято writer-ом за текущий запуск генерации.
     */
    public int getWrittenDocuments() {
        return writtenDocuments;
    }

    /**
     * Показывает количество уже сохранённых PDF плюс текущая несохранённая пачка, если она есть.
     */
    public int getCreatedFilesCount() {
        return fileNumber + (pendingDocuments.isEmpty() ? 0 : 1);
    }

    /**
     * Сохраняет последнюю открытую пачку платёжек.
     */
    @Override
    public void close() throws IOException {
        saveCurrentDocument();
    }

    private void saveCurrentDocument() throws IOException {
        if (pendingDocuments.isEmpty()) {
            return;
        }

        fileNumber++;
        Path resultFile = outputFolder.resolve("%s-%03d.pdf".formatted(RESULT_FILE_PREFIX, fileNumber));

        try (OutputStream outputStream = Files.newOutputStream(resultFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(PdfFontLoader.resolveFontPath("arial.ttf").toFile(), "Arial");
            builder.useFont(PdfFontLoader.resolveFontPath("arialbd.ttf").toFile(), "ArialBold");
            builder.withHtmlContent(fullHtml(), null);
            builder.toStream(outputStream);
            builder.run();
        } catch (Exception exception) {
            throw new IOException("Не удалось сформировать PDF из HTML-шаблона", exception);
        }

        pendingDocuments.clear();

        System.out.println("Создан PDF из XML: " + resultFile);
    }

    private String fullHtml() throws IOException {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <style>
            %s
                </style>
            </head>
            <body>
            %s
            </body>
            </html>
            """.formatted(readCss(), String.join("\n", pendingDocuments));
    }

    private String readCss() throws IOException {
        try (InputStream inputStream = XmlPaymentHtmlPdfWriter.class.getResourceAsStream(CSS_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Не найден CSS-шаблон: " + CSS_RESOURCE);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
