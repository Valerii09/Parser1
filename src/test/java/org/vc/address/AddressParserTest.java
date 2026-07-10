package org.vc.address;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressParserTest {

    private final AddressParser parser = new AddressParser();

    @Test
    void shouldParseHouseRegistryAddressWithoutStreetType() {
        assertEquals(
            List.of("50 лет ВЛКСМ д. 26"),
            parser.parseAddresses("50 лет ВЛКСМ, 26")
        );
    }

    @Test
    void shouldKeepHouseLetterInHouseRegistryAddressWithoutStreetType() {
        assertEquals(
            List.of("Ухтомского д. 12А"),
            parser.parseAddresses("Ухтомского, 12а")
        );
    }

    @Test
    void shouldKeepCorpusInHouseRegistryAddressWithoutStreetType() {
        assertEquals(
            List.of("Юбилейная д. 54 корп. 3"),
            parser.parseAddresses("Юбилейная, 54 корп.3")
        );
    }

    @Test
    void shouldKeepReverseCorpusInHouseRegistryAddressWithoutStreetType() {
        assertEquals(
            List.of("Бородулина д. 13А корп. 2"),
            parser.parseAddresses("Бородулина, 13а, 2 корпус")
        );
    }

    @Test
    void shouldParseQuarterAddressWithFractionAndLetter() {
        assertEquals(
            List.of("кв-л 277-й д. 17/17А"),
            parser.parseAddresses("г. Ангарск, 277-й кв-л., д. 17/17а")
        );
    }

    @Test
    void shouldParseLetterOnlyHouse() {
        assertEquals(
            List.of("мкр 7-й д. А"),
            parser.parseAddresses("г. Ангарск, 7-й мкр., д. А")
        );
    }

    @Test
    void shouldParseAttachedCorpusAfterHouse() {
        assertEquals(
            List.of("ул Петра Поручикова д. 4А корп. В"),
            parser.parseAddresses("г. Бодайбо, ул. Петра Поручикова, д. 4АкорпусВ")
        );
    }

    @Test
    void shouldSeparateHouseLetterAndAttachedCorpusLetter() {
        assertEquals(
            List.of("ул Петра Поручикова д. 4А корп. А"),
            parser.parseAddresses("г. Бодайбо, ул. Петра Поручикова, д. 4АКОРПУСА")
        );
    }

    @Test
    void shouldParseAttachedBlockAfterHouse() {
        assertEquals(
            List.of("ул Урицкого д. 24 блок 3"),
            parser.parseAddresses("г. Бодайбо, ул. Урицкого, д. 24БЛОК3")
        );
    }

    @Test
    void shouldParseStreetTypeAfterStreetNameWithHouseRange() {
        assertEquals(
            List.of("ул Ядринцева д. 34-36"),
            parser.parseAddresses(", Иркутская область, г. Иркутск, Ядринцева ул, дом № 34-36")
        );
    }

    @Test
    void shouldParseLocalityAddressWithoutStreet() {
        assertEquals(
            List.of("п. Молодежный д. 2 корп. А"),
            parser.parseAddresses("р-н. Иркутский п. Молодежный, д. 2КОРПА")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "г. Ангарск, 94-й кв-л., д. 3а/3б",
        "г. Бодайбо, ул. Петра Поручикова, д. 4АКОРПУСА",
        "г. Нижнеудинск, ул. Восточный переезд, д. 21КОРП1",
        "г. Усолье-Сибирское, ул. Машиностроителей, д. 8АКОРПБ",
        "г. Шелехов, кв-л. 11-й, д. 4КОРП3",
        "Иркутский р-н. с. Хомутово, ул. Чапаева, д. 12АКОРПУС3",
        "р-н. Иркутский д. Новолисиха, ул. Спортивная, д. 3КОРПУС2",
        "р-н. Киренский г. Киренск мкр. Авиагородок, ул. Гастелло, д. 5КОРП1"
    })
    void shouldParseAddressFormatsFromFailureLog(String rawAddress) {
        org.junit.jupiter.api.Assertions.assertFalse(parser.parseAddresses(rawAddress).isEmpty());
    }
}
