package org.vc.address;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Нормализует адреса в ключи для сравнения и поиска курьера.
 *
 * @author Valerii Trufanov
 * @since 14.05.2026
 */
public final class AddressKeyNormalizer {

    private static final Map<String, String> HOUSE_ADDRESS_CACHE = new ConcurrentHashMap<>();

    private AddressKeyNormalizer() {
    }

    /**
     * Возвращает ключ адреса дома без квартиры.
     */
    public static String normalizeHouseAddressForCompare(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return HOUSE_ADDRESS_CACHE.computeIfAbsent(value, AddressKeyNormalizer::normalizeHouseAddressInternal);
    }

    /**
     * Нормализует адресную строку для сравнения: типы улиц, сокращения и пробелы.
     */
    public static String normalizeAddressForCompare(String value) {
        if (value == null) {
            return "";
        }

        return AddressCleaner.normalizeLookAlikeLetters(value)
            .toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .replaceAll("(?iu)\\bмаксима\\s+горького\\b", "горького")
            .replaceAll("(?iu)\\bпр-кт\\b", "пр")
            .replaceAll("(?iu)\\bпроспект\\b", "пр")
            .replaceAll("(?iu)\\bпросп\\.?\\b", "пр")
            .replaceAll("(?iu)\\bпр\\.?\\b", "пр")

            .replaceAll("(?iu)\\bулица\\b", "ул")
            .replaceAll("(?iu)\\bул\\.?\\b", "ул")

            .replaceAll("(?iu)\\bпереулок\\b", "пер")
            .replaceAll("(?iu)\\bпер\\.?\\b", "пер")

            .replaceAll("(?iu)\\bшоссе\\b", "ш")
            .replaceAll("(?iu)\\bш\\.?\\b", "ш")

            .replaceAll("(?iu)\\bбульвар\\b", "б-р")
            .replaceAll("(?iu)\\bб-р\\b", "б-р")

            .replaceAll("(?iu)\\bнабережная\\b", "наб")
            .replaceAll("(?iu)\\bнаб\\.?\\b", "наб")

            .replaceAll("(?iu)\\bпоселок\\b", "пос")
            .replaceAll("(?iu)\\bпосёлок\\b", "пос")
            .replaceAll("(?iu)\\bпос\\.?\\b", "пос")

            .replaceAll("(?iu)\\bплощадь\\b", "пл")
            .replaceAll("(?iu)\\bпл\\.?\\b", "пл")
            .replaceAll("(?iu)\\bмикрорайон\\b", "мкр")
            .replaceAll("(?iu)\\bмкр\\.?\\b", "мкр")
            .replaceAll("(?iu)\\bтракт\\b", "тракт")
            .replaceAll("(?iu)\\bпроезд\\b", "проезд")

            .replaceAll("(?iu)\\bдом\\b", "д")
            .replaceAll("(?iu)\\bд\\.?\\b", "д")
            .replace("№", "")
            .replaceAll("(?iu)\\bквартира\\b", "кв")
            .replaceAll("(?iu)\\bкв\\.?\\b", "кв")
            .replaceAll("(?iu)\\bкорпус\\b", "к")
            .replaceAll("(?iu)\\bкорп\\.?\\b", "к")
            .replaceAll("(?iu)\\bк\\.?\\b", "к")
            .replaceAll("(?iu)\\bстроение\\b", "стр")
            .replaceAll("(?iu)\\bстр\\.?\\b", "стр")
            .replaceAll("(?iu)\\bлитера\\b", "лит")
            .replaceAll("(?iu)\\bлит\\.?\\b", "лит")

            .replaceAll("\\s+", "")
            .replaceAll("[.,]", "")
            .trim();
    }

    /**
     * Убирает одиночный буквенный хвост дома для fallback-поиска.
     */
    public static String removeExtraBuildingSuffix(String normalizedAddress) {
        if (normalizedAddress == null || normalizedAddress.isBlank()) {
            return "";
        }

        return normalizedAddress
            .replaceAll("(?iu)(\\d+(?:/\\d+)?)(?:к|стр)[а-я\\d]+$", "$1")
            .replaceAll("(?iu)(\\d+(?:/\\d+)?[а-я]?)(?:к|стр)[а-я\\d]+$", "$1")
            .replaceAll("(?iu)(\\d+(?:/\\d+)?)[а-я]$", "$1");
    }

    /**
     * Строит ключ адреса дома: без города, квартиры, корпуса и лишних хвостов.
     */
    private static String normalizeHouseAddressInternal(String value) {
        String address = AddressCleaner.cleanup(chooseAddressPart(value));

        address = removeCityPrefix(address);
        address = removeAdministrativePrefix(address);
        address = removeFlat(address);
        address = removeTrailingFlat(address);

        address = address
            .replaceAll("(?iu)(\\d+)\\s*/\\s*(?:лит\\.?|литера)\\s*\\.?\\s*([а-яa-z]).*$", "$1$2")
            .replaceAll("(?iu)(\\d+\\s*[а-яa-z]?)\\s*,\\s*([а-яa-z\\d]+)\\s*(?:корпус|корп\\.?)\\b", "$1 корп. $2")
            .replaceAll("(?iu)(\\d+)\\s*/\\s*(?:корпус|корп\\.?)\\s*([а-яa-z\\d]+)", "$1 корп. $2")
            .replaceAll("(?iu)(?:,|/)\\s*(?:корпус|корп\\.?|к\\.?)\\s*([а-яa-z\\d]+)", " корп. $1")
            .replaceAll("(?iu)(\\d+\\s*[а-яa-z])\\s*\\.\\s*[а-яa-z]\\b", "$1")
            .replaceAll("(?iu)(\\d+\\s*[а-яa-z]?)\\s*/\\s*(?!(?:\\d+|лит\\.?|литера|корпус|корп\\.?)\\b)[^,\\s]+", "$1")
            .trim();

        address = removeFlat(address);
        address = removeTrailingFlat(address);

        return normalizeAddressForCompare(address)
            .replaceAll("д(?=\\d)", "");
    }

    /**
     * Убирает название города в начале адреса.
     */
    private static String removeCityPrefix(String address) {
        return address
            .replaceAll("(?iu)^\\s*г\\.?\\s*иркутск\\s*,?\\s*", "")
            .replaceAll("(?iu)^\\s*город\\s+иркутск\\s*,?\\s*", "")
            .replaceAll("(?iu)^\\s*иркутск\\s*,?\\s*", "")
            .replaceAll("(?iu)^\\s*г\\.?\\s*рыбинск\\s*,?\\s*", "")
            .replaceAll("(?iu)^\\s*город\\s+рыбинск\\s*,?\\s*", "")
            .replaceAll("(?iu)^\\s*рыбинск\\s*,?\\s*", "");
    }

    private static String removeAdministrativePrefix(String address) {
        return address.replaceFirst("(?iu)^.*?\\b(" + AddressPatterns.STREET_TYPE_PATTERN + ")\\b", "$1");
    }

    /**
     * Убирает номер квартиры из адреса.
     */
    private static String removeFlat(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        return AddressCleaner.cleanup(address)
            .replaceAll("(?iu)[,\\s]*(?:кв\\.?\\s*комн\\.?|комн\\.?)\\s*\\d+[а-яa-z]?\\b.*$", "")
            .replaceAll("(?iu)[,\\s]*(?:к\\s*в\\.?|квартира)\\s*\\d+[а-яa-z]?(?:\\s*к\\.?\\s*\\d+)?\\b.*$", "")
            .trim();
    }

    private static String removeTrailingFlat(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        return AddressCleaner.cleanup(address)
            .replaceAll("(?iu)((?:д\\.?|дом)\\s*№?\\s*\\d+\\s*[а-яa-z]?(?:/\\d+)?),\\s*\\d+[а-яa-z]?\\b.*$", "$1")
            .trim();
    }

    /**
     * Выбирает нужную часть адреса, если в строке через "/" указано несколько адресов.
     */
    private static String chooseAddressPart(String address) {
        if (!address.contains("/")) {
            return address;
        }

        if (!address.matches("(?iu).*?/\\s*(" + AddressPatterns.STREET_TYPE_PATTERN + ")\\b.*")) {
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
}
