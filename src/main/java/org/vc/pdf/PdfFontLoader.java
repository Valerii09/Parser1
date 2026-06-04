package org.vc.pdf;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Находит системные шрифты Windows для PDFBox.
 * Генератор использует Arial, потому что он стабильно печатает русские символы
 * и визуально близок к образцам платёжек.
 */
final class PdfFontLoader {

    private PdfFontLoader() {
    }

    static Path resolveFontPath(String fileName) {
        Path windowsFont = Path.of(System.getenv("WINDIR"), "Fonts", fileName);

        if (Files.exists(windowsFont)) {
            return windowsFont;
        }

        return Path.of("C:", "Windows", "Fonts", fileName);
    }
}
