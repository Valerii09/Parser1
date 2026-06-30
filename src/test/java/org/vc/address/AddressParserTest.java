package org.vc.address;

import org.junit.jupiter.api.Test;

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
}
