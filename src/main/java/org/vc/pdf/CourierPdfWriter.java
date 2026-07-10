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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    // TEMP_FAST_MODE: временное ускорение под срочную большую обработку.
    // Откатить после обработки: вернуть последовательную запись и MemoryUsageSetting.setupTempFileOnly().
    // Запись намеренно ограничена двумя потоками: большее число одновременно создаёт
    // слишком много крупных файлов и резко увеличивает пиковый расход диска.
    private static final boolean TEMP_FAST_MODE = true;
    private static final int TEMP_FAST_WRITER_THREADS = Integer.getInteger(
        "paymentCourier.fast.writerThreads",
        2
    );

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
        List<CourierWriteJob> writeJobs = new ArrayList<>();

        for (Map.Entry<String, List<CourierPage>> entry : courierPages.entrySet()) {
            String courierName = entry.getKey();

            int inputPagesCount = countPhysicalPages(entry.getValue());
            List<CourierPage> pages = new ArrayList<>(entry.getValue());

            if (pages.isEmpty()) {
                System.out.println("Для курьера не найдено платёжек: " + courierName);
                continue;
            }

            pages.sort(pageComparator);
            addressRegistryRows.addAll(addressRegistryBuilder.build(courierName, pages));

            int outputPagesCount = countPhysicalPages(pages);

            Path courierFolder = couriersRoot.resolve(courierName);
            Files.createDirectories(courierFolder);

            writeJobs.add(new CourierWriteJob(courierFolder, courierName, pages, outputPagesCount));

            summaryRows.add(new CourierSummaryRow(
                courierName,
                inputPagesCount,
                outputPagesCount,
                pages.get(0).getAddress(),
                pages.get(pages.size() - 1).getAddress()
            ));
        }

        for (CourierWriteResult result : writeCourierPdfJobs(writeJobs)) {
            stats.incrementCreatedCourierPdfFiles(result.getCreatedFilesCount());
            stats.addWrittenPages(result.getOutputPagesCount());

            System.out.println(
                "Создано PDF для курьера " + result.getCourierName()
                    + ": файлов " + result.getCreatedFilesCount()
                    + ", на выходе сортировки: " + result.getOutputPagesCount()
            );
        }

        writeSummary(couriersRoot, summaryRows);
        writeAddressRegistry(couriersRoot, addressRegistryRows);
    }

    private List<CourierWriteResult> writeCourierPdfJobs(List<CourierWriteJob> writeJobs) throws IOException {
        if (!TEMP_FAST_MODE || writeJobs.size() <= 1) {
            List<CourierWriteResult> results = new ArrayList<>();

            for (CourierWriteJob writeJob : writeJobs) {
                results.add(writeCourierPdfJob(writeJob));
            }

            return results;
        }

        int threads = Math.min(TEMP_FAST_WRITER_THREADS, writeJobs.size());

        System.out.println("TEMP_FAST_MODE: параллельная запись PDF курьеров, потоков: " + threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<CourierWriteResult>> futures = new ArrayList<>();

        for (CourierWriteJob writeJob : writeJobs) {
            futures.add(executor.submit(() -> writeCourierPdfJob(writeJob)));
        }

        executor.shutdown();

        List<CourierWriteResult> results = new ArrayList<>();

        for (Future<CourierWriteResult> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Запись PDF курьеров прервана", exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();

                if (cause instanceof IOException ioException) {
                    throw ioException;
                }

                throw new IOException("Ошибка параллельной записи PDF курьеров", cause);
            }
        }

        return results;
    }

    private CourierWriteResult writeCourierPdfJob(CourierWriteJob writeJob) throws IOException {
        int createdFilesCount = writeCourierPdfParts(
            writeJob.getCourierFolder(),
            writeJob.getCourierName(),
            writeJob.getPages()
        );

        return new CourierWriteResult(
            writeJob.getCourierName(),
            createdFilesCount,
            writeJob.getOutputPagesCount()
        );
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
        deleteMergedTempFiles(pages);

        System.out.println(
            "Создан файл: " + resultPdf
                + ", страниц: " + pagesCount
        );
    }

    /**
     * Освобождает место на диске сразу после успешной сборки части курьерского PDF.
     * Общая очистка временного каталога остаётся страховкой на случай ошибки.
     */
    private void deleteMergedTempFiles(List<CourierPage> pages) {
        for (CourierPage page : pages) {
            for (Path pageFile : page.getPageFiles()) {
                try {
                    Files.deleteIfExists(pageFile);
                } catch (IOException ignored) {
                    // В конце обработки временный каталог будет очищен целиком.
                }
            }
        }
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

        merger.mergeDocuments(TEMP_FAST_MODE
            ? MemoryUsageSetting.setupMainMemoryOnly()
            : MemoryUsageSetting.setupTempFileOnly()
        );
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

    private static class CourierWriteJob {

        private final Path courierFolder;
        private final String courierName;
        private final List<CourierPage> pages;
        private final int outputPagesCount;

        private CourierWriteJob(Path courierFolder, String courierName, List<CourierPage> pages, int outputPagesCount) {
            this.courierFolder = courierFolder;
            this.courierName = courierName;
            this.pages = pages;
            this.outputPagesCount = outputPagesCount;
        }

        private Path getCourierFolder() {
            return courierFolder;
        }

        private String getCourierName() {
            return courierName;
        }

        private List<CourierPage> getPages() {
            return pages;
        }

        private int getOutputPagesCount() {
            return outputPagesCount;
        }
    }

    private static class CourierWriteResult {

        private final String courierName;
        private final int createdFilesCount;
        private final int outputPagesCount;

        private CourierWriteResult(String courierName, int createdFilesCount, int outputPagesCount) {
            this.courierName = courierName;
            this.createdFilesCount = createdFilesCount;
            this.outputPagesCount = outputPagesCount;
        }

        private String getCourierName() {
            return courierName;
        }

        private int getCreatedFilesCount() {
            return createdFilesCount;
        }

        private int getOutputPagesCount() {
            return outputPagesCount;
        }
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
