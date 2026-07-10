package org.vc.address;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressComparatorTest {

    private final AddressComparator comparator = new AddressComparator();

    @Test
    void shouldSortFlatsByNumericValue() {
        List<String> addresses = new ArrayList<>(List.of(
            "ул Кирова, д.4, кв.10",
            "ул Кирова, д.4, кв.1",
            "ул Кирова, д.4, кв.2"
        ));

        addresses.sort(comparator);

        assertEquals(
            List.of(
                "ул Кирова, д.4, кв.1",
                "ул Кирова, д.4, кв.2",
                "ул Кирова, д.4, кв.10"
            ),
            addresses
        );
    }

    @Test
    void shouldSortAllNumberPartsNaturally() {
        List<String> addresses = new ArrayList<>(List.of(
            "Юбилейная д. 54 корп. 10",
            "Юбилейная д. 54 корп. 2",
            "Юбилейная д. 54 корп. 1",
            "Юбилейная д. 10",
            "Юбилейная д. 2"
        ));

        addresses.sort(comparator);

        assertEquals(
            List.of(
                "Юбилейная д. 2",
                "Юбилейная д. 10",
                "Юбилейная д. 54 корп. 1",
                "Юбилейная д. 54 корп. 2",
                "Юбилейная д. 54 корп. 10"
            ),
            addresses
        );
    }

    @Test
    void shouldSortByHouseThenCorpusThenFlat() {
        List<String> addresses = new ArrayList<>(List.of(
            "Юбилейная д. 54 корп. 2, кв.1",
            "Юбилейная д. 54 корп. 1, кв.10",
            "Юбилейная д. 54 корп. 10, кв.1",
            "Юбилейная д. 54 корп. 1, кв.2",
            "Юбилейная д. 54 корп. 1, кв.1"
        ));

        addresses.sort(comparator);

        assertEquals(
            List.of(
                "Юбилейная д. 54 корп. 1, кв.1",
                "Юбилейная д. 54 корп. 1, кв.2",
                "Юбилейная д. 54 корп. 1, кв.10",
                "Юбилейная д. 54 корп. 2, кв.1",
                "Юбилейная д. 54 корп. 10, кв.1"
            ),
            addresses
        );
    }

    @Test
    void shouldSortFractionalHouseSuffixBeforeFlat() {
        List<String> addresses = new ArrayList<>(List.of(
            "ул Захарова, д.33/10, кв.1",
            "ул Захарова, д.33/2, кв.99",
            "ул Захарова, д.33, кв.1"
        ));

        addresses.sort(comparator);

        assertEquals(
            List.of(
                "ул Захарова, д.33, кв.1",
                "ул Захарова, д.33/2, кв.99",
                "ул Захарова, д.33/10, кв.1"
            ),
            addresses
        );
    }

    @Test
    void shouldSortLargeNumbersWithoutOverflow() {
        List<String> addresses = new ArrayList<>(List.of(
            "ул Кирова, д.4, кв.10000000000000000000",
            "ул Кирова, д.4, кв.9"
        ));

        addresses.sort(comparator);

        assertEquals(
            List.of(
                "ул Кирова, д.4, кв.9",
                "ул Кирова, д.4, кв.10000000000000000000"
            ),
            addresses
        );
    }
}
