package org.vc.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CourierMatcherTest {

    @Test
    void shouldFindCourierBySimpleAddress() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("Рыбинск курьер 14", Set.of("ул Радищева д. 12А"));

        matcher.init(courierAddresses);

        assertEquals(
            "Рыбинск курьер 14",
            matcher.findCourierByAddress("ул Радищева, д.12/лит.А, кв.1")
        );
    }

    @Test
    void shouldFindCourierByAddressWithFlatLetter() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("Рыбинск курьер 18", Set.of("ул Луговая д. 1"));

        matcher.init(courierAddresses);

        assertEquals(
            "Рыбинск курьер 18",
            matcher.findCourierByAddress("ул Луговая, д.1, кв.55А")
        );
    }

    @Test
    void shouldFindCourierByFallbackWithoutCorpus() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("Рыбинск курьер 15", Set.of("ул Куйбышева д. 3"));

        matcher.init(courierAddresses);

        assertEquals(
            "Рыбинск курьер 15",
            matcher.findCourierByAddress("ул Куйбышева, д.3, корпусВ, кв.1")
        );
    }

    @Test
    void shouldFindCourierByFractionHouse() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("Рыбинск курьер 15", Set.of("ул Захарова д. 33/2"));

        matcher.init(courierAddresses);

        assertEquals(
            "Рыбинск курьер 15",
            matcher.findCourierByAddress("ул Захарова, д.33/2, кв.4")
        );
    }

    @Test
    void shouldReturnNullWhenAddressIsUnknown() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("Рыбинск курьер 1", Set.of("ул Кирова д. 4"));

        matcher.init(courierAddresses);

        assertNull(matcher.findCourierByAddress("ул Неизвестная, д.1, кв.1"));
    }
}