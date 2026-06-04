package org.vc.repository;

import org.vc.address.AddressComparator;
import org.vc.util.FileNameUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Записывает отсортированные списки адресов курьеров в файловую систему.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierAddressWriter {

    private static final String OUTPUT_FILE_NAME = "Адреса.txt";

    private final AddressComparator addressComparator = new AddressComparator();

    /**
     * Создаёт папки курьеров и записывает в них отсортированные списки адресов.
     */
    public void write(Path outputRoot, Map<String, Set<String>> courierAddresses) throws IOException {
        Files.createDirectories(outputRoot);

        for (Map.Entry<String, Set<String>> entry : courierAddresses.entrySet()) {
            String courierName = entry.getKey();
            Set<String> addresses = entry.getValue();

            Path courierFolder = outputRoot.resolve(FileNameUtils.safeFileName(courierName));
            Files.createDirectories(courierFolder);

            Path addressesFile = courierFolder.resolve(OUTPUT_FILE_NAME);

            List<String> sortedAddresses = new ArrayList<>(addresses);
            sortedAddresses.sort(addressComparator);

            Files.write(
                addressesFile,
                sortedAddresses,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );

            System.out.println("Создан файл адресов для " + courierName + ": " + addressesFile);
        }
    }
}
