package org.vc.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Хранит подтверждённые назначения внутри одного исходного PDF и помогает
 * распределить редкие адреса, которые не удалось сопоставить напрямую.
 */
final class PdfFileRoutingContext {

    private final Map<String, Set<String>> couriersByAddress = new LinkedHashMap<>();
    private final Map<Integer, Set<String>> couriersByPage = new LinkedHashMap<>();

    void remember(String courierName, int pageIndex, List<String> addresses) {
        couriersByPage
            .computeIfAbsent(pageIndex, ignored -> new LinkedHashSet<>())
            .add(courierName);

        for (String address : addresses) {
            String key = addressKey(address);
            if (!key.isEmpty()) {
                couriersByAddress
                    .computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                    .add(courierName);
            }
        }
    }

    String resolve(List<String> addresses, int pageIndex) {
        String courierName = findByAddress(addresses);
        if (courierName != null) {
            return courierName;
        }

        courierName = findNearestPage(pageIndex);
        return courierName != null ? courierName : findDominantCourier();
    }

    private String findByAddress(List<String> addresses) {
        Set<String> couriers = new LinkedHashSet<>();

        for (String address : addresses) {
            Set<String> addressCouriers = couriersByAddress.get(addressKey(address));
            if (addressCouriers != null) {
                couriers.addAll(addressCouriers);
            }
        }

        return couriers.size() == 1 ? couriers.iterator().next() : null;
    }

    private String findNearestPage(int pageIndex) {
        int nearestDistance = Integer.MAX_VALUE;
        String nearestCourier = null;

        for (Map.Entry<Integer, Set<String>> entry : couriersByPage.entrySet()) {
            if (entry.getValue().size() != 1) {
                continue;
            }

            int distance = Math.abs(entry.getKey() - pageIndex);
            if (distance < nearestDistance || distance == nearestDistance && entry.getKey() < pageIndex) {
                nearestDistance = distance;
                nearestCourier = entry.getValue().iterator().next();
            }
        }

        return nearestCourier;
    }

    private String findDominantCourier() {
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (Set<String> couriers : couriersByPage.values()) {
            if (couriers.size() == 1) {
                counts.merge(couriers.iterator().next(), 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private String addressKey(String address) {
        CourierAddressKey key = CourierAddressKey.from(address);
        String localityKey = key.localityFullHouseKey();

        return localityKey.isEmpty() ? key.fullHouseKey() : localityKey;
    }
}
