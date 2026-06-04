package org.vc.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Читает данные об адресах курьеров из файловой системы.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierAddressRepository {

    private static final String ADDRESSES_FILE_NAME = "Адреса.txt";

    /**
     * Читает файлы адресов курьеров из дочерних каталогов указанной папки.
     */
    public Map<String, Set<String>> readCourierAddresses(Path couriersRoot) throws IOException {
        Map<String, Set<String>> result = new LinkedHashMap<>();

        if (!Files.exists(couriersRoot)) {
            throw new IllegalStateException("Папка курьеров не найдена: " + couriersRoot);
        }

        try (Stream<Path> courierFolders = Files.list(couriersRoot)) {
            for (Path courierFolder : courierFolders.filter(Files::isDirectory).toList()) {
                Path addressesFile = courierFolder.resolve(ADDRESSES_FILE_NAME);

                if (!Files.exists(addressesFile)) {
                    continue;
                }

                Set<String> addresses = readAddresses(addressesFile);

                if (!addresses.isEmpty()) {
                    result.put(courierFolder.getFileName().toString(), addresses);
                }
            }
        }

        return result;
    }

    private Set<String> readAddresses(Path addressesFile) throws IOException {
        return Files.readAllLines(addressesFile, StandardCharsets.UTF_8)
            .stream().map(String::trim)
            .filter(address -> !address.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
