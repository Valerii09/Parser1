package org.vc.app;

import org.vc.service.XmlPaymentPdfGenerationService;

import java.nio.file.Path;

/**
 * Консольный запуск генерации PDF-платёжек из XML.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class XmlPaymentPdfGenerator {

    private static final int DEFAULT_MAX_PAGES_PER_FILE = 1000;

    /**
     * Точка входа для запуска сценария.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Использование: java -cp payment-courier-tool.jar org.vc.app.XmlPaymentPdfGenerator <xml-файл> <папка-результата> [страниц-в-файле]");
            return;
        }

        Path xmlFile = Path.of(args[0]);
        Path outputFolder = Path.of(args[1]);
        int maxPagesPerFile = args.length >= 3
            ? Integer.parseInt(args[2])
            : DEFAULT_MAX_PAGES_PER_FILE;

        new XmlPaymentPdfGenerationService().generate(xmlFile, outputFolder, maxPagesPerFile);
    }
}
