package org.vc.pdf;

import org.vc.report.ProcessingStats;
import org.vc.report.CourierAddressRegistryBuilder;
import org.vc.report.CourierAddressRegistryRow;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Формирует PDF-файлы курьеров и сводную таблицу сортировки.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierPdfWriter {

    private static final String RESULT_FILE_PREFIX = "платежка";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String SUMMARY_FILE_NAME = "Итог сортировки.xlsx";
    private static final String ADDRESS_REGISTRY_FILE_NAME = "Реестр адресов.xlsx";
    private static final int MAX_PAGES_PER_FILE = 5000;

    private final CourierPageComparator pageComparator = new CourierPageComparator();
    private final CourierAddressRegistryBuilder addressRegistryBuilder = new CourierAddressRegistryBuilder();
    private final int maxPagesPerFile;

    /**
     * Создаёт writer с безопасным ограничением размера одного итогового PDF.
     *
     * <p>Ограничение не позволяет PDFBox удерживать в памяти структуру сразу
     * нескольких тысяч страниц во время сохранения документа.</p>
     */
    public CourierPdfWriter() {
        this(MAX_PAGES_PER_FILE);
    }

    CourierPdfWriter(int maxPagesPerFile) {
        if (maxPagesPerFile <= 0) {
            throw new IllegalArgumentException("Количество страниц в одном PDF должно быть больше нуля");
        }

        this.maxPagesPerFile = maxPagesPerFile;
    }

    /**
     * Записывает PDF-файлы курьеров и итоговую таблицу сортировки.
     */
    public void writeCourierPdfs(
        Path couriersRoot,
        Map<String, List<CourierPage>> courierPages,
        ProcessingStats stats
    ) throws IOException {
        List<CourierSummaryRow> summaryRows = new ArrayList<>();
        List<CourierAddressRegistryRow> addressRegistryRows = new ArrayList<>();

        for (Map.Entry<String, List<CourierPage>> entry : courierPages.entrySet()) {
            String courierName = entry.getKey();

            int inputPagesCount = countPhysicalPages(entry.getValue());
            List<CourierPage> pages = new ArrayList<>(entry.getValue());

            if (pages.isEmpty()) {
                System.out.println("Для курьера не найдено платежек: " + courierName);
                continue;
            }

            pages.sort(pageComparator);
            addressRegistryRows.addAll(addressRegistryBuilder.build(courierName, pages));

            int outputPagesCount = countPhysicalPages(pages);

            Path courierFolder = couriersRoot.resolve(courierName);
            Files.createDirectories(courierFolder);

            int createdFilesCount = writeCourierPdfParts(courierFolder, courierName, pages);

            stats.incrementCreatedCourierPdfFiles(createdFilesCount);
            stats.addWrittenPages(outputPagesCount);

            summaryRows.add(new CourierSummaryRow(
                courierName,
                inputPagesCount,
                outputPagesCount,
                pages.get(0).getAddress(),
                pages.get(pages.size() - 1).getAddress()
            ));

            System.out.println(
                "Создано PDF для курьера " + courierName
                    + ": файлов " + createdFilesCount
                    + ", на входе сортировки: " + inputPagesCount
                    + ", на выходе сортировки: " + outputPagesCount
            );
        }

        writeSummary(couriersRoot, summaryRows);
        writeAddressRegistry(couriersRoot, addressRegistryRows);
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
                writeCourierPdfPart(courierFolder, courierName, createdFilesCount, currentPart, currentPartPagesCount);

                currentPart = new ArrayList<>();
                currentPartPagesCount = 0;
            }

            currentPart.add(page);
            currentPartPagesCount += paymentPagesCount;
        }

        if (!currentPart.isEmpty()) {
            createdFilesCount++;
            writeCourierPdfPart(courierFolder, courierName, createdFilesCount, currentPart, currentPartPagesCount);
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

        System.out.println(
            "Создан файл: " + resultPdf
                + ", страниц: " + pagesCount
        );
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

    private String getResultFileName(String courierName, int fileNumber) {
        String safeCourierName = toSafeFileName(courierName)
            .replaceAll("\\s+", "_")
            .toLowerCase();

        return RESULT_FILE_PREFIX
            + "_"
            + safeCourierName
            + "_"
            + fileNumber
            + PDF_EXTENSION;
    }

    private String toSafeFileName(String value) {
        return value
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private void writeSummary(Path couriersRoot, List<CourierSummaryRow> rows) throws IOException {
        Path resultFile = couriersRoot.resolve(SUMMARY_FILE_NAME);

        rows.sort(Comparator
            .comparingInt((CourierSummaryRow row) -> extractCourierNumber(row.getCourierName()))
            .thenComparing(CourierSummaryRow::getCourierName, String.CASE_INSENSITIVE_ORDER)
        );

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Итог");

            createSummaryHeader(sheet);

            int rowIndex = 1;
            int totalInputCount = 0;
            int totalOutputCount = 0;

            for (CourierSummaryRow summaryRow : rows) {
                writeSummaryRow(sheet.createRow(rowIndex++), summaryRow);

                totalInputCount += summaryRow.getInputPagesCount();
                totalOutputCount += summaryRow.getOutputPagesCount();
            }

            writeTotalRow(sheet.createRow(rowIndex), totalInputCount, totalOutputCount);

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream outputStream = Files.newOutputStream(resultFile)) {
                workbook.write(outputStream);
            }
        }

        System.out.println("Создан итоговый файл сортировки: " + resultFile);
    }

    private void writeAddressRegistry(Path couriersRoot, List<CourierAddressRegistryRow> rows) throws IOException {
        Path resultFile = couriersRoot.resolve(ADDRESS_REGISTRY_FILE_NAME);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Реестр");

            createAddressRegistryHeader(sheet);

            int rowIndex = 1;

            for (CourierAddressRegistryRow registryRow : rows) {
                writeAddressRegistryRow(sheet.createRow(rowIndex++), registryRow);
            }

            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream outputStream = Files.newOutputStream(resultFile)) {
                workbook.write(outputStream);
            }
        }

        System.out.println("Создан реестр адресов: " + resultFile);
    }

    private void writeSummaryRow(Row row, CourierSummaryRow summaryRow) {
        row.createCell(0).setCellValue(summaryRow.getCourierName());
        row.createCell(1).setCellValue(summaryRow.getInputPagesCount());
        row.createCell(2).setCellValue(summaryRow.getOutputPagesCount());
        row.createCell(3).setCellValue(summaryRow.getFirstAddress());
        row.createCell(4).setCellValue(summaryRow.getLastAddress());
    }

    private void writeTotalRow(Row row, int totalInputCount, int totalOutputCount) {
        row.createCell(0).setCellValue("Итого");
        row.createCell(1).setCellValue(totalInputCount);
        row.createCell(2).setCellValue(totalOutputCount);
        row.createCell(3).setCellValue("");
        row.createCell(4).setCellValue("");
    }

    private void createSummaryHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Курьер");
        header.createCell(1).setCellValue("На входе сортировки");
        header.createCell(2).setCellValue("На выходе сортировки");
        header.createCell(3).setCellValue("Первый адрес");
        header.createCell(4).setCellValue("Последний адрес");
    }

    private void createAddressRegistryHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Адрес");
        header.createCell(1).setCellValue("Количество ЛС");
        header.createCell(2).setCellValue("Курьер");
    }

    private void writeAddressRegistryRow(Row row, CourierAddressRegistryRow registryRow) {
        row.createCell(0).setCellValue(registryRow.getAddress());
        row.createCell(1).setCellValue(registryRow.getPaymentDocumentsCount());
        row.createCell(2).setCellValue(registryRow.getCourierName());
    }

    private int extractCourierNumber(String courierName) {
        String number = courierName.replaceAll("\\D+", "");

        if (number.isBlank()) {
            return Integer.MAX_VALUE;
        }

        return Integer.parseInt(number);
    }

    private static class CourierSummaryRow {

        private final String courierName;
        private final int inputPagesCount;
        private final int outputPagesCount;
        private final String firstAddress;
        private final String lastAddress;

        private CourierSummaryRow(
            String courierName,
            int inputPagesCount,
            int outputPagesCount,
            String firstAddress,
            String lastAddress
        ) {
            this.courierName = courierName;
            this.inputPagesCount = inputPagesCount;
            this.outputPagesCount = outputPagesCount;
            this.firstAddress = firstAddress;
            this.lastAddress = lastAddress;
        }

        
        public String getCourierName() {
            return courierName;
        }

        
        public int getInputPagesCount() {
            return inputPagesCount;
        }

        
        public int getOutputPagesCount() {
            return outputPagesCount;
        }

        
        public String getFirstAddress() {
            return firstAddress;
        }

        
        public String getLastAddress() {
            return lastAddress;
        }
    }
}
