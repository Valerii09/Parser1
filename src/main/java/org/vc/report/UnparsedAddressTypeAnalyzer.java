package org.vc.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Анализирует неизвестные обозначения типов улиц в неразобранных адресах.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class UnparsedAddressTypeAnalyzer {

    private static final String INPUT_FILE_NAME = "Не удалось разобрать адреса.txt";
    private static final String OUTPUT_FILE_NAME = "Нераспознанные обозначения адресов.txt";

    private static final Pattern ADDRESS_FROM_LINE_PATTERN = Pattern.compile(
        "(?iu).*?\\|\\s*Адрес:\\s*(.+)$"
    );

    private static final Pattern FIRST_WORD_PATTERN = Pattern.compile(
        "(?iu)^\\s*([а-яa-zё.-]+)\\b.*"
    );

    private static final Set<String> KNOWN_STREET_TYPES = Set.of(
        "ул",
        "улица",
        "пр-кт",
        "проспект",
        "просп",
        "пр",
        "пер",
        "переулок"
    );

    public static void main(String[] args) throws IOException {
        Path inputFile = resolveInputFile(args);
        Path outputFile = inputFile.getParent().resolve(OUTPUT_FILE_NAME);

        Map<String, AddressTypeStats> statsByType = analyze(inputFile);

        writeResult(outputFile, statsByType);

        System.out.println("Готово");
        System.out.println("Входной файл: " + inputFile);
        System.out.println("Результат: " + outputFile);
        System.out.println("Найдено уникальных неизвестных обозначений: " + statsByType.size());
    }

    private static Path resolveInputFile(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return Paths.get(args[0]);
        }

        return Paths.get(
            System.getProperty("user.home"),
            "Documents",
            "Курьеры",
            INPUT_FILE_NAME
        );
    }

    private static Map<String, AddressTypeStats> analyze(Path inputFile) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IllegalStateException("Файл не найден: " + inputFile);
        }

        Map<String, AddressTypeStats> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        List<String> lines = Files.readAllLines(inputFile, StandardCharsets.UTF_8);

        for (String line : lines) {
            String rawAddress = extractAddress(line);

            if (rawAddress.isEmpty()) {
                continue;
            }

            for (String addressPart : splitAddress(rawAddress)) {
                String streetType = extractStreetType(addressPart);

                if (streetType.isEmpty()) {
                    continue;
                }

                String normalizedStreetType = normalizeStreetType(streetType);

                if (KNOWN_STREET_TYPES.contains(normalizedStreetType)) {
                    continue;
                }

                result.computeIfAbsent(
                    normalizedStreetType,
                    key -> new AddressTypeStats(normalizedStreetType)
                ).addExample(addressPart);
            }
        }

        return result;
    }

    private static String extractAddress(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }

        Matcher matcher = ADDRESS_FROM_LINE_PATTERN.matcher(line);

        if (matcher.matches()) {
            return matcher.group(1).trim();
        }

        return line.trim();
    }

    private static List<String> splitAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(rawAddress.split("/"))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
    }

    private static String extractStreetType(String address) {
        String preparedAddress = cleanupAddressPrefix(address);

        Matcher matcher = FIRST_WORD_PATTERN.matcher(preparedAddress);

        if (!matcher.matches()) {
            return "";
        }

        return matcher.group(1).trim();
    }

    private static String cleanupAddressPrefix(String address) {
        return address
            .replace('ё', 'е')
            .replace('Ё', 'Е')
            .replaceAll("(?iu)^\\s*адрес:\\s*", "")
            .replaceAll("(?iu)^\\s*г\\.?\\s*рыбинск\\s*,?\\s*", "")
            .replaceAll("(?iu)^\\s*город\\s+рыбинск\\s*,?\\s*", "")
            .replaceAll("(?iu)^\\s*рыбинск\\s*,?\\s*", "")
            .replaceAll("[\\r\\n\\t]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String normalizeStreetType(String value) {
        return value
            .toLowerCase(Locale.ROOT)
            .replace(".", "")
            .trim();
    }

    private static void writeResult(
        Path outputFile,
        Map<String, AddressTypeStats> statsByType
    ) throws IOException {
        List<String> lines = new ArrayList<>();

        lines.add("Обозначение;Количество;Примеры");

        for (AddressTypeStats stats : statsByType.values()) {
            lines.add(
                stats.streetType
                    + ";"
                    + stats.count
                    + ";"
                    + String.join(" | ", stats.examples)
            );
        }

        Files.write(
            outputFile,
            lines,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static class AddressTypeStats {

        private static final int MAX_EXAMPLES = 5;

        private final String streetType;
        private int count;
        private final List<String> examples = new ArrayList<>();

        private AddressTypeStats(String streetType) {
            this.streetType = streetType;
        }

        private void addExample(String address) {
            count++;

            if (examples.size() < MAX_EXAMPLES) {
                examples.add(address);
            }
        }
    }
}
