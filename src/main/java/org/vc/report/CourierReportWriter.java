package org.vc.report;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Записывает сводку сортировки и реестр адресов, не смешивая Excel-логику
 * со сборкой итоговых PDF.
 */
public class CourierReportWriter {

    private static final String SUMMARY_FILE_NAME = "Итог сортировки.xlsx";
    private static final String ADDRESS_REGISTRY_FILE_NAME = "Реестр адресов.xlsx";

    /**
     * Формирует оба обязательных Excel-отчёта после завершения сортировки.
     */
    public void write(
        Path couriersRoot,
        List<CourierSummary> summaries,
        List<CourierAddressRegistryRow> registryRows
    ) throws IOException {
        writeSummary(couriersRoot, summaries);
        writeAddressRegistry(couriersRoot, registryRows);
    }

    private void writeSummary(Path couriersRoot, List<CourierSummary> summaries) throws IOException {
        Path resultFile = couriersRoot.resolve(SUMMARY_FILE_NAME);
        summaries.sort(Comparator
            .comparingInt((CourierSummary summary) -> extractCourierNumber(summary.courierName()))
            .thenComparing(CourierSummary::courierName, String.CASE_INSENSITIVE_ORDER));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Итог");
            createSummaryHeader(sheet);

            int rowIndex = 1;
            int totalInputCount = 0;
            int totalOutputCount = 0;

            for (CourierSummary summary : summaries) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(summary.courierName());
                row.createCell(1).setCellValue(summary.inputPagesCount());
                row.createCell(2).setCellValue(summary.outputPagesCount());
                row.createCell(3).setCellValue(summary.firstAddress());
                row.createCell(4).setCellValue(summary.lastAddress());

                totalInputCount += summary.inputPagesCount();
                totalOutputCount += summary.outputPagesCount();
            }

            Row totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(0).setCellValue("Итого");
            totalRow.createCell(1).setCellValue(totalInputCount);
            totalRow.createCell(2).setCellValue(totalOutputCount);

            autoSizeColumns(sheet, 5);
            save(workbook, resultFile);
        }

        System.out.println("Создан итоговый файл сортировки: " + resultFile);
    }

    private void writeAddressRegistry(Path couriersRoot, List<CourierAddressRegistryRow> rows) throws IOException {
        Path resultFile = couriersRoot.resolve(ADDRESS_REGISTRY_FILE_NAME);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Реестр");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Адрес");
            header.createCell(1).setCellValue("Количество ЛС");
            header.createCell(2).setCellValue("Курьер");

            int rowIndex = 1;
            for (CourierAddressRegistryRow registryRow : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(registryRow.getAddress());
                row.createCell(1).setCellValue(registryRow.getPaymentDocumentsCount());
                row.createCell(2).setCellValue(registryRow.getCourierName());
            }

            autoSizeColumns(sheet, 3);
            save(workbook, resultFile);
        }

        System.out.println("Создан реестр адресов: " + resultFile);
    }

    private void createSummaryHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Курьер");
        header.createCell(1).setCellValue("На входе сортировки");
        header.createCell(2).setCellValue("На выходе сортировки");
        header.createCell(3).setCellValue("Первый адрес");
        header.createCell(4).setCellValue("Последний адрес");
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int column = 0; column < count; column++) {
            sheet.autoSizeColumn(column);
        }
    }

    private void save(Workbook workbook, Path resultFile) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(resultFile)) {
            workbook.write(outputStream);
        }
    }

    private int extractCourierNumber(String courierName) {
        String number = courierName.replaceAll("\\D+", "");

        return number.isBlank() ? Integer.MAX_VALUE : Integer.parseInt(number);
    }
}
