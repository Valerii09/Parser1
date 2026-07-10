package org.vc.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vc.report.ProcessingStats;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierPdfWriterTest {

    @TempDir
    private Path tempDir;

    private final CourierPdfWriter writer = new CourierPdfWriter();

    @Test
    void shouldCreateAddressRegistryWithAddressCountAndCourier() throws Exception {
        Map<String, List<CourierPage>> courierPages = new LinkedHashMap<>();
        courierPages.put(
            "Рыбинск курьер 1",
            List.of(
                page("г.Рыбинск, ул Шлюзовая, д.4, кв.1"),
                page("г.Рыбинск, ул Шлюзовая, д.4, кв.2")
            )
        );
        courierPages.put(
            "Рыбинск курьер 2",
            List.of(page("г.Рыбинск, ул Алябьева, д.30, кв.1"))
        );

        writer.writeCourierPdfs(tempDir, courierPages, new ProcessingStats());

        Path registryFile = tempDir.resolve("Реестр адресов.xlsx");

        assertTrue(Files.exists(registryFile));

        try (InputStream inputStream = Files.newInputStream(registryFile);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheet("Реестр");

            assertEquals("Адрес", getString(sheet, 0, 0));
            assertEquals("Количество ЛС", getString(sheet, 0, 1));
            assertEquals("Курьер", getString(sheet, 0, 2));

            assertEquals("г.рыбинск, ул шлюзовая, д.4", getString(sheet, 1, 0));
            assertEquals(2, getNumber(sheet, 1, 1));
            assertEquals("Рыбинск курьер 1", getString(sheet, 1, 2));

            assertEquals("г.рыбинск, ул алябьева, д.30", getString(sheet, 2, 0));
            assertEquals(1, getNumber(sheet, 2, 1));
            assertEquals("Рыбинск курьер 2", getString(sheet, 2, 2));
        }
    }

    @Test
    void shouldSplitCourierPdfBeforeMergeBecomesTooLarge() throws Exception {
        CourierPdfWriter limitedWriter = new CourierPdfWriter(2);
        Map<String, List<CourierPage>> courierPages = new LinkedHashMap<>();
        courierPages.put(
            "Иркутск курьер 1",
            List.of(
                page("г. Иркутск, ул. Ленина, д. 1"),
                page("г. Иркутск, ул. Ленина, д. 2"),
                page("г. Иркутск, ул. Ленина, д. 3")
            )
        );

        limitedWriter.writeCourierPdfs(tempDir, courierPages, new ProcessingStats());

        Path courierFolder = tempDir.resolve("Иркутск курьер 1");

        try (Stream<Path> files = Files.list(courierFolder)) {
            List<Path> pdfFiles = files
                .filter(path -> path.getFileName().toString().endsWith(".pdf"))
                .sorted()
                .toList();

            assertEquals(2, pdfFiles.size());
            assertEquals(2, getPdfPagesCount(pdfFiles.get(0)));
            assertEquals(1, getPdfPagesCount(pdfFiles.get(1)));
        }
    }

    private CourierPage page(String address) throws Exception {
        return new CourierPage(address, createPdfPage());
    }

    private Path createPdfPage() throws Exception {
        Path pageFile = Files.createTempFile(tempDir, "page-", ".pdf");

        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pageFile.toFile());
        }

        return pageFile;
    }

    private String getString(Sheet sheet, int rowIndex, int cellIndex) {
        Row row = sheet.getRow(rowIndex);

        return row.getCell(cellIndex).getStringCellValue();
    }

    private int getNumber(Sheet sheet, int rowIndex, int cellIndex) {
        Row row = sheet.getRow(rowIndex);

        return (int) row.getCell(cellIndex).getNumericCellValue();
    }

    private int getPdfPagesCount(Path pdfFile) throws Exception {
        try (PDDocument document = PDDocument.load(pdfFile.toFile())) {
            return document.getNumberOfPages();
        }
    }
}
