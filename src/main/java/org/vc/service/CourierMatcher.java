package org.vc.service;

import org.vc.address.AddressNormalizer;
import org.vc.address.AddressKeyNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сопоставляет адреса из PDF с курьерами по нормализованным ключам домов.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierMatcher {

    private final Map<String, Set<String>> couriersByHouseAddress = new LinkedHashMap<>();
    private final Map<String, Set<String>> couriersByLocalityHouseAddress = new LinkedHashMap<>();
    private final Map<String, Set<String>> couriersByLooseHouseAddress = new LinkedHashMap<>();
    private final Map<String, Set<String>> couriersByBaseHouseAddress = new LinkedHashMap<>();
    private final Map<String, Set<String>> couriersByLocalityLooseHouseAddress = new LinkedHashMap<>();
    private final Map<String, Set<String>> couriersByLocalityBaseHouseAddress = new LinkedHashMap<>();
    private final Map<String, Set<IndexedCourierAddress>> addressesByStreet = new LinkedHashMap<>();
    private final Map<String, Set<IndexedCourierAddress>> addressesByLocalityStreet = new LinkedHashMap<>();
    private final Set<String> courierNames = new LinkedHashSet<>();
    private final Map<String, Optional<String>> fallbackCache = new ConcurrentHashMap<>();

    /**
     * Строит индекс курьеров по нормализованным адресам домов.
     */
    public void init(Map<String, Set<String>> courierAddresses) {
        couriersByHouseAddress.clear();
        couriersByLocalityHouseAddress.clear();
        couriersByLooseHouseAddress.clear();
        couriersByBaseHouseAddress.clear();
        couriersByLocalityLooseHouseAddress.clear();
        couriersByLocalityBaseHouseAddress.clear();
        addressesByStreet.clear();
        addressesByLocalityStreet.clear();
        courierNames.clear();
        fallbackCache.clear();

        for (Map.Entry<String, Set<String>> entry : courierAddresses.entrySet()) {
            String courierName = entry.getKey();
            courierNames.add(courierName);

            for (String courierAddress : entry.getValue()) {
                String houseAddress = AddressNormalizer.normalizeHouseAddressForCompare(courierAddress);

                if (!houseAddress.isEmpty()) {
                    addExactCourier(houseAddress, courierName, courierAddress);
                }

                indexLooseAddress(courierName, courierAddress);

                if (isDebugAddress(courierAddress)) {
                    System.out.println(
                        "INDEX: ["
                            + houseAddress
                            + "] -> "
                            + courierName
                            + " / "
                            + courierAddress
                    );
                }
            }
        }

        System.out.println("Построен индекс адресов курьеров: " + couriersByHouseAddress.size());
    }

    /**
     * Возвращает имя курьера по адресу из PDF или {@code null}, если совпадение не найдено.
     */
    public String findCourierByAddress(String pdfAddress) {
        String pdfHouseAddress = AddressNormalizer.normalizeHouseAddressForCompare(pdfAddress);

        if (pdfHouseAddress.isEmpty()) {
            if (isDebugAddress(pdfAddress)) {
                System.out.println("PDF АДРЕС НЕ ДАЛ КЛЮЧ: " + pdfAddress);
            }
            return null;
        }

        CourierAddressKey addressKey = CourierAddressKey.from(pdfAddress);
        boolean localityStreetKnown = hasLocalityStreetRoute(addressKey);
        String courierName = findUniqueCourier(
            couriersByLocalityHouseAddress.get(addressKey.localityFullHouseKey())
        );

        if (courierName == null && !localityStreetKnown) {
            courierName = findUniqueCourier(couriersByHouseAddress.get(pdfHouseAddress));
        }

        if (courierName != null) {
            logFound(pdfHouseAddress, courierName, pdfAddress);
            return courierName;
        }

        return fallbackCache
            .computeIfAbsent(
                addressKey.cacheKey(pdfHouseAddress),
                ignored -> Optional.ofNullable(
                    findCourierByFallback(pdfAddress, pdfHouseAddress, addressKey, localityStreetKnown)
                )
            )
            .orElse(null);
    }

    /**
     * Определяет курьера по первому адресу платёжки, а при его отсутствии использует остальные
     * адреса только тогда, когда все найденные варианты ведут к одному курьеру.
     *
     * <p>Такой fallback нужен для документов, где основной адрес записан с ошибкой или отсутствует
     * в маршруте Excel, но второй адрес той же платёжки однозначно подтверждает курьерскую зону.</p>
     */
    public String findCourierByAddresses(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        String primaryCourier = findCourierByAddress(addresses.get(0));
        if (primaryCourier != null || addresses.size() == 1) {
            return primaryCourier;
        }

        Set<String> relatedCouriers = new LinkedHashSet<>();
        for (int index = 1; index < addresses.size(); index++) {
            String courier = findCourierByAddress(addresses.get(index));
            if (courier != null) {
                relatedCouriers.add(courier);
            }
        }

        String courier = findUniqueCourier(relatedCouriers);
        if (courier != null) {
            logFallback("связанному адресу платёжки", courier, addresses.get(0));
        }
        return courier;
    }

    private String findCourierByFallback(
        String pdfAddress,
        String pdfHouseAddress,
        CourierAddressKey addressKey,
        boolean localityStreetKnown
    ) {
        String courierName;

        if (hasExplicitCorpus(pdfAddress)) {
            String withoutExtraSuffix = AddressNormalizer.removeExtraBuildingSuffix(pdfHouseAddress);

            if (!withoutExtraSuffix.equals(pdfHouseAddress)) {
                courierName = localityStreetKnown
                    ? null
                    : findUniqueCourier(couriersByHouseAddress.get(withoutExtraSuffix));

                if (courierName != null) {
                    System.out.println(
                        "PDF НАЙДЕН ПО FALLBACK БЕЗ КОРПУСА: ["
                            + pdfHouseAddress
                            + "] -> ["
                            + withoutExtraSuffix
                            + "] -> "
                            + courierName
                            + " / "
                            + pdfAddress
                    );
                    return courierName;
                }
            }
        }

        courierName = findUniqueCourier(
            couriersByLocalityLooseHouseAddress.get(addressKey.localityFullHouseKey())
        );
        if (courierName != null) {
            logFallback("адресу в населённом пункте", courierName, pdfAddress);
            return courierName;
        }

        courierName = findUniqueCourier(
            couriersByLocalityBaseHouseAddress.get(addressKey.localityBaseHouseKey())
        );
        if (courierName != null) {
            logFallback("адресу без корпуса в населённом пункте", courierName, pdfAddress);
            return courierName;
        }

        courierName = localityStreetKnown
            ? null
            : findUniqueCourier(couriersByLooseHouseAddress.get(addressKey.fullHouseKey()));
        if (courierName != null) {
            logFallback("каноническому адресу", courierName, pdfAddress);
            return courierName;
        }

        courierName = localityStreetKnown
            ? null
            : findUniqueCourier(couriersByBaseHouseAddress.get(addressKey.baseHouseKey()));
        if (courierName != null) {
            logFallback("адресу без корпуса", courierName, pdfAddress);
            return courierName;
        }

        courierName = findByStreet(addressKey);
        if (courierName != null) {
            logFallback("маршруту улицы", courierName, pdfAddress);
            return courierName;
        }

        courierName = findByContainedStreet(addressKey);
        if (courierName != null) {
            logFallback("сокращённому названию улицы", courierName, pdfAddress);
            return courierName;
        }

        courierName = findByCourierName(addressKey.getStreet());
        if (courierName != null) {
            logFallback("названию зоны курьера", courierName, pdfAddress);
            return courierName;
        }

        courierName = findBySimilarStreet(addressKey);
        if (courierName != null) {
            logFallback("похожему названию улицы", courierName, pdfAddress);
            return courierName;
        }

        if (isDebugAddress(pdfAddress)) {
            System.out.println(
                "PDF НЕ НАЙДЕН В ИНДЕКСЕ: ["
                    + pdfHouseAddress
                    + "] / "
                    + pdfAddress
            );
        }

        return null;
    }

    private void indexLooseAddress(String courierName, String courierAddress) {
        CourierAddressKey key = CourierAddressKey.from(courierAddress);

        addCourier(couriersByLooseHouseAddress, key.fullHouseKey(), courierName);
        addCourier(couriersByBaseHouseAddress, key.baseHouseKey(), courierName);
        addCourier(couriersByLocalityLooseHouseAddress, key.localityFullHouseKey(), courierName);
        addCourier(couriersByLocalityBaseHouseAddress, key.localityBaseHouseKey(), courierName);

        if (!key.getStreet().isEmpty() && key.hasHouse()) {
            IndexedCourierAddress indexedAddress = new IndexedCourierAddress(courierName, key.getHouseNumber());
            addressesByStreet
                .computeIfAbsent(key.getStreet(), ignored -> new LinkedHashSet<>())
                .add(indexedAddress);

            if (key.hasCity()) {
                addressesByLocalityStreet
                    .computeIfAbsent(key.localityStreetKey(), ignored -> new LinkedHashSet<>())
                    .add(indexedAddress);
            }
        }
    }

    private void addExactCourier(String houseAddress, String courierName, String courierAddress) {
        Set<String> couriers = couriersByHouseAddress
            .computeIfAbsent(houseAddress, ignored -> new LinkedHashSet<>());

        for (String previousCourier : couriers) {
            if (!previousCourier.equals(courierName)) {
                logDuplicateAddress(houseAddress, previousCourier, courierName, courierAddress);
            }
        }
        couriers.add(courierName);

        CourierAddressKey key = CourierAddressKey.from(courierAddress);
        addCourier(couriersByLocalityHouseAddress, key.localityFullHouseKey(), courierName);
    }

    private void addCourier(Map<String, Set<String>> index, String key, String courierName) {
        if (key == null || key.isEmpty()) {
            return;
        }

        index.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(courierName);
    }

    private boolean hasLocalityStreetRoute(CourierAddressKey addressKey) {
        return addressKey.hasCity()
            && addressesByLocalityStreet.containsKey(addressKey.localityStreetKey());
    }

    private String findByStreet(CourierAddressKey addressKey) {
        if (addressKey.getStreet().isEmpty()) {
            return null;
        }

        if (addressKey.hasCity()) {
            Set<IndexedCourierAddress> localityCandidates = addressesByLocalityStreet.get(
                addressKey.localityStreetKey()
            );
            if (localityCandidates != null && !localityCandidates.isEmpty()) {
                return findByStreetCandidates(addressKey, localityCandidates);
            }
        }

        return findByStreetCandidates(addressKey, addressesByStreet.get(addressKey.getStreet()));
    }

    private String findByContainedStreet(CourierAddressKey addressKey) {
        String requestedStreet = addressKey.getStreet();
        if (requestedStreet.length() < 4) {
            return null;
        }

        if (addressKey.hasCity()) {
            String localityKey = addressKey.localityStreetKey();
            String localityPrefix = localityKey.substring(0, localityKey.length() - requestedStreet.length());
            Set<IndexedCourierAddress> localityCandidates = collectContainedStreetCandidates(
                requestedStreet,
                addressesByLocalityStreet,
                localityPrefix
            );
            if (!localityCandidates.isEmpty()) {
                return findByStreetCandidates(addressKey, localityCandidates);
            }
        }

        return findByStreetCandidates(
            addressKey,
            collectContainedStreetCandidates(requestedStreet, addressesByStreet, "")
        );
    }

    private Set<IndexedCourierAddress> collectContainedStreetCandidates(
        String requestedStreet,
        Map<String, Set<IndexedCourierAddress>> index,
        String keyPrefix
    ) {
        Set<IndexedCourierAddress> result = new LinkedHashSet<>();

        for (Map.Entry<String, Set<IndexedCourierAddress>> entry : index.entrySet()) {
            String indexedKey = entry.getKey();
            if (!keyPrefix.isEmpty() && !indexedKey.startsWith(keyPrefix)) {
                continue;
            }

            String indexedStreet = keyPrefix.isEmpty()
                ? indexedKey
                : indexedKey.substring(keyPrefix.length());
            if (indexedStreet.contains(requestedStreet) || requestedStreet.contains(indexedStreet)) {
                result.addAll(entry.getValue());
            }
        }

        return result;
    }

    private String findByStreetCandidates(
        CourierAddressKey addressKey,
        Set<IndexedCourierAddress> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Set<String> streetCouriers = new LinkedHashSet<>();
        for (IndexedCourierAddress candidate : candidates) {
            streetCouriers.add(candidate.courierName());
        }

        String uniqueStreetCourier = findUniqueCourier(streetCouriers);
        if (uniqueStreetCourier != null) {
            return uniqueStreetCourier;
        }

        if (!addressKey.hasHouse()) {
            return null;
        }

        List<IndexedCourierAddress> preferredCandidates = candidates.stream()
            .filter(candidate -> candidate.houseNumber() % 2 == addressKey.getHouseNumber() % 2)
            .toList();

        if (preferredCandidates.isEmpty()) {
            preferredCandidates = new ArrayList<>(candidates);
        }

        int minimumDistance = preferredCandidates.stream()
            .mapToInt(candidate -> Math.abs(candidate.houseNumber() - addressKey.getHouseNumber()))
            .min()
            .orElse(Integer.MAX_VALUE);

        Set<String> nearestCouriers = new LinkedHashSet<>();
        for (IndexedCourierAddress candidate : preferredCandidates) {
            if (Math.abs(candidate.houseNumber() - addressKey.getHouseNumber()) == minimumDistance) {
                nearestCouriers.add(candidate.courierName());
            }
        }

        return findUniqueCourier(nearestCouriers);
    }

    private String findByCourierName(String street) {
        if (street == null || street.length() < 5) {
            return null;
        }

        Set<String> matches = new LinkedHashSet<>();
        for (String courierName : courierNames) {
            String normalizedCourierName = AddressKeyNormalizer.normalizeAddressForCompare(courierName)
                .replaceAll("[^а-яёa-z0-9]+", "");
            if (normalizedCourierName.contains(street)) {
                matches.add(courierName);
            }
        }

        return findUniqueCourier(matches);
    }

    private String findBySimilarStreet(CourierAddressKey addressKey) {
        String requestedStreet = addressKey.getStreet();
        if (requestedStreet.length() < 5) {
            return null;
        }

        if (addressKey.hasCity()) {
            String localityKey = addressKey.localityStreetKey();
            String localityPrefix = localityKey.substring(0, localityKey.length() - requestedStreet.length());
            String localityResult = findBySimilarStreetInIndex(
                addressKey,
                addressesByLocalityStreet,
                localityPrefix
            );
            if (localityResult != null) {
                return localityResult;
            }
        }

        return findBySimilarStreetInIndex(addressKey, addressesByStreet, "");
    }

    private String findBySimilarStreetInIndex(
        CourierAddressKey addressKey,
        Map<String, Set<IndexedCourierAddress>> index,
        String keyPrefix
    ) {
        String requestedStreet = addressKey.getStreet();

        int allowedDistance = Math.min(3, Math.max(1, requestedStreet.length() / 5));
        int bestDistance = Integer.MAX_VALUE;
        List<String> bestStreets = new ArrayList<>();

        for (String indexedKey : index.keySet()) {
            if (!keyPrefix.isEmpty() && !indexedKey.startsWith(keyPrefix)) {
                continue;
            }
            String indexedStreet = keyPrefix.isEmpty()
                ? indexedKey
                : indexedKey.substring(keyPrefix.length());
            int distance = levenshteinDistance(requestedStreet, indexedStreet);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestStreets.clear();
                bestStreets.add(indexedKey);
            } else if (distance == bestDistance) {
                bestStreets.add(indexedKey);
            }
        }

        if (bestDistance > allowedDistance || bestStreets.size() != 1) {
            return null;
        }

        return findByStreetCandidates(addressKey, index.get(bestStreets.get(0)));
    }

    private int levenshteinDistance(String first, String second) {
        int[] previous = new int[second.length() + 1];
        int[] current = new int[second.length() + 1];

        for (int column = 0; column <= second.length(); column++) {
            previous[column] = column;
        }

        for (int row = 1; row <= first.length(); row++) {
            current[0] = row;

            for (int column = 1; column <= second.length(); column++) {
                int replacementCost = first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                    Math.min(current[column - 1] + 1, previous[column] + 1),
                    previous[column - 1] + replacementCost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[second.length()];
    }

    private String findUniqueCourier(Set<String> couriers) {
        if (couriers == null || couriers.size() != 1) {
            return null;
        }

        return couriers.iterator().next();
    }

    private void logFallback(String reason, String courierName, String pdfAddress) {
        System.out.println(
            "PDF НАЙДЕН ПО " + reason.toUpperCase() + ": "
                + courierName + " / " + pdfAddress
        );
    }

    private record IndexedCourierAddress(String courierName, int houseNumber) {
    }

    private boolean hasExplicitCorpus(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }

        return address.matches("(?iu).*(\\bкорпус\\s*[а-яa-z\\d]*\\b|\\bкорп\\.?\\s*[а-яa-z\\d]*\\b|[,/]\\s*к\\.\\s*[а-яa-z\\d]+).*");
    }

    private void logFound(String pdfHouseAddress, String courierName, String pdfAddress) {
        if (!isDebugAddress(pdfAddress)) {
            return;
        }

        System.out.println(
            "PDF НАЙДЕН: ["
                + pdfHouseAddress
                + "] -> "
                + courierName
                + " / "
                + pdfAddress
        );
    }

    private void logDuplicateAddress(
        String houseAddress,
        String previousCourier,
        String courierName,
        String courierAddress
    ) {
        System.out.println(
            "ВНИМАНИЕ: адрес уже есть у другого курьера: ["
                + houseAddress
                + "] первый: " + previousCourier
                + ", второй: " + courierName
                + ", адрес: " + courierAddress
        );
    }

    private boolean isDebugAddress(String address) {
        if (address == null) {
            return false;
        }

        String value = address.toLowerCase();

        return value.contains("радищ")
            || value.contains("ярослав")
            || value.contains("щепкин")
            || value.contains("киров")
            || value.contains("румянцев")
            || value.contains("куйбыш")
            || value.contains("лугов")
            || value.contains("губкин")
            || value.contains("глеб")
            || value.contains("успенск")
            || value.contains("сурков")
            || value.contains("ленина")
            || value.contains("трактор")
            || value.contains("молодеж")
            || value.contains("космонавт")
            || value.contains("желяб")
            || value.contains("чернях")
            || value.contains("выборг")
            || value.contains("нансен")
            || value.contains("юбилей")
            || value.contains("моисеенко")
            || value.contains("горьк")
            || value.contains("батов")
            || value.contains("рабкоров")
            || value.contains("серов")
            || value.contains("малинов")
            || value.contains("9 мая")
            || value.contains("ворошил")
            || value.contains("свобод")
            || value.contains("фурман")
            || value.contains("карякин")
            || value.contains("кустов")
            || value.contains("моторостро");
    }
}
