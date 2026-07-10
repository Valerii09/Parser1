package org.vc.address;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressNormalizerTest {

    @Test
    void shouldNormalizeStreetTypeAfterStreetName() {
        assertEquals(
            "уллермонтова136к4",
            AddressNormalizer.normalizeHouseAddressForCompare("г.Иркутск, ЛЕРМОНТОВА УЛ., д. 136, корп. 4, кв. 1")
        );
    }

    @Test
    void shouldNormalizeSimpleHouseAddress() {
        assertEquals(
            "улкирова4",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Кирова, д.4, кв.2")
        );
    }

    @Test
    void shouldRemoveFlatWithLetter() {
        assertEquals(
            "уллуговая1",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Луговая, д.1, кв.55А")
        );

        assertEquals(
            "улкуйбышева3",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Куйбышева, д.3, кв.15Б")
        );
    }

    @Test
    void shouldRemoveComplexFlatNumber() {
        assertEquals(
            "улакадемикагубкина1/11",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Академика Губкина, д.1/11, кв.213к.214")
        );
    }

    @Test
    void shouldNormalizeLiteraAsHouseLetter() {
        assertEquals(
            "улрадищева12а",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Радищева, д.12/лит.А, кв.1")
        );
    }

    @Test
    void shouldNormalizeComplexLiteraAsFirstHouseLetter() {
        assertEquals(
            "трактярославский77а",
            AddressNormalizer.normalizeHouseAddressForCompare("тракт Ярославский, д.77/лит.А,А1,А2/, кв.2")
        );
    }

    @Test
    void shouldIgnoreUnknownSlashSuffix() {
        assertEquals(
            "улкирова30",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Кирова, д.30/Гер, кв.4")
        );
    }

    @Test
    void shouldNormalizeHouseWithExtraPointSuffix() {
        assertEquals(
            "улрумянцевская20к",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Румянцевская, д.20к.Б, кв.1")
        );
    }

    @Test
    void shouldNormalizeCityPrefix() {
        assertEquals(
            "улрадищева12а",
            AddressNormalizer.normalizeHouseAddressForCompare("г Иркутск, ул Радищева, д.12А, кв.1")
        );

        assertEquals(
            "улрадищева12а",
            AddressNormalizer.normalizeHouseAddressForCompare("Иркутск, ул Радищева, д.12А, кв.1")
        );
    }

    @Test
    void shouldNormalizeLatinLookAlikeLetters() {
        assertEquals(
            "улрумянцевская20к",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Румянцевская, д.20K, кв.1")
        );

        assertEquals(
            "улрадищева12а",
            AddressNormalizer.normalizeHouseAddressForCompare("ул Радищева, д.12A, кв.1")
        );
    }

    @Test
    void shouldNormalizeStreetTypes() {
        assertEquals(
            "пр50летоктября36",
            AddressNormalizer.normalizeHouseAddressForCompare("пр-кт 50 Лет Октября, д.36, кв.1")
        );

        assertEquals(
            "набкосмонавтов57",
            AddressNormalizer.normalizeHouseAddressForCompare("наб Космонавтов, д.57, кв.1")
        );

        assertEquals(
            "б-р200летрыбинска10",
            AddressNormalizer.normalizeHouseAddressForCompare("б-р 200 Лет Рыбинска, д.10, кв.1")
        );
    }

    @Test
    void shouldNormalizeHouseNumberSignAndTrailingFlat() {
        assertEquals(
            "ул25октября20",
            AddressNormalizer.normalizeHouseAddressForCompare("664022, обл Иркутская, г. Иркутск, ул. 25 Октября, дом № 20, 18")
        );
    }

    @Test
    void shouldRemoveExtraBuildingSuffixForFallback() {
        assertEquals(
            "улкуйбышева3",
            AddressNormalizer.removeExtraBuildingSuffix("улкуйбышева3в")
        );

        assertEquals(
            "улакадемикагубкина1/11",
            AddressNormalizer.removeExtraBuildingSuffix("улакадемикагубкина1/11в")
        );
    }

    @Test
    void shouldKeepCorpusInHouseCompareKey() {
        assertEquals(
            "юбилейная54к3",
            AddressNormalizer.normalizeHouseAddressForCompare("Юбилейная д. 54 корп. 3")
        );

        assertEquals(
            "юбилейная54к3",
            AddressNormalizer.normalizeHouseAddressForCompare("Юбилейная, 54 корп.3, кв.45")
        );
    }

    @Test
    void shouldNormalizeReverseCorpusInHouseCompareKey() {
        assertEquals(
            "бородулина13ак2",
            AddressNormalizer.normalizeHouseAddressForCompare("Бородулина, 13а, 2 корпус, кв.3")
        );
    }

    @Test
    void shouldRemoveRoomFlatFromHouseCompareKey() {
        assertEquals(
            "борисарукавицына6",
            AddressNormalizer.normalizeHouseAddressForCompare("Бориса Рукавицына, 6, кв.комн.94")
        );
    }

    @Test
    void shouldRemoveCorpusForFallback() {
        assertEquals(
            "юбилейная54",
            AddressNormalizer.removeExtraBuildingSuffix("юбилейная54к3")
        );
    }
}
