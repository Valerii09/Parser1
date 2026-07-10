package org.vc.address;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает адреса из платёжек на части, необходимые для сортировки и отчётов.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class PaymentAddressParts {

    private static final String STREET_TYPE_PATTERN = AddressPatterns.STREET_TYPE_PATTERN;

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "(?iu).*?\\b(" + STREET_TYPE_PATTERN + ")\\s*(.+?)" +
            "\\s*,?\\s*(?:д\\.?|дом)\\s*(?:№\\s*)?(\\d+)\\s*([а-яa-z]?)(/\\d+)?" +
            "(?:\\s*(?:,|/)\\s*(?:корпус|корп\\.?|к\\.)\\s*([\\dа-яa-z]+))?" +
            "(?:\\s*(?:,|/)\\s*(?:лит\\.?|литера)\\s*([а-яa-z]))?" +
            ".*",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern CITY_PREFIX_PATTERN = Pattern.compile(
        "(?iu)\\b(?:г\\.?|город)\\s*([а-яёa-z-]+)"
    );

    private static final Pattern CITY_SUFFIX_PATTERN = Pattern.compile(
        "(?iu)\\b([а-яёa-z-]+)\\s+г\\b"
    );

    private static final Pattern FLAT_PATTERN = Pattern.compile(
        "(?iu)\\bкв\\.?\\s*(\\d+)\\b"
    );

    private static final Pattern TRAILING_FLAT_PATTERN = Pattern.compile(
        "(?iu)\\b(?:д\\.?|дом)\\s*(?:№\\s*)?\\d+\\s*[а-яa-z]?(?:/\\d+)?\\s*,\\s*(\\d+)\\b"
    );

    private final String city;
    private final String street;
    private final int houseNumber;
    private final String houseLetter;
    private final String corpus;
    private final int flatNumber;
    private final boolean empty;

    private PaymentAddressParts(
        String city,
        String street,
        int houseNumber,
        String houseLetter,
        String corpus,
        int flatNumber,
        boolean empty
    ) {
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.houseLetter = houseLetter;
        this.corpus = corpus;
        this.flatNumber = flatNumber;
        this.empty = empty;
    }

    /**
     * Разбирает адрес из платёжки или возвращает пустой объект, если формат не распознан.
     */
    public static PaymentAddressParts parse(String address) {
        if (address == null || address.isBlank()) {
            return empty();
        }

        String preparedAddress = normalizeStreetTypeSuffix(AddressNormalizer.cleanup(chooseAddressPart(address)));
        Matcher matcher = ADDRESS_PATTERN.matcher(preparedAddress);

        if (!matcher.matches()) {
            return empty();
        }

        String city = extractCity(preparedAddress);
        String streetType = AddressNormalizer.normalizeStreetType(matcher.group(1));
        String streetName = AddressNormalizer.normalizeStreetName(matcher.group(2));
        String street = streetType + " " + streetName;

        int houseNumber = Integer.parseInt(matcher.group(3));

        String houseLetter = matcher.group(4) == null ? "" : matcher.group(4).trim().toLowerCase(Locale.ROOT);
        String fractionPart = matcher.group(5) == null ? "" : matcher.group(5).trim().toLowerCase(Locale.ROOT);
        String corpus = matcher.group(6) == null ? "" : matcher.group(6).trim().toLowerCase(Locale.ROOT);
        String litera = matcher.group(7) == null ? "" : matcher.group(7).trim().toLowerCase(Locale.ROOT);

        String fullHouseSuffix = houseLetter + fractionPart + litera;

        int flatNumber = extractFlatNumber(preparedAddress);

        return new PaymentAddressParts(city, street, houseNumber, fullHouseSuffix, corpus, flatNumber, false);
    }

    private static String extractCity(String address) {
        Matcher prefixMatcher = CITY_PREFIX_PATTERN.matcher(address);

        if (prefixMatcher.find()) {
            return prefixMatcher.group(1).trim().toLowerCase(Locale.ROOT);
        }

        Matcher suffixMatcher = CITY_SUFFIX_PATTERN.matcher(address);

        if (suffixMatcher.find()) {
            return suffixMatcher.group(1).trim().toLowerCase(Locale.ROOT);
        }

        return "";
    }

    private static int extractFlatNumber(String address) {
        Matcher matcher = FLAT_PATTERN.matcher(address);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        matcher = TRAILING_FLAT_PATTERN.matcher(address);

        if (!matcher.find()) {
            return Integer.MAX_VALUE;
        }

        return Integer.parseInt(matcher.group(1));
    }

    private static String chooseAddressPart(String address) {
        if (!address.contains("/")) {
            return address;
        }

        /*
         * Делим адрес по "/" только если после "/" начинается новый полноценный адрес:
         * ул Чкалова, д.40/ул Бородулина, д.9, кв.1
         *
         * Не делим:
         * ул Захарова, д.33/2, кв.4
         * ул X, д.10/корпусБ, кв.12
         * ул X, д.10/лит.А, кв.1
         */
        if (!address.matches("(?iu).*?/\\s*(" + STREET_TYPE_PATTERN + ")\\b.*")) {
            return address;
        }

        String[] parts = address.split("/");

        for (String part : parts) {
            if (part.matches("(?iu).*\\bкв\\.?\\s*\\d+.*")) {
                return part.trim();
            }
        }

        return parts[parts.length - 1].trim();
    }

    private static String normalizeStreetTypeSuffix(String address) {
        Matcher localityMatcher = Pattern.compile(
            "(?iu)^\\s*((?:г\\.?\\s*[а-яёa-z-]+\\s*,\\s*)?)([^,]+?)\\s+("
                + "мкр\\.?|мрн\\.?|микрорайон|кв-л\\.?|квартал"
                + ")\\.?\\s+[а-яёa-z-]+\\s*,\\s*((?:д\\.?|дом)\\s*.*)$"
        ).matcher(address);

        if (localityMatcher.matches()) {
            String type = AddressNormalizer.normalizeStreetType(localityMatcher.group(3));
            return localityMatcher.group(1)
                + type + " " + localityMatcher.group(2)
                + ", " + localityMatcher.group(4);
        }

        Matcher matcher = Pattern.compile(
            "(?iu)^\\s*((?:г\\.?\\s*[а-яёa-z-]+\\s*,\\s*)?)([^,]+?)\\s+(" + STREET_TYPE_PATTERN + ")\\s*,\\s*((?:д\\.?|дом)\\s*.*)$"
        ).matcher(address);

        if (!matcher.matches() || matcher.group(2).matches("(?iu).*\\b(" + STREET_TYPE_PATTERN + ")\\b.*")) {
            return address;
        }

        return matcher.group(1) + matcher.group(3) + " " + matcher.group(2) + ", " + matcher.group(4);
    }

    private static PaymentAddressParts empty() {
        return new PaymentAddressParts("",
            "",
            Integer.MAX_VALUE,
            "",
            "",
            Integer.MAX_VALUE,
            true
        );
    }

    
    public String getCity() {
        return city;
    }

    
    public String getStreet() {
        return street;
    }

    
    public int getHouseNumber() {
        return houseNumber;
    }

    
    public String getHouseLetter() {
        return houseLetter;
    }

    
    public String getCorpus() {
        return corpus;
    }

    
    public int getFlatNumber() {
        return flatNumber;
    }

    
    public boolean isEmpty() {
        return empty;
    }
}
