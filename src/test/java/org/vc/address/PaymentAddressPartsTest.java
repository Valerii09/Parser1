package org.vc.address;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAddressPartsTest {

    @Test
    void shouldNormalizeStreetTypeWrittenAfterStreetName() {
        PaymentAddressParts boulevard = PaymentAddressParts.parse("Гагарина бул., д.72, кв.1");
        PaymentAddressParts district = PaymentAddressParts.parse("Приморский мрн., д.6, кв.1");

        assertEquals("б-р гагарина", boulevard.getStreet());
        assertEquals("мкр приморский", district.getStreet());
    }

    @Test
    void shouldKeepDistrictNameBeforeShelekhovLocality() {
        PaymentAddressParts quarter = PaymentAddressParts.parse(
            "г.Иркутск, 1-й КВАРТАЛ. ШЕЛЕХОВ, д. 6, кв. 13"
        );
        PaymentAddressParts district = PaymentAddressParts.parse(
            "г.Иркутск, 4-й МКР. ШЕЛЕХОВ, д. 65, кв. 109"
        );
        PaymentAddressParts namedDistrict = PaymentAddressParts.parse(
            "г.Иркутск, ПРИВОКЗАЛЬНЫЙ МКР. ШЕЛЕХОВ, д. 4, кв. 30"
        );

        assertEquals("кв-л 1-й", quarter.getStreet());
        assertEquals("мкр 4-й", district.getStreet());
        assertEquals("мкр привокзальный", namedDistrict.getStreet());
    }

    @Test
    void shouldParseStreetTypeAfterStreetName() {
        PaymentAddressParts parts = PaymentAddressParts.parse("г.Иркутск, ЛЕРМОНТОВА УЛ., д. 136, корп. 4, кв. 1");

        assertFalse(parts.isEmpty());
        assertEquals("иркутск", parts.getCity());
        assertEquals("ул лермонтова", parts.getStreet());
        assertEquals(136, parts.getHouseNumber());
        assertEquals("4", parts.getCorpus());
        assertEquals(1, parts.getFlatNumber());
    }

    @Test
    void shouldParseSimpleAddress() {
        PaymentAddressParts parts = PaymentAddressParts.parse("ул Кирова, д.4, кв.2");

        assertFalse(parts.isEmpty());
        assertEquals("ул кирова", parts.getStreet());
        assertEquals(4, parts.getHouseNumber());
        assertEquals("", parts.getHouseLetter());
        assertEquals(2, parts.getFlatNumber());
    }

    @Test
    void shouldParseHouseLetter() {
        PaymentAddressParts parts = PaymentAddressParts.parse("ул Кирова, д.4А, кв.13");

        assertFalse(parts.isEmpty());
        assertEquals("ул кирова", parts.getStreet());
        assertEquals(4, parts.getHouseNumber());
        assertEquals("а", parts.getHouseLetter());
        assertEquals(13, parts.getFlatNumber());
    }

    @Test
    void shouldParseFractionHouse() {
        PaymentAddressParts parts = PaymentAddressParts.parse("ул Захарова, д.33/2, кв.4");

        assertFalse(parts.isEmpty());
        assertEquals("ул захарова", parts.getStreet());
        assertEquals(33, parts.getHouseNumber());
        assertEquals("/2", parts.getHouseLetter());
        assertEquals(4, parts.getFlatNumber());
    }

    @Test
    void shouldParseProspectWithNumberInStreetName() {
        PaymentAddressParts parts = PaymentAddressParts.parse("пр-кт 50 Лет Октября, д.36, кв.1");

        assertFalse(parts.isEmpty());
        assertEquals("пр 50 лет октября", parts.getStreet());
        assertEquals(36, parts.getHouseNumber());
        assertEquals(1, parts.getFlatNumber());
    }

    @Test
    void shouldParseNaberezhnaya() {
        PaymentAddressParts parts = PaymentAddressParts.parse("наб Волжская, д.53, кв.12");

        assertFalse(parts.isEmpty());
        assertEquals("наб волжская", parts.getStreet());
        assertEquals(53, parts.getHouseNumber());
        assertEquals(12, parts.getFlatNumber());
    }

    @Test
    void shouldParseTrakt() {
        PaymentAddressParts parts = PaymentAddressParts.parse("тракт Ярославский, д.77А, кв.2");

        assertFalse(parts.isEmpty());
        assertEquals("тракт ярославский", parts.getStreet());
        assertEquals(77, parts.getHouseNumber());
        assertEquals("а", parts.getHouseLetter());
        assertEquals(2, parts.getFlatNumber());
    }

    @Test
    void shouldParseCorpusWithoutCuttingFirstLetter() {
        PaymentAddressParts parts = PaymentAddressParts.parse("ул Щепкина, д.29/корпусБ, кв.12");

        assertFalse(parts.isEmpty());
        assertEquals("ул щепкина", parts.getStreet());
        assertEquals(29, parts.getHouseNumber());
        assertEquals("б", parts.getCorpus());
        assertEquals(12, parts.getFlatNumber());
    }

    @Test
    void shouldParseCityFromPremiseAddress() {
        PaymentAddressParts parts = PaymentAddressParts.parse("г.Усть-Илимск, ул.Южный пер, д.1, кв.118");

        assertFalse(parts.isEmpty());
        assertEquals("усть-илимск", parts.getCity());
        assertEquals("ул южный пер", parts.getStreet());
        assertEquals(1, parts.getHouseNumber());
        assertEquals(118, parts.getFlatNumber());
    }

    @Test
    void shouldNotParseFlatPrefixAsCorpus() {
        PaymentAddressParts parts = PaymentAddressParts.parse("г.Рыбинск, ул Шлюзовая, д.4, кв.1");

        assertFalse(parts.isEmpty());
        assertEquals("", parts.getCorpus());
        assertEquals(1, parts.getFlatNumber());
    }

    @Test
    void shouldParseHouseNumberSignAndTrailingFlat() {
        PaymentAddressParts parts = PaymentAddressParts.parse(
            "664022, \u043e\u0431\u043b \u0418\u0440\u043a\u0443\u0442\u0441\u043a\u0430\u044f, \u0433. \u0418\u0440\u043a\u0443\u0442\u0441\u043a, \u0443\u043b. 25 \u041e\u043a\u0442\u044f\u0431\u0440\u044f, \u0434\u043e\u043c \u2116 20, 18"
        );

        assertFalse(parts.isEmpty());
        assertEquals("\u0438\u0440\u043a\u0443\u0442\u0441\u043a", parts.getCity());
        assertEquals("\u0443\u043b 25 \u043e\u043a\u0442\u044f\u0431\u0440\u044f", parts.getStreet());
        assertEquals(20, parts.getHouseNumber());
        assertEquals(18, parts.getFlatNumber());
    }

    @Test
    void shouldReturnEmptyForUnknownFormat() {
        PaymentAddressParts parts = PaymentAddressParts.parse("непонятный адрес");

        assertTrue(parts.isEmpty());
    }
}
