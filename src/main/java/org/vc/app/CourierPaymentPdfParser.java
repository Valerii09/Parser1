package org.vc.app;

import org.vc.service.CourierPaymentService;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Консольная точка входа для распределения PDF-платёжек по курьерам.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierPaymentPdfParser {

    private static final String COURIERS_FOLDER_NAME = "Курьеры";

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Передайте путь к папке с PDF-файлами первым аргументом");
        }

        Path pdfFolder = Paths.get(args[0]);
        Path couriersRoot = Paths.get(
            System.getProperty("user.home"),
            "Documents",
            COURIERS_FOLDER_NAME
        );

        new CourierPaymentService().process(pdfFolder, couriersRoot);
    }
}
