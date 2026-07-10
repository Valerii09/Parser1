package org.vc.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vc.address.AddressNormalizer;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierExcelReaderTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldReadCourierPathExcelWithHeaderNotInFirstRow() throws Exception {
        Path excel = tempDir.resolve("courier-path.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Путь курьера");
            sheet.createRow(0).createCell(0).setCellValue("Адреса, закрепленные за курьерами");

            Row header = sheet.createRow(2);
            header.createCell(1).setCellValue("Курьер");
            header.createCell(2).setCellValue("Организация");
            header.createCell(3).setCellValue("Адрес ");

            Row row = sheet.createRow(3);
            row.createCell(1).setCellValue("ЗУ 10");
            row.createCell(2).setCellValue("Все внутренние");
            row.createCell(3).setCellValue("664005, Россия, Иркутская обл., г. Иркутск, пер. Спортивный, д. 5а");

            try (OutputStream outputStream = Files.newOutputStream(excel)) {
                workbook.write(outputStream);
            }
        }

        Map<String, Set<String>> result = new CourierExcelReader().readCourierAddresses(excel);

        assertTrue(result.containsKey("ЗУ 10"));
        assertEquals(1, result.get("ЗУ 10").size());
        assertEquals(
            "перспортивный5а",
            AddressNormalizer.normalizeHouseAddressForCompare(result.get("ЗУ 10").iterator().next())
        );
    }
}
