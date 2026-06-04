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
import java.util.Map;
import java.util.Set;

/**
 * Читает назначения курьеров из исходного Excel-файла.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierExcelReader {

    private static final String SHEET_NAME = "Данные";

    private static final String ADDRESS_COLUMN = "Полный адрес";
    private static final String SOURCE_ADDRESS_COLUMN = "Исходный адрес";

    private static final String STREET_TYPE_COLUMN = "ТипУлицы";
    private static final String STREET_COLUMN = "Улица";
    private static final String HOUSE_COLUMN = "НомерДома";
    private static final String CORPUS_COLUMN = "Корпус";

    private static final String COURIER_COLUMN = "Курьер";

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

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalStateException("Не найдена строка заголовков");
            }

            int courierColumnIndex = findColumnIndex(headerRow, COURIER_COLUMN, formatter);

            int addressColumnIndex = findOptionalColumnIndex(headerRow, ADDRESS_COLUMN, formatter);
            int sourceAddressColumnIndex = findOptionalColumnIndex(headerRow, SOURCE_ADDRESS_COLUMN, formatter);

            int streetTypeColumnIndex = findOptionalColumnIndex(headerRow, STREET_TYPE_COLUMN, formatter);
            int streetColumnIndex = findOptionalColumnIndex(headerRow, STREET_COLUMN, formatter);
            int houseColumnIndex = findOptionalColumnIndex(headerRow, HOUSE_COLUMN, formatter);
            int corpusColumnIndex = findOptionalColumnIndex(headerRow, CORPUS_COLUMN, formatter);

            boolean hasOldAddressFormat = addressColumnIndex >= 0;
            boolean hasNewAddressFormat = streetTypeColumnIndex >= 0
                && streetColumnIndex >= 0
                && houseColumnIndex >= 0;
            boolean hasSourceAddressFormat = sourceAddressColumnIndex >= 0;

            if (!hasOldAddressFormat && !hasNewAddressFormat && !hasSourceAddressFormat) {
                throw new IllegalStateException(
                    "Не найдены колонки адреса. Нужна колонка '" + ADDRESS_COLUMN
                        + "' или набор колонок: "
                        + STREET_TYPE_COLUMN + ", "
                        + STREET_COLUMN + ", "
                        + HOUSE_COLUMN
                );
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String rawAddress = getAddress(
                    row,
                    formatter,
                    addressColumnIndex,
                    streetTypeColumnIndex,
                    streetColumnIndex,
                    houseColumnIndex,
                    corpusColumnIndex,
                    sourceAddressColumnIndex
                );

                String courier = getCellValue(row, courierColumnIndex, formatter);

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
        Sheet sheet = workbook.getSheet(SHEET_NAME);

        if (sheet == null && workbook.getNumberOfSheets() > 0) {
            sheet = workbook.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IllegalStateException("В Excel не найдено ни одного листа");
        }

        return sheet;
    }

    private String getAddress(
        Row row,
        DataFormatter formatter,
        int addressColumnIndex,
        int streetTypeColumnIndex,
        int streetColumnIndex,
        int houseColumnIndex,
        int corpusColumnIndex,
        int sourceAddressColumnIndex
    ) {
        if (addressColumnIndex >= 0) {
            String rawAddress = getCellValue(row, addressColumnIndex, formatter);
            if (!rawAddress.isEmpty()) {
                return rawAddress;
            }
        }

        String builtAddress = buildAddressFromColumns(
            row,
            formatter,
            streetTypeColumnIndex,
            streetColumnIndex,
            houseColumnIndex,
            corpusColumnIndex
        );

        if (!builtAddress.isEmpty()) {
            return builtAddress;
        }

        if (sourceAddressColumnIndex >= 0) {
            return getCellValue(row, sourceAddressColumnIndex, formatter);
        }

        return "";
    }

    private String buildAddressFromColumns(
        Row row,
        DataFormatter formatter,
        int streetTypeColumnIndex,
        int streetColumnIndex,
        int houseColumnIndex,
        int corpusColumnIndex
    ) {
        if (streetTypeColumnIndex < 0 || streetColumnIndex < 0 || houseColumnIndex < 0) {
            return "";
        }

        String streetType = getCellValue(row, streetTypeColumnIndex, formatter);
        String street = getCellValue(row, streetColumnIndex, formatter);
        String house = getCellValue(row, houseColumnIndex, formatter);
        String corpus = corpusColumnIndex >= 0 ? getCellValue(row, corpusColumnIndex, formatter) : "";

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

    private int findColumnIndex(Row headerRow, String columnName, DataFormatter formatter) {
        int columnIndex = findOptionalColumnIndex(headerRow, columnName, formatter);

        if (columnIndex < 0) {
            throw new IllegalStateException("Не найдена колонка: " + columnName);
        }

        return columnIndex;
    }

    private int findOptionalColumnIndex(Row headerRow, String columnName, DataFormatter formatter) {
        for (Cell cell : headerRow) {
            String value = formatter.formatCellValue(cell).trim();

            if (columnName.equalsIgnoreCase(value)) {
                return cell.getColumnIndex();
            }
        }

        return -1;
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
}