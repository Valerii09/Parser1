package org.vc.address;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Извлекает адреса из текста страницы PDF-платёжки.
 *
 * @author Valerii Trufanov
 * @since 14.05.2026
 */
public class AddressExtractor {

    private static final Pattern PAYMENT_DOCUMENT_PATTERN = Pattern.compile(
        "(?=Плат[её]жный\\s+документ)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
        "(?=Лицевой\\s+сч[её]т:)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern PAYMENT_DOCUMENT_TITLE_PATTERN = Pattern.compile(
        "Плат[её]жный\\s+документ",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern PREMISE_ADDRESS_PATTERN = Pattern.compile(
        "Адрес\\s+помещения:\\s*(.+?)(?=\\s*(?:Потребитель:|Информация\\s+для|Организация-поставщик|Лицевой\\s+сч[её]т:|Плат[её]жный\\s+документ|$))",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
    );

    private static final Pattern DELIVERY_ADDRESS_PATTERN = Pattern.compile(
        "Адрес\\s+доставки:\\s*(.+?)(?=\\s*(?:Потребитель:|Информация\\s+для|Организация-поставщик|Лицевой\\s+сч[её]т:|Плат[её]жный\\s+документ|$))",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
    );

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "Адрес:\\s*(.+?)(?=\\s*(?:Плательщик:|Потребитель:|Общ\\.пл\\.|Информация\\s+для|Организация-поставщик|Лицевой\\s+сч[её]т:|Плат[её]жный\\s+документ|$))",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
    );

    private static final Pattern SUPPLIER_BLOCK_PATTERN = Pattern.compile(
        "\\R?\\s*Организация-поставщик.*",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
    );

    private static final Pattern YAROBLVODOKANAL_PATTERN = Pattern.compile(
        "Яроблводоканал",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern FACTORIAL_PATTERN = Pattern.compile(
        "\u0424\u0430\u043A\u0442\u043E\u0440\u0438\u0430\u043B",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern FACTORIAL_INLINE_STREET_ADDRESS_PATTERN = Pattern.compile(
        "(?iu)(?:\\b\u0433\\.?\\s*\u0418\u0440\u043A\u0443\u0442\u0441\u043A\\s*,\\s*)?([\u0410-\u042F\u0401A-Z0-9\\-\\s]+?)\\s+("
            + AddressPatterns.STREET_TYPE_PATTERN
            + ")\\s*(?:\\([^)]*\\)\\s*)?,\\s*(?:\u0434\\.?|\u0434\u043E\u043C)\\s*(?:\u2116\\s*)?(\\d+\\s*[\u0410-\u042F\u0401A-Z]?(?:/\\d+)?)"
            + "(?:\\s*,\\s*(?:\u043A\u043E\u0440\u043F\u0443\u0441|\u043A\u043E\u0440\u043F\\.?|\u043A\\.(?!\u0432))\\s*([\u0410-\u042F\u0401A-Z0-9]+))?"
            + "(?:\\s*,\\s*\u043A\u0432\\.?\\s*\\d+[\u0410-\u042F\u0401A-Z]?)?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern FACTORIAL_INLINE_LOCALITY_ADDRESS_PATTERN = Pattern.compile(
        "(?iu)(?:\\b\u0433\\.?\\s*\u0418\u0440\u043A\u0443\u0442\u0441\u043A\\s*,\\s*)?([\u0410-\u042F\u0401A-Z0-9\\-\\s]+?(?:\u0413\u041E\u0420\u041E\u0414\u041E\u041A|\u041F\u041E\u0421\u0415\u041B\u041E\u041A|\u041F\u041E\u0421\u0401\u041B\u041E\u041A|\u0421\u0415\u041B\u041E|\u0420\\.?\\s*\u041F\\.?)\\s*)"
            + "\\s*(?:\\([^)]*\\)\\s*)?(?:,\\s*)+(?:\u0434\\.?|\u0434\u043E\u043C)\\s*(?:\u2116\\s*)?(\\d+\\s*[\u0410-\u042F\u0401A-Z]?(?:/\\d+)?)"
            + "(?:\\s*,\\s*(?:\u043A\u043E\u0440\u043F\u0443\u0441|\u043A\u043E\u0440\u043F\\.?|\u043A\\.(?!\u0432))\\s*([\u0410-\u042F\u0401A-Z0-9]+))?"
            + "(?:\\s*,\\s*\u043A\u0432\\.?\\s*\\d+[\u0410-\u042F\u0401A-Z]?)?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern FACTORIAL_INLINE_BARE_STREET_ADDRESS_PATTERN = Pattern.compile(
        "(?iu)(?:\\b\u0433\\.?\\s*\u0418\u0440\u043A\u0443\u0442\u0441\u043A\\s*,\\s*)?([\u0410-\u042F\u0401A-Z][\u0410-\u042F\u0401A-Z0-9\\-\\s]+?)\\.?,\\s*(?:\u0434\\.?|\u0434\u043E\u043C)\\s*(?:\u2116\\s*)?(\\d+\\s*[\u0410-\u042F\u0401A-Z]?(?:/\\d+)?)"
            + "(?:\\s*,\\s*(?:\u043A\u043E\u0440\u043F\u0443\u0441|\u043A\u043E\u0440\u043F\\.?|\u043A\\.(?!\u0432))\\s*([\u0410-\u042F\u0401A-Z0-9]+))?"
            + "(?:\\s*,\\s*\u043A\u0432\\.?\\s*\\d+[\u0410-\u042F\u0401A-Z]?)?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final PaymentSupplier supplier;

    public AddressExtractor() {
        this(PaymentSupplier.AUTO);
    }

    public AddressExtractor(PaymentSupplier supplier) {
        this.supplier = supplier == null ? PaymentSupplier.AUTO : supplier;
    }

    /**
     * Возвращает первый адрес из текста страницы или пустую строку, если адрес не найден.
     */
    public String extractAddress(String pageText) {
        List<String> addresses = extractAddresses(pageText);

        if (addresses.isEmpty()) {
            return "";
        }

        return addresses.get(0);
    }

    /**
     * Возвращает адреса всех платёжек на странице.
     *
     * <p>Для каждой платёжки сначала ищется {@code Адрес помещения:}.
     * Если такого поля нет, используется первый запасной вариант {@code Адрес:}.</p>
     */
    public List<String> extractAddresses(String pageText) {
        if (isYaroblvodokanalPage(pageText)) {
            String address = extractFirstByPattern(ADDRESS_PATTERN, pageText);

            return address.isEmpty() ? List.of() : List.of(address);
        }

        List<String> paymentDocuments = splitPaymentDocuments(pageText);
        List<String> addresses = extractAddressesFromPaymentDocuments(paymentDocuments);

        if (addresses.isEmpty() && isFactorialPage(pageText)) {
            return extractFactorialInlineAddresses(pageText);
        }

        return addresses;
    }

    private boolean isYaroblvodokanalPage(String pageText) {
        return supplier == PaymentSupplier.YAROBLVODOKANAL
            || (supplier == PaymentSupplier.AUTO && YAROBLVODOKANAL_PATTERN.matcher(pageText).find());
    }

    private boolean isFactorialPage(String pageText) {
        return supplier == PaymentSupplier.FACTORIAL
            || (supplier == PaymentSupplier.AUTO && FACTORIAL_PATTERN.matcher(pageText).find());
    }

    private List<String> splitPaymentDocuments(String pageText) {
        Pattern splitPattern = PAYMENT_DOCUMENT_TITLE_PATTERN.matcher(pageText).find()
            ? PAYMENT_DOCUMENT_PATTERN
            : ACCOUNT_PATTERN;

        return splitPattern
            .splitAsStream(pageText)
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
    }

    private List<String> extractAddressesFromPaymentDocuments(List<String> paymentDocuments) {
        List<String> addresses = new ArrayList<>();

        for (String paymentDocument : paymentDocuments) {
            String address = extractAddressFromPaymentDocument(paymentDocument);

            if (!address.isEmpty()) {
                addresses.add(address);
            }
        }

        return addresses;
    }

    private String extractAddressFromPaymentDocument(String paymentDocument) {
        String premiseAddress = extractFirstByPattern(PREMISE_ADDRESS_PATTERN, paymentDocument);

        if (!premiseAddress.isEmpty()) {
            return premiseAddress;
        }

        String deliveryAddress = extractFirstByPattern(DELIVERY_ADDRESS_PATTERN, paymentDocument);

        if (!deliveryAddress.isEmpty()) {
            return deliveryAddress;
        }

        return extractFirstByPattern(ADDRESS_PATTERN, removeSupplierBlock(paymentDocument));
    }


    private List<String> extractFactorialInlineAddresses(String pageText) {
        List<String> addresses = new ArrayList<>();
        Matcher matcher = FACTORIAL_INLINE_STREET_ADDRESS_PATTERN.matcher(pageText);

        while (matcher.find()) {
            String streetName = AddressNormalizer.cleanupStreetName(matcher.group(1));
            String streetType = AddressNormalizer.normalizeStreetType(matcher.group(2));
            String house = matcher.group(3).replaceAll("\\s+", "").toUpperCase();
            String corpus = matcher.group(4);

            StringBuilder address = new StringBuilder("г.Иркутск, ")
                .append(streetType)
                .append(" ")
                .append(streetName)
                .append(", д. ")
                .append(house);

            if (corpus != null && !corpus.isBlank()) {
                address.append(", корп. ").append(corpus.trim());
            }

            addresses.add(address.toString());
        }

        Matcher localityMatcher = FACTORIAL_INLINE_LOCALITY_ADDRESS_PATTERN.matcher(pageText);
        while (localityMatcher.find()) {
            String locality = normalizeFactorialLocality(localityMatcher.group(1));
            String house = localityMatcher.group(2).replaceAll("\\s+", "").toUpperCase();
            String corpus = localityMatcher.group(3);

            StringBuilder address = new StringBuilder("г.Иркутск, ")
                .append(locality)
                .append(", д. ")
                .append(house);

            if (corpus != null && !corpus.isBlank()) {
                address.append(", корп. ").append(corpus.trim());
            }

            addresses.add(address.toString());
        }

        if (addresses.isEmpty()) {
            Matcher bareStreetMatcher = FACTORIAL_INLINE_BARE_STREET_ADDRESS_PATTERN.matcher(pageText);
            while (bareStreetMatcher.find()) {
                String streetName = AddressNormalizer.cleanupStreetName(bareStreetMatcher.group(1));
                String house = bareStreetMatcher.group(2).replaceAll("\\s+", "").toUpperCase();
                String corpus = bareStreetMatcher.group(3);

                StringBuilder address = new StringBuilder("г.Иркутск, ул ")
                    .append(streetName)
                    .append(", д. ")
                    .append(house);

                if (corpus != null && !corpus.isBlank()) {
                    address.append(", корп. ").append(corpus.trim());
                }

                addresses.add(address.toString());
            }
        }

        return addresses;
    }

    private String normalizeFactorialLocality(String value) {
        String locality = AddressNormalizer.cleanupStreetName(value);
        Matcher reverseRpMatcher = Pattern.compile("(?iu)^(.+?)\\s+\u0440\\.?\\s*\u043F\\.?$").matcher(locality);

        if (reverseRpMatcher.matches()) {
            return "\u0440\u043F. " + AddressNormalizer.cleanupStreetName(reverseRpMatcher.group(1));
        }

        return locality;
    }

    private String removeSupplierBlock(String paymentDocument) {
        return SUPPLIER_BLOCK_PATTERN.matcher(paymentDocument).replaceFirst("");
    }

    private String extractFirstByPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            return "";
        }

        return cleanupExtractedAddress(matcher.group(1));
    }

    private String cleanupExtractedAddress(String address) {
        return address
            .replaceAll("[\\r\\n\\t\\u00A0]", " ")
            .replaceAll("\\s+", " ")
            .replaceAll("\\s*,\\s*", ", ")
            .replaceAll("(?iu)\\s+Дл\\s*я\\s+оп\\s*ла\\s*ты.*$", "")
            .replaceAll("(?iu)\\s+Для\\s+оплаты.*$", "")
            .replaceAll("(?iu)\\s+Оплата\\s+по\\s+QR-коду.*$", "")
            .replaceAll("[)\\s]+$", "")
            .trim();
    }
}
