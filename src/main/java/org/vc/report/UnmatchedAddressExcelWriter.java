package org.vc.report;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Формирует отчёты по нераспределённым и неразобранным адресам.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class UnmatchedAddressExcelWriter {

    private static final String OLD_FORMAT_FILE_NAME = "Не распределено.xlsx";
    private static final String DETAIL_FORMAT_FILE_NAME = "Не распределено_подробно.xlsx";
    private static final String UNPARSED_ADDRESSES_FILE_NAME = "Не удалось разобрать адреса.txt";

    private static final String DEFAULT_REGISTRY_NUMBER = "1";
    private static final int DEFAULT_ACCOUNT_COUNT = 1;

    /**
     * Записывает только те отчёты, которые нужны для накопленных нераспределённых данных.
     */
    public void write(Path couriersRoot, UnmatchedAddressRegistry registry) throws IOException {
        if (registry.isEmpty()) {
            System.out.println("Нераспределенных адресов нет");
            return;
        }

        if (registry.hasMatchedUnassignedAddresses()) {
            writeOldFormat(couriersRoot, registry);
            writeDetailFormat(couriersRoot, registry);
        }

        if (registry.hasUnparsedAddresses()) {
            writeUnparsedAddresses(couriersRoot, registry);
        }
    }

    private void writeUnparsedAddresses(Path couriersRoot, UnmatchedAddressRegistry registry) throws IOException {
        Path resultFile = couriersRoot.resolve(UNPARSED_ADDRESSES_FILE_NAME);

        Files.write(
            resultFile,
            registry.getUnparsedAddresses(),
            java.nio.charset.StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );

        System.out.println("Создан файл с адресами, которые не удалось разобрать: " + resultFile);
    }

    private void writeOldFormat(Path couriersRoot, UnmatchedAddressRegistry registry) throws IOException {
        Path resultFile = couriersRoot.resolve(OLD_FORMAT_FILE_NAME);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Данные");
            createOldFormatHeader(sheet);

            int rowIndex = 1;

            for (Map.Entry<UnmatchedAddress, Integer> entry : registry.getFlatCountByAddress()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(this::compareAddresses))
                .toList()) {

                UnmatchedAddress address = entry.getKey();
                Integer flatCount = entry.getValue();

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("г");
                row.createCell(1).setCellValue(address.getCity());
                row.createCell(2).setCellValue(getStreetType(address.getStreet()));
                row.createCell(3).setCellValue(getStreetName(address.getStreet()));
                row.createCell(4).setCellValue(address.getHouseNumber());
                row.createCell(5).setCellValue(address.getCorpus());
                row.createCell(6).setCellValue(flatCount);
                row.createCell(7).setCellValue("");
            }

            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream outputStream = Files.newOutputStream(resultFile)) {
                workbook.write(outputStream);
            }
        }

        System.out.println("Создан файл с нераспределенными адресами в старом формате: " + resultFile);
    }

    private void writeDetailFormat(Path couriersRoot, UnmatchedAddressRegistry registry) throws IOException {
        Path resultFile = couriersRoot.resolve(DETAIL_FORMAT_FILE_NAME);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Данные");
            createDetailFormatHeader(sheet);

            List<UnmatchedAddress> addresses = registry.getAddresses()
                .stream()
                .sorted(this::compareAddresses)
                .toList();

            int rowIndex = 1;

            for (UnmatchedAddress address : addresses) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(DEFAULT_REGISTRY_NUMBER);
                row.createCell(1).setCellValue(address.getIndex());
                row.createCell(2).setCellValue(address.getFullAddress());
                row.createCell(3).setCellValue(DEFAULT_ACCOUNT_COUNT);
                row.createCell(4).setCellValue("");
            }

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream outputStream = Files.newOutputStream(resultFile)) {
                workbook.write(outputStream);
            }
        }

        System.out.println("Создан файл с нераспределенными адресами в подробном формате: " + resultFile);
    }

    private void createOldFormatHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ТипНП");
        header.createCell(1).setCellValue("НаселенныйПункт");
        header.createCell(2).setCellValue("ТипУлицы");
        header.createCell(3).setCellValue("Улица");
        header.createCell(4).setCellValue("НомерДома");
        header.createCell(5).setCellValue("Корпус");
        header.createCell(6).setCellValue("КоличествоКвартир");
        header.createCell(7).setCellValue("Курьер");
    }

    private void createDetailFormatHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Реестр №");
        header.createCell(1).setCellValue("Индекс");
        header.createCell(2).setCellValue("Полный адрес");
        header.createCell(3).setCellValue("Количество лицевых счетов");
        header.createCell(4).setCellValue("Курьер");
    }

    private int compareAddresses(UnmatchedAddress first, UnmatchedAddress second) {
        Comparator<String> stringComparator = String.CASE_INSENSITIVE_ORDER;

        int indexCompare = stringComparator.compare(first.getIndex(), second.getIndex());
        if (indexCompare != 0) {
            return indexCompare;
        }

        int cityCompare = stringComparator.compare(first.getCity(), second.getCity());
        if (cityCompare != 0) {
            return cityCompare;
        }

        int streetCompare = stringComparator.compare(first.getStreet(), second.getStreet());
        if (streetCompare != 0) {
            return streetCompare;
        }

        int houseNumberCompare = Integer.compare(
            parseNumber(first.getHouseNumber()),
            parseNumber(second.getHouseNumber())
        );
        if (houseNumberCompare != 0) {
            return houseNumberCompare;
        }

        int houseTextCompare = stringComparator.compare(first.getHouseNumber(), second.getHouseNumber());
        if (houseTextCompare != 0) {
            return houseTextCompare;
        }

        return stringComparator.compare(first.getCorpus(), second.getCorpus());
    }

    private int parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return Integer.MAX_VALUE;
        }

        String number = value.replaceAll("\\D+", "");
        if (number.isBlank()) {
            return Integer.MAX_VALUE;
        }

        return Integer.parseInt(number);
    }

    private String getStreetType(String street) {
        if (street == null || street.isBlank()) {
            return "";
        }

        String[] parts = street.trim().split("\\s+", 2);

        if (parts.length == 0) {
            return "";
        }

        return parts[0].trim();
    }

    private String getStreetName(String street) {
        if (street == null || street.isBlank()) {
            return "";
        }

        String[] parts = street.trim().split("\\s+", 2);

        if (parts.length < 2) {
            return "";
        }

        return parts[1].trim();
    }
}
