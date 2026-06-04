package org.vc.address;

import java.util.Locale;

/**
 * Приводит варианты написания типов улиц к единым сокращениям.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public final class StreetTypeNormalizer {

    private StreetTypeNormalizer() {
    }

    /**
     * Приводит разные написания типов улиц к каноническим значениям.
     */
    public static String normalizeStreetType(String value) {
        if (value == null) {
            return "";
        }

        String normalized = AddressCleaner.cleanup(value)
            .toLowerCase(Locale.ROOT)
            .replace(".", "")
            .trim();

        return switch (normalized) {
            case "улица", "ул" -> "ул";
            case "пр-кт", "проспект", "просп", "пр" -> "пр";
            case "переулок", "пер" -> "пер";
            case "ш", "шоссе" -> "ш";
            case "проезд" -> "проезд";
            case "пл", "площадь" -> "пл";
            case "б-р", "бульвар" -> "б-р";
            case "наб", "набережная" -> "наб";
            case "мкр", "микрорайон" -> "мкр";
            case "пос", "поселок", "посёлок" -> "пос";
            case "тракт" -> "тракт";
            case "тер", "территория" -> "тер";
            case "снт" -> "снт";
            default -> normalized;
        };
    }
}
