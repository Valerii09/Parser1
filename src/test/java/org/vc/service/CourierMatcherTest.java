package org.vc.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
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

    @Test
    void shouldMatchReorderedStreetNameAndParentheticalNote() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put(
            "СУ_Батарейная",
            Set.of(
                "ул Павла Красильникова д. 172",
                "ул Ангарская (Батарейная ст.) д. 5"
            )
        );

        matcher.init(courierAddresses);

        assertEquals(
            "СУ_Батарейная",
            matcher.findCourierByAddress("ул Красильникова Павла, д.172, кв.1")
        );
        assertEquals(
            "СУ_Батарейная",
            matcher.findCourierByAddress("ул Ангарская, д.5, кв.1")
        );
    }

    @Test
    void shouldUseUniqueCourierForStreetWhenHouseIsMissingFromExcel() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put(
            "ЗУ_Первомайский",
            Set.of("мкр Первомайский д. 1", "мкр Первомайский д. 3")
        );

        matcher.init(courierAddresses);

        assertEquals(
            "ЗУ_Первомайский",
            matcher.findCourierByAddress("мкр Первомайский, д.35, кв.1")
        );
    }

    @Test
    void shouldChooseCourierByNearestHousesOnSharedStreet() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("ЗУ_Синюшка Левый", Set.of("б-р Рябикова д. 10А", "б-р Рябикова д. 12А"));
        courierAddresses.put("ЗУ_Синюшка Правый", Set.of("б-р Рябикова д. 36", "б-р Рябикова д. 40"));

        matcher.init(courierAddresses);

        assertEquals(
            "ЗУ_Синюшка Правый",
            matcher.findCourierByAddress("б-р Рябикова, д.38, кв.1")
        );
    }

    @Test
    void shouldUseCourierZoneNameWhenExcelContainsOnlyRepresentativeAddresses() {
        CourierMatcher matcher = new CourierMatcher();

        assertEquals("радужный", CourierAddressKey.from("мкр Радужный, д.16, кв.1").getStreet());

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("ЗУ_Радужный", Set.of("ул Калинина д. 7А", "ул Калинина д. 9"));

        matcher.init(courierAddresses);

        assertEquals(
            "ЗУ_Радужный",
            matcher.findCourierByAddress("мкр Радужный, д.16, кв.1")
        );
    }

    @Test
    void shouldNotGuessWhenStreetIsEquallyCloseToDifferentCouriers() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("Курьер 1", Set.of("ул Тестовая д. 10"));
        courierAddresses.put("Курьер 2", Set.of("ул Тестовая д. 12"));

        matcher.init(courierAddresses);

        assertNull(matcher.findCourierByAddress("ул Тестовая, д.11, кв.1"));
    }

    @Test
    void shouldKeepShelekhovQuarterAndDistrictDuringMatching() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put(
            "Казимирова_г.Шелехов_ФКР Курьер 2 Асламова",
            Set.of("кв-л 1-й д. 6", "мкр Привокзальный д. 4")
        );
        courierAddresses.put(
            "Казимирова_г.Шелехов_ФКР Курьер 3 Прядко",
            Set.of("кв-л 20-й д. 95")
        );

        matcher.init(courierAddresses);

        assertEquals(
            "Казимирова_г.Шелехов_ФКР Курьер 2 Асламова",
            matcher.findCourierByAddress("г.Иркутск, 1-й КВАРТАЛ. ШЕЛЕХОВ, д. 6, кв. 13")
        );
        assertEquals(
            "Казимирова_г.Шелехов_ФКР Курьер 3 Прядко",
            matcher.findCourierByAddress("г.Иркутск, 20-й КВАРТАЛ. ШЕЛЕХОВ, д. 95, кв. 46")
        );
        assertEquals(
            "Казимирова_г.Шелехов_ФКР Курьер 2 Асламова",
            matcher.findCourierByAddress("г.Иркутск, ПРИВОКЗАЛЬНЫЙ МКР. ШЕЛЕХОВ, д. 4, кв. 30")
        );
    }

    @Test
    void shouldPreferRequestedCityWhenSameStreetExistsInAnotherCity() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put(
            "ЗУ_Синюшка Правый",
            Set.of(
                "г. Иркутск, б-р. Рябикова, д. 36",
                "г. Иркутск, б-р. Рябикова, д. 40"
            )
        );
        courierAddresses.put(
            "Братск курьер",
            Set.of("г. Братск, ул. Рябикова, д. 38")
        );

        matcher.init(courierAddresses);

        assertEquals(
            "ЗУ_Синюшка Правый",
            matcher.findCourierByAddress("г. Иркутск, б-р Рябикова, д. 38")
        );
    }

    @Test
    void shouldMatchShortenedStreetNameWhenHouseBelongsToOneRoute() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put(
            "Казимирова_р-н.Иркутский_ФКР Луговое",
            Set.of("р-н. Иркутский рп. Маркова, ул. Алексея Рыбака, д. 1/1")
        );

        matcher.init(courierAddresses);

        assertEquals(
            "Казимирова_р-н.Иркутский_ФКР Луговое",
            matcher.findCourierByAddress("г.Иркутск, ул Рыбака, д. 1, корп. 1")
        );
    }

    @Test
    void shouldUseRelatedAddressOnlyWhenItPointsToOneCourier() {
        CourierMatcher matcher = new CourierMatcher();

        Map<String, Set<String>> courierAddresses = new LinkedHashMap<>();
        courierAddresses.put("Курьер 1", Set.of("ул Безбокова д. 42"));
        courierAddresses.put("Курьер 2", Set.of("ул Тестовая д. 7"));

        matcher.init(courierAddresses);

        assertEquals(
            "Курьер 1",
            matcher.findCourierByAddresses(List.of("ул Салацкого д. 17", "ул Безбокова д. 42"))
        );
        assertNull(
            matcher.findCourierByAddresses(
                List.of("ул Салацкого д. 17", "ул Безбокова д. 42", "ул Тестовая д. 7")
            )
        );
    }
}
