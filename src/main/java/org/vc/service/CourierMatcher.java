package org.vc.service;

import org.vc.address.AddressNormalizer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Сопоставляет адреса из PDF с курьерами по нормализованным ключам домов.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierMatcher {

    private final Map<String, String> courierByHouseAddress = new LinkedHashMap<>();

    /**
     * Строит индекс курьеров по нормализованным адресам домов.
     */
    public void init(Map<String, Set<String>> courierAddresses) {
        courierByHouseAddress.clear();

        for (Map.Entry<String, Set<String>> entry : courierAddresses.entrySet()) {
            String courierName = entry.getKey();

            for (String courierAddress : entry.getValue()) {
                String houseAddress = AddressNormalizer.normalizeHouseAddressForCompare(courierAddress);

                if (!houseAddress.isEmpty()) {
                    String previousCourier = courierByHouseAddress.putIfAbsent(houseAddress, courierName);

                    if (previousCourier != null && !previousCourier.equals(courierName)) {
                        logDuplicateAddress(houseAddress, previousCourier, courierName, courierAddress);
                    }
                }

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

        System.out.println("Построен индекс адресов курьеров: " + courierByHouseAddress.size());
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

        String courierName = courierByHouseAddress.get(pdfHouseAddress);

        if (courierName != null) {
            logFound(pdfHouseAddress, courierName, pdfAddress);
            return courierName;
        }

        if (hasExplicitCorpus(pdfAddress)) {
            String withoutExtraSuffix = AddressNormalizer.removeExtraBuildingSuffix(pdfHouseAddress);

            if (!withoutExtraSuffix.equals(pdfHouseAddress)) {
                courierName = courierByHouseAddress.get(withoutExtraSuffix);

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

    private boolean hasExplicitCorpus(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }

        return address.matches("(?iu).*(\\bкорпус\\b|\\bкорп\\.?\\b|[,/]\\s*к\\.\\s*[а-яa-z\\d]+).*");
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
