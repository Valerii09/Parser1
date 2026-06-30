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

        return extractAddressesFromPaymentDocuments(paymentDocuments);
    }

    private boolean isYaroblvodokanalPage(String pageText) {
        return supplier == PaymentSupplier.YAROBLVODOKANAL
            || (supplier == PaymentSupplier.AUTO && YAROBLVODOKANAL_PATTERN.matcher(pageText).find());
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
