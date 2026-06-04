package org.vc.address;

import java.util.Locale;

/**
 * Фасад для нормализации адресов.
 *
 * @author Valerii Trufanov
 * @since 14.05.2026
 */
public final class AddressNormalizer {

    static final String STREET_TYPE_PATTERN = AddressPatterns.STREET_TYPE_PATTERN;

    private AddressNormalizer() {
    }

    /**
     * Выполняет базовую очистку адресной строки.
     */
    public static String cleanup(String value) {
        return AddressCleaner.cleanup(value);
    }

    /**
     * Очищает название улицы от лишних частей адреса.
     */
    public static String cleanupStreetName(String value) {
        return AddressCleaner.cleanupStreetName(value);
    }

    /**
     * Нормализует тип улицы: улица -> ул, проспект -> пр.
     */
    public static String normalizeStreetType(String value) {
        return StreetTypeNormalizer.normalizeStreetType(value);
    }

    /**
     * Нормализует название улицы.
     */
    public static String normalizeStreetName(String value) {
        return cleanupStreetName(value)
            .toLowerCase(Locale.ROOT);
    }

    /**
     * Возвращает ключ дома без квартиры для поиска курьера.
     */
    public static String normalizeHouseAddressForCompare(String value) {
        return AddressKeyNormalizer.normalizeHouseAddressForCompare(value);
    }

    /**
     * Убирает буквенный хвост дома для fallback-поиска.
     */
    public static String removeExtraBuildingSuffix(String normalizedAddress) {
        return AddressKeyNormalizer.removeExtraBuildingSuffix(normalizedAddress);
    }
}