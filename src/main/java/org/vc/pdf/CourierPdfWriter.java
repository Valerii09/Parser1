package org.vc.pdf;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.vc.report.CourierAddressRegistryBuilder;
import org.vc.report.CourierAddressRegistryRow;
import org.vc.report.CourierReportWriter;
import org.vc.report.CourierSummary;
import org.vc.report.ProcessingStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Сортирует временные страницы и последовательно собирает PDF-файлы курьеров.
 *
 * <p>Последовательная запись является намеренным ограничением: параллельное
 * сохранение нескольких документов по 5000 страниц резко увеличивает пиковый
 * расход памяти PDFBox и может привести к {@link OutOfMemoryError}.</p>
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierPdfWriter {

    private static final String RESULT_FILE_PREFIX = "платежка";
    private static final String PDF_EXTENSION = ".pdf";
    private static final int DEFAULT_MAX_PAGES_PER_FILE = 5000;

    private final CourierPageComparator pageComparator = new CourierPageComparator();
    private final CourierAddressRegistryBuilder addressRegistryBuilder = new CourierAddressRegistryBuilder();
    private final CourierReportWriter reportWriter = new CourierReportWriter();
    private final int maxPagesPerFile;

    /**
     * Создаёт writer с ограничением до 5000 страниц в одном итоговом PDF.
     */
    public CourierPdfWriter() {
        this(Integer.getInteger("paymentCourier.maxPagesPerFile", DEFAULT_MAX_PAGES_PER_FILE));
    }

    CourierPdfWriter(int maxPagesPerFile) {
        if (maxPagesPerFile <= 0) {
            throw new IllegalArgumentException("Количество страниц в одном PDF должно быть больше нуля");
        }

        this.maxPagesPerFile = maxPagesPerFile;
    }

    /**
     * Поочерёдно создаёт PDF каждого курьера, после чего записывает сводные отчёты.
     */
    public void writeCourierPdfs(
        Path couriersRoot,
        Map<String, List<CourierPage>> courierPages,
        ProcessingStats stats
    ) throws IOException {
        List<CourierSummary> summaries = new ArrayList<>();
        List<CourierAddressRegistryRow> registryRows = new ArrayList<>();

        for (Map.Entry<String, List<CourierPage>> entry : courierPages.entrySet()) {
            String courierName = entry.getKey();
            List<CourierPage> pages = new ArrayList<>(entry.getValue());

            if (pages.isEmpty()) {
                System.out.println("Для курьера не найдено платёжек: " + courierName);
                continue;
            }

            int inputPagesCount = countPhysicalPages(pages);
            pages.sort(pageComparator);
            int outputPagesCount = countPhysicalPages(pages);

            registryRows.addAll(addressRegistryBuilder.build(courierName, pages));

            Path courierFolder = couriersRoot.resolve(courierName);
            Files.createDirectories(courierFolder);

            int createdFilesCount = writeCourierPdfParts(courierFolder, courierName, pages);
            stats.incrementCreatedCourierPdfFiles(createdFilesCount);
            stats.addWrittenPages(outputPagesCount);

            summaries.add(new CourierSummary(
                courierName,
                inputPagesCount,
                outputPagesCount,
                pages.get(0).getAddress(),
                pages.get(pages.size() - 1).getAddress()
            ));

            System.out.println(
                "Создано PDF для курьера " + courierName
                    + ": файлов " + createdFilesCount
                    + ", на выходе сортировки: " + outputPagesCount
            );
        }

        reportWriter.write(couriersRoot, summaries, registryRows);
    }

    private int countPhysicalPages(List<CourierPage> pages) {
        return pages.stream()
            .mapToInt(CourierPage::getPhysicalPagesCount)
            .sum();
    }

    private int writeCourierPdfParts(
        Path courierFolder,
        String courierName,
        List<CourierPage> pages
    ) throws IOException {
        int createdFilesCount = 0;
        List<CourierPage> currentPart = new ArrayList<>();
        int currentPartPagesCount = 0;

        for (CourierPage page : pages) {
            int paymentPagesCount = page.getPhysicalPagesCount();

            if (!currentPart.isEmpty()
                && currentPartPagesCount + paymentPagesCount > maxPagesPerFile) {
                createdFilesCount++;
                writeCourierPdfPart(
                    courierFolder,
                    courierName,
                    createdFilesCount,
                    currentPart,
                    currentPartPagesCount
                );

                currentPart = new ArrayList<>();
                currentPartPagesCount = 0;
            }

            currentPart.add(page);
            currentPartPagesCount += paymentPagesCount;
        }

        if (!currentPart.isEmpty()) {
            createdFilesCount++;
            writeCourierPdfPart(
                courierFolder,
                courierName,
                createdFilesCount,
                currentPart,
                currentPartPagesCount
            );
        }

        return createdFilesCount;
    }

    private void writeCourierPdfPart(
        Path courierFolder,
        String courierName,
        int fileNumber,
        List<CourierPage> pages,
        int pagesCount
    ) throws IOException {
        Path resultPdf = courierFolder.resolve(getResultFileName(courierName, fileNumber));

        writePdf(resultPdf, pages);
        deleteMergedTempFiles(pages);

        System.out.println("Создан файл: " + resultPdf + ", страниц: " + pagesCount);
    }

    private void writePdf(Path resultPdf, List<CourierPage> pages) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(resultPdf.toString());
        merger.setDocumentMergeMode(PDFMergerUtility.DocumentMergeMode.OPTIMIZE_RESOURCES_MODE);

        for (CourierPage page : pages) {
            for (Path pageFile : page.getPageFiles()) {
                merger.addSource(pageFile.toFile());
            }
        }

        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
    }

    /**
     * Удаляет уже записанные временные страницы сразу после успешной сборки части.
     */
    private void deleteMergedTempFiles(List<CourierPage> pages) {
        for (CourierPage page : pages) {
            for (Path pageFile : page.getPageFiles()) {
                try {
                    Files.deleteIfExists(pageFile);
                } catch (IOException ignored) {
                    // Общая очистка временного каталога остаётся страховкой.
                }
            }
        }
    }

    private String getResultFileName(String courierName, int fileNumber) {
        String safeCourierName = courierName
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("\\s+", "_")
            .toLowerCase(Locale.ROOT);

        return RESULT_FILE_PREFIX + "_" + safeCourierName + "_" + fileNumber + PDF_EXTENSION;
    }
}
