package org.vc.pdf;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Готовит адрес для печати в верхней части платёжного документа.
 * Главное правило: если в XML есть адрес помещения, показываем именно его;
 * иначе оставляем адрес доставки или добавляем этот префикс к обычному адресу.
 */
final class PaymentAddressPrintFormatter {

    private static final Pattern PREMISE_ADDRESS_PATTERN = Pattern.compile(
        "(?iu)\\(\\s*адрес\\s+помещения:\\s*(.+?)\\s*\\)"
    );

    private PaymentAddressPrintFormatter() {
    }

    static String format(String sourceAddress) {
        String preparedAddress = cleanupAddressForPrint(sourceAddress);

        String premiseAddress = extractPremiseAddress(preparedAddress);
        if (!premiseAddress.isBlank()) {
            return "Адрес помещения: " + cleanupAddressForPrint(premiseAddress);
        }

        String lowerAddress = preparedAddress.toLowerCase(Locale.ROOT);

        if (lowerAddress.startsWith("адрес помещения:")
            || lowerAddress.startsWith("адрес доставки:")) {
            return preparedAddress;
        }

        return "Адрес доставки: " + preparedAddress;
    }

    private static String extractPremiseAddress(String address) {
        Matcher matcher = PREMISE_ADDRESS_PATTERN.matcher(address);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1);
    }

    private static String cleanupAddressForPrint(String address) {
        return clean(address)
            .replaceAll("\\s*,\\s*\\.\\s*,\\s*", ", ")
            .replaceAll("\\s*,\\s*\\.\\s+", ", ")
            .replaceAll("\\s+\\.\\s*,\\s*", ", ")
            .replaceAll(",\\s*,+", ", ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String clean(String text) {
        return text == null
            ? ""
            : text.replace('\u00A0', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
