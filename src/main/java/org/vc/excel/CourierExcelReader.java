package org.vc.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.vc.address.AddressParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Читает назначения курьеров из Excel.
 * Поддерживает старый формат с листом «Данные» и новый формат «Путь курьера»,
 * где заголовки находятся не в первой строке, а адрес лежит в колонке «Адрес».
 */
public class CourierExcelReader {

    private static final String PREFERRED_SHEET_NAME = "Данные";
    private static final int HEADER_SCAN_LIMIT = 30;

    private static final List<String> COURIER_COLUMNS = List.of("курьер");
    private static final List<String> FULL_ADDRESS_COLUMNS = List.of("полный адрес", "адрес", "адреса", "исходный адрес");
    private static final List<String> SOURCE_ADDRESS_COLUMNS = List.of("исходный адрес");

    private static final String STREET_TYPE_COLUMN = "типулицы";
    private static final String STREET_COLUMN = "улица";
    private static final String HOUSE_COLUMN = "номердома";
    private static final String CORPUS_COLUMN = "корпус";

    private final AddressParser addressParser = new AddressParser();

    /**
     * Читает Excel-файл и группирует нормализованные адреса по имени курьера.
     */
    public Map<String, Set<String>> readCourierAddresses(Path excelPath) throws IOException {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream inputStream = Files.newInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = getSheet(workbook);
            System.out.println("Используется лист Excel: " + sheet.getSheetName());

            Header header = findHeader(sheet, formatter);

            for (int i = header.rowIndex() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String rawAddress = getAddress(row, formatter, header);
                String courier = getCellValue(row, header.courierColumnIndex(), formatter);

                if (rawAddress.isEmpty() || courier.isEmpty()) {
                    continue;
                }

                for (String address : addressParser.parseAddresses(rawAddress)) {
                    if (!address.isEmpty()) {
                        result.computeIfAbsent(courier, key -> new LinkedHashSet<>()).add(address);
                    }
                }
            }
        }

        return result;
    }

    private Sheet getSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(PREFERRED_SHEET_NAME);

        if (sheet == null && workbook.getNumberOfSheets() > 0) {
            sheet = workbook.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IllegalStateException("В Excel не найдено ни одного листа");
        }

        return sheet;
    }

    private Header findHeader(Sheet sheet, DataFormatter formatter) {
        int lastCandidate = Math.min(sheet.getLastRowNum(), HEADER_SCAN_LIMIT);

        for (int rowIndex = 0; rowIndex <= lastCandidate; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            int courierColumnIndex = findOptionalColumnIndex(row, COURIER_COLUMNS, formatter);
            if (courierColumnIndex < 0) {
                continue;
            }

            int fullAddressColumnIndex = findOptionalColumnIndex(row, FULL_ADDRESS_COLUMNS, formatter);
            int sourceAddressColumnIndex = findOptionalColumnIndex(row, SOURCE_ADDRESS_COLUMNS, formatter);
            int streetTypeColumnIndex = findOptionalColumnIndex(row, STREET_TYPE_COLUMN, formatter);
            int streetColumnIndex = findOptionalColumnIndex(row, STREET_COLUMN, formatter);
            int houseColumnIndex = findOptionalColumnIndex(row, HOUSE_COLUMN, formatter);
            int corpusColumnIndex = findOptionalColumnIndex(row, CORPUS_COLUMN, formatter);

            boolean hasFullAddress = fullAddressColumnIndex >= 0 || sourceAddressColumnIndex >= 0;
            boolean hasStructuredAddress = streetTypeColumnIndex >= 0
                && streetColumnIndex >= 0
                && houseColumnIndex >= 0;

            if (hasFullAddress || hasStructuredAddress) {
                return new Header(
                    rowIndex,
                    courierColumnIndex,
                    fullAddressColumnIndex,
                    sourceAddressColumnIndex,
                    streetTypeColumnIndex,
                    streetColumnIndex,
                    houseColumnIndex,
                    corpusColumnIndex
                );
            }
        }

        throw new IllegalStateException(
            "Не найдена строка заголовков Excel. Нужна колонка «Курьер» и адрес в колонке «Адрес»/«Полный адрес» "
                + "или в колонках «ТипУлицы», «Улица», «НомерДома»."
        );
    }

    private String getAddress(Row row, DataFormatter formatter, Header header) {
        if (header.fullAddressColumnIndex() >= 0) {
            String rawAddress = getCellValue(row, header.fullAddressColumnIndex(), formatter);
            if (!rawAddress.isEmpty()) {
                return rawAddress;
            }
        }

        String builtAddress = buildAddressFromColumns(row, formatter, header);

        if (!builtAddress.isEmpty()) {
            return builtAddress;
        }

        if (header.sourceAddressColumnIndex() >= 0) {
            return getCellValue(row, header.sourceAddressColumnIndex(), formatter);
        }

        return "";
    }

    private String buildAddressFromColumns(Row row, DataFormatter formatter, Header header) {
        if (header.streetTypeColumnIndex() < 0 || header.streetColumnIndex() < 0 || header.houseColumnIndex() < 0) {
            return "";
        }

        String streetType = getCellValue(row, header.streetTypeColumnIndex(), formatter);
        String street = getCellValue(row, header.streetColumnIndex(), formatter);
        String house = getCellValue(row, header.houseColumnIndex(), formatter);
        String corpus = header.corpusColumnIndex() >= 0 ? getCellValue(row, header.corpusColumnIndex(), formatter) : "";

        if (streetType.isEmpty() || street.isEmpty() || house.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder()
            .append(streetType.trim())
            .append(" ")
            .append(street.trim())
            .append(" д. ")
            .append(house.trim());

        if (!corpus.isEmpty()) {
            result.append(" корпус ").append(corpus.trim());
        }

        return result.toString();
    }

    private int findOptionalColumnIndex(Row headerRow, List<String> columnNames, DataFormatter formatter) {
        for (String columnName : columnNames) {
            int columnIndex = findOptionalColumnIndex(headerRow, columnName, formatter);

            if (columnIndex >= 0) {
                return columnIndex;
            }
        }

        return -1;
    }

    private int findOptionalColumnIndex(Row headerRow, String columnName, DataFormatter formatter) {
        String expected = normalizeHeader(columnName);

        for (Cell cell : headerRow) {
            String actual = normalizeHeader(formatter.formatCellValue(cell));

            if (expected.equals(actual)) {
                return cell.getColumnIndex();
            }
        }

        return -1;
    }

    private String normalizeHeader(String value) {
        return value == null
            ? ""
            : value.replace('\u00A0', ' ')
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private String getCellValue(Row row, int columnIndex, DataFormatter formatter) {
        if (columnIndex < 0) {
            return "";
        }

        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private record Header(
        int rowIndex,
        int courierColumnIndex,
        int fullAddressColumnIndex,
        int sourceAddressColumnIndex,
        int streetTypeColumnIndex,
        int streetColumnIndex,
        int houseColumnIndex,
        int corpusColumnIndex
    ) {
    }
}
