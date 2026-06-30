package org.vc.address;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсит адреса из исходной строки Excel в единый формат для файлов курьеров.
 *
 * @author Valerii Trufanov
 * @since 14.05.2026
 */
public class AddressParser {

    private static final String STREET_TYPE_PATTERN = AddressPatterns.STREET_TYPE_PATTERN;

    private static final String HOUSE_NUMBER_PATTERN =
        "\\d+\\s*[а-яa-z]?(?:/\\d+)?";

    private static final Pattern STREET_WITH_HOUSE_PATTERN = Pattern.compile(
        "(?iu)\\b(" + STREET_TYPE_PATTERN + ")\\s+(.+?)" +
            "(?:\\s|,|\\.)+\\s*(?:д\\.?|дом)?\\s*(?:№\\s*)?(" +
            HOUSE_NUMBER_PATTERN +
            ")(?:\\s|,|$)"
    );

    private static final Pattern HOUSE_PATTERN = Pattern.compile(
        "(?iu)\\b(?:д\\.?|дом)\\s*(?:№\\s*)?(" + HOUSE_NUMBER_PATTERN + ")\\b"
    );

    private static final Pattern STREET_NAME_WITH_HOUSE_PATTERN = Pattern.compile(
        "(?iu)^(.+?)(?:\\s|,)+\\s*(" + HOUSE_NUMBER_PATTERN + ")(?:\\s|,|$)"
    );

    private static final Pattern CORPUS_AFTER_HOUSE_PATTERN = Pattern.compile(
        "(?iu)(?:^|[,\\s]+)(?:(?:корпус|корп\\.?|к\\.?)\\s*([а-яa-z\\d]+)|([а-яa-z\\d]+)\\s*(?:корпус|корп\\.?))\\b"
    );

    private static final Pattern STREET_WITHOUT_HOUSE_PATTERN = Pattern.compile(
        "(?iu)\\b(" + STREET_TYPE_PATTERN + ")\\s+(.+)$"
    );

    /**
     * Возвращает список нормализованных адресов из одной исходной строки.
     */
    public List<String> parseAddresses(String rawAddress) {
        List<String> result = new ArrayList<>();

        for (String addressPart : splitAddressLine(rawAddress)) {
            String parsedAddress = parseAddress(addressPart);
            if (!parsedAddress.isEmpty()) {
                result.add(parsedAddress);
            }
        }

        return result;
    }

    /**
     * Делит строку на несколько адресов, если через "/" указан новый полноценный адрес.
     */
    private List<String> splitAddressLine(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return Collections.emptyList();
        }

        String value = rawAddress.trim();

        if (value.matches("(?iu).*?/\\s*(" + STREET_TYPE_PATTERN + ")\\b.*")) {
            return Arrays.stream(value.split("/"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
        }

        return List.of(value);
    }

    /**
     * Парсит один адрес и приводит его к формату "тип улицы + улица + д. + дом".
     */
    private String parseAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return "";
        }

        String value = AddressNormalizer.cleanup(rawAddress);

        Matcher streetWithHouseMatcher = STREET_WITH_HOUSE_PATTERN.matcher(value);
        if (streetWithHouseMatcher.find()) {
            String streetType = AddressNormalizer.normalizeStreetType(streetWithHouseMatcher.group(1));
            String streetName = AddressNormalizer.cleanupStreetName(streetWithHouseMatcher.group(2));
            String house = normalizeHouse(streetWithHouseMatcher.group(3));

            return streetType + " " + streetName + " д. " + house + extractCorpus(value, streetWithHouseMatcher.end(3));
        }

        Matcher houseMatcher = HOUSE_PATTERN.matcher(value);
        if (houseMatcher.find()) {
            String house = normalizeHouse(houseMatcher.group(1));
            String beforeHouse = value.substring(0, houseMatcher.start()).trim();

            String street = extractStreetWithoutHouse(beforeHouse);
            if (!street.isEmpty()) {
                return street + " д. " + house + extractCorpus(value, houseMatcher.end(1));
            }
        }

        Matcher streetNameWithHouseMatcher = STREET_NAME_WITH_HOUSE_PATTERN.matcher(value);
        if (streetNameWithHouseMatcher.find()) {
            String streetName = AddressNormalizer.cleanupStreetName(streetNameWithHouseMatcher.group(1));
            String house = normalizeHouse(streetNameWithHouseMatcher.group(2));

            if (!streetName.isEmpty()) {
                return streetName + " д. " + house + extractCorpus(value, streetNameWithHouseMatcher.end(2));
            }
        }

        System.out.println("Не удалось разобрать адрес: " + rawAddress);
        return "";
    }

    /**
     * Извлекает улицу из части адреса без номера дома.
     */
    private String extractStreetWithoutHouse(String value) {
        Matcher matcher = STREET_WITHOUT_HOUSE_PATTERN.matcher(value);

        if (!matcher.find()) {
            return "";
        }

        String streetType = AddressNormalizer.normalizeStreetType(matcher.group(1));
        String streetName = AddressNormalizer.cleanupStreetName(matcher.group(2));

        return streetType + " " + streetName;
    }

    /**
     * Убирает пробелы внутри номера дома и приводит букву дома к верхнему регистру.
     */
    private String normalizeHouse(String house) {
        return house
            .replaceAll("\\s+", "")
            .toUpperCase(Locale.ROOT);
    }

    private String extractCorpus(String value, int fromIndex) {
        if (value == null || fromIndex >= value.length()) {
            return "";
        }

        Matcher matcher = CORPUS_AFTER_HOUSE_PATTERN.matcher(value.substring(fromIndex));
        if (!matcher.find()) {
            return "";
        }

        String corpus = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return " корп. " + normalizeHouse(corpus);
    }
}
