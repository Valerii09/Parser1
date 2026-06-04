package org.vc.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.vc.payment.PaymentDocument;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Управляет пакетной записью платёжных документов из XML в PDF-файлы.
 * Класс отвечает только за жизненный цикл PDF-документа: открытие файла, загрузку шрифтов,
 * ограничение количества страниц в одном файле и сохранение результата. Сам макет страницы
 * вынесен в {@link XmlPaymentPdfPageRenderer}, чтобы файловая логика не смешивалась с версткой.
 */
public class XmlPaymentPdfWriter implements Closeable {

    private static final int DEFAULT_MAX_PAGES_PER_FILE = 1000;
    private static final String RESULT_FILE_PREFIX = "payment-documents";

    private final Path outputFolder;
    private final int maxPagesPerFile;

    private PDDocument document;
    private PDType0Font font;
    private PDType0Font boldFont;
    private int currentFilePages;
    private int fileNumber;
    private int writtenDocuments;

    /**
     * Создает writer с обычным лимитом страниц на один PDF-файл.
     */
    public XmlPaymentPdfWriter(Path outputFolder) throws IOException {
        this(outputFolder, DEFAULT_MAX_PAGES_PER_FILE);
    }

    /**
     * Создает writer с заданным лимитом страниц, чтобы большие XML можно было делить на несколько PDF.
     */
    public XmlPaymentPdfWriter(Path outputFolder, int maxPagesPerFile) throws IOException {
        this.outputFolder = outputFolder;
        this.maxPagesPerFile = maxPagesPerFile;

        Files.createDirectories(outputFolder);
    }

    /**
     * Добавляет одну платёжку в текущий PDF и сохраняет файл, когда достигнут лимит страниц.
     */
    public void write(PaymentDocument paymentDocument) throws IOException {
        ensureDocument();

        XmlPaymentPdfPageRenderer renderer = new XmlPaymentPdfPageRenderer(document, font, boldFont);
        currentFilePages += renderer.render(paymentDocument);
        writtenDocuments++;

        if (currentFilePages >= maxPagesPerFile) {
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
     * Показывает количество уже сохранённых PDF плюс текущий открытый файл, если в нем есть страницы.
     */
    public int getCreatedFilesCount() {
        return fileNumber + (document == null || currentFilePages == 0 ? 0 : 1);
    }

    /**
     * Сохраняет последний открытый PDF. Метод безопасно вызывать, даже если документов не было.
     */
    @Override
    public void close() throws IOException {
        saveCurrentDocument();
    }

    private void ensureDocument() throws IOException {
        if (document != null) {
            return;
        }

        document = new PDDocument();
        font = PDType0Font.load(document, PdfFontLoader.resolveFontPath("arial.ttf").toFile());
        boldFont = PDType0Font.load(document, PdfFontLoader.resolveFontPath("arialbd.ttf").toFile());
        currentFilePages = 0;
    }

    private void saveCurrentDocument() throws IOException {
        if (document == null || currentFilePages == 0) {
            return;
        }

        fileNumber++;
        Path resultFile = outputFolder.resolve("%s-%03d.pdf".formatted(RESULT_FILE_PREFIX, fileNumber));

        document.save(resultFile.toFile());
        document.close();
        document = null;
        font = null;
        boldFont = null;
        currentFilePages = 0;

        System.out.println("Создан PDF из XML: " + resultFile);
    }
}
