package org.vc.app;

import org.vc.excel.CourierExcelReader;
import org.vc.repository.CourierAddressWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Консольная точка входа для формирования файлов адресов курьеров из Excel-файла.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierAddressParser {

    private static final String OUTPUT_ROOT_FOLDER = "Курьеры";

    /**
     * Точка входа для запуска сценария.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Передайте путь к Excel-файлу первым аргументом");
        }

        Path excelPath = Paths.get(args[0]);
        Path outputRoot = Paths.get(
            System.getProperty("user.home"),
            "Documents",
            OUTPUT_ROOT_FOLDER
        );

        CourierExcelReader reader = new CourierExcelReader();
        CourierAddressWriter writer = new CourierAddressWriter();

        Map<String, Set<String>> courierAddresses = reader.readCourierAddresses(excelPath);
        writer.write(outputRoot, courierAddresses);

        System.out.println("Готово. Папка создана: " + outputRoot.toAbsolutePath());
    }
}
