package org.vc.service;

import org.vc.address.AddressKeyNormalizer;
import org.vc.address.AddressCleaner;
import org.vc.address.AddressParts;
import org.vc.address.AddressPatterns;
import org.vc.address.PaymentAddressParts;

import java.util.Arrays;
import java.util.Locale;

/**
 * Строит устойчивые ключи адреса для дополнительных способов поиска курьера.
 *
 * <p>Точный индекс остаётся основным. Эти ключи используются только когда
 * исходные файлы по-разному записывают тип улицы, порядок слов или примечание
 * в скобках.</p>
 */
final class CourierAddressKey {

    private final String city;
    private final String street;
    private final int houseNumber;
    private final String houseSuffix;
    private final String corpus;

    private CourierAddressKey(String city, String street, int houseNumber, String houseSuffix, String corpus) {
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.houseSuffix = houseSuffix;
        this.corpus = corpus;
    }

    static CourierAddressKey from(String address) {
        PaymentAddressParts paymentParts = PaymentAddressParts.parse(address);
        if (!paymentParts.isEmpty()) {
            return new CourierAddressKey(
                normalizePart(paymentParts.getCity()),
                normalizeStreet(paymentParts.getStreet()),
                paymentParts.getHouseNumber(),
                normalizePart(paymentParts.getHouseLetter()),
                normalizePart(paymentParts.getCorpus())
            );
        }

        AddressParts parts = AddressParts.parse(address == null ? "" : address);

        return new CourierAddressKey(
            "",
            normalizeStreet(parts.getStreet()),
            parts.getHouseNumber(),
            normalizePart(parts.getHouseSuffix()),
            normalizePart(parts.getCorpus())
        );
    }

    boolean hasHouse() {
        return houseNumber != Integer.MAX_VALUE;
    }

    String getStreet() {
        return street;
    }

    boolean hasCity() {
        return !city.isEmpty();
    }

    int getHouseNumber() {
        return houseNumber;
    }

    String fullHouseKey() {
        if (!hasHouse() || street.isEmpty()) {
            return "";
        }

        return street + "|" + houseNumber + "|" + houseSuffix + "|" + corpus;
    }

    String baseHouseKey() {
        if (!hasHouse() || street.isEmpty()) {
            return "";
        }

        return street + "|" + houseNumber + "|" + houseSuffix;
    }

    String localityFullHouseKey() {
        return hasCity() && !fullHouseKey().isEmpty() ? city + "|" + fullHouseKey() : "";
    }

    String localityBaseHouseKey() {
        return hasCity() && !baseHouseKey().isEmpty() ? city + "|" + baseHouseKey() : "";
    }

    String localityStreetKey() {
        return hasCity() && !street.isEmpty() ? city + "|" + street : "";
    }

    String cacheKey(String legacyHouseKey) {
        return city + "|" + legacyHouseKey;
    }

    private static String normalizeStreet(String street) {
        if (street == null || street.isBlank()) {
            return "";
        }

        String normalized = AddressCleaner.cleanup(street.replaceAll("\\([^)]*\\)", " "))
            .toLowerCase(Locale.ROOT)
            .replaceFirst("(?iu)^(?:" + AddressPatterns.STREET_TYPE_PATTERN + ")\\s*", "")
            .replaceAll("[^а-яёa-z0-9]+", " ")
            .trim();

        if (normalized.isEmpty()) {
            return "";
        }

        return Arrays.stream(normalized.split("\\s+"))
            .filter(token -> !token.isBlank())
            .map(AddressKeyNormalizer::normalizeAddressForCompare)
            .sorted()
            .reduce((first, second) -> first + second)
            .orElse("");
    }

    private static String normalizePart(String value) {
        if (value == null) {
            return "";
        }

        return value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^а-яёa-z0-9/-]+", "")
            .trim();
    }
}
