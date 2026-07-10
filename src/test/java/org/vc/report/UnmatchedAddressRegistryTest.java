package org.vc.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnmatchedAddressRegistryTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldStorePdfPageTextWhenAddressWasNotFound() throws Exception {
        UnmatchedAddressRegistry registry = new UnmatchedAddressRegistry();

        registry.addMissingAddressDocument(
            Path.of("C:\\pdf\\Батарейная.pdf"),
            12,
            "Факториал\nЛицевой счет: 123\nбез явного адреса"
        );

        assertTrue(registry.hasMissingAddressDocuments());
        assertEquals(1, registry.getMissingAddressDocuments().size());
        assertTrue(registry.getMissingAddressDocuments().get(0).contains("Батарейная.pdf"));
        assertTrue(registry.getMissingAddressDocuments().get(0).contains("Страница: 12"));
        assertTrue(registry.getMissingAddressDocuments().get(0).contains("Лицевой счет: 123"));
    }

    @Test
    void shouldWriteMissingAddressDocumentsReport() throws Exception {
        UnmatchedAddressRegistry registry = new UnmatchedAddressRegistry();
        registry.addMissingAddressDocument(Path.of("source.pdf"), 3, "текст страницы");

        new UnmatchedAddressExcelWriter().write(tempDir, registry);

        Path report = tempDir.resolve("Не найден адрес в PDF.txt");
        assertTrue(Files.exists(report));
        String reportText = Files.readString(report);
        assertTrue(reportText.contains("source.pdf"));
        assertTrue(reportText.contains("Страница: 3"));
        assertTrue(reportText.contains("текст страницы"));
    }
}
