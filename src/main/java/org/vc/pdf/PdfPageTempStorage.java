package org.vc.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.vc.util.FileNameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Управляет временными PDF-файлами страниц во время обработки.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class PdfPageTempStorage {

    /**
     * Сохраняет одну страницу исходного документа во временный PDF-файл.
     */
    public Path savePageToTempPdf(
        PDDocument sourceDocument,
        int pageIndex,
        Path tempRoot,
        String courierName
    ) throws IOException {
        Path courierTempFolder = tempRoot.resolve(FileNameUtils.safeFileName(courierName));
        Files.createDirectories(courierTempFolder);

        Path pagePdf = Files.createTempFile(courierTempFolder, "page-", ".pdf");

        try (PDDocument pageDocument = new PDDocument()) {
            pageDocument.importPage(sourceDocument.getPage(pageIndex));
            pageDocument.save(pagePdf.toFile());
        }

        return pagePdf;
    }

    /**
     * Сохраняет вертикальную часть страницы, когда на одной физической странице находится несколько платёжек.
     */
    public Path savePagePartToTempPdf(
        PDDocument sourceDocument,
        int pageIndex,
        Path tempRoot,
        String courierName,
        int partIndex,
        int partCount
    ) throws IOException {
        Path courierTempFolder = tempRoot.resolve(FileNameUtils.safeFileName(courierName));
        Files.createDirectories(courierTempFolder);

        Path pagePdf = Files.createTempFile(courierTempFolder, "page-", ".pdf");

        try (PDDocument pageDocument = new PDDocument()) {
            PDPage page = pageDocument.importPage(sourceDocument.getPage(pageIndex));
            PDRectangle region = createVerticalRegion(page.getMediaBox(), partIndex, partCount);

            page.setMediaBox(region);
            page.setCropBox(region);
            pageDocument.save(pagePdf.toFile());
        }

        return pagePdf;
    }

    private PDRectangle createVerticalRegion(PDRectangle sourceBox, int partIndex, int partCount) {
        float partHeight = sourceBox.getHeight() / partCount;
        float lowerLeftY = sourceBox.getLowerLeftY() + partHeight * (partCount - partIndex - 1);

        return new PDRectangle(
            sourceBox.getLowerLeftX(),
            lowerLeftY,
            sourceBox.getWidth(),
            partHeight
        );
    }

    /**
     * Удаляет временный рабочий каталог, если он существует.
     */
    public void deleteTempDirectory(Path tempRoot) {
        if (tempRoot == null || !Files.exists(tempRoot)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(tempRoot)) {
            paths
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // игнорируем
                    }
                });
        } catch (IOException ignored) {
            // игнорируем
        }
    }
}
