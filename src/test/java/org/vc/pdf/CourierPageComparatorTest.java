package org.vc.pdf;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourierPageComparatorTest {

    @Test
    void shouldSortHousesAsNumbers() {
        List<CourierPage> pages = new ArrayList<>();
        pages.add(new CourierPage("пр-кт 50 Лет Октября, д.36, кв.1", Paths.get("36.pdf")));
        pages.add(new CourierPage("пр-кт 50 Лет Октября, д.4, кв.1", Paths.get("4.pdf")));

        pages.sort(new CourierPageComparator());

        assertEquals("пр-кт 50 Лет Октября, д.4, кв.1", pages.get(0).getAddress());
        assertEquals("пр-кт 50 Лет Октября, д.36, кв.1", pages.get(1).getAddress());
    }

    @Test
    void shouldSortFlatsAsNumbers() {
        List<CourierPage> pages = new ArrayList<>();
        pages.add(new CourierPage("ул Кирова, д.4, кв.29", Paths.get("29.pdf")));
        pages.add(new CourierPage("ул Кирова, д.4, кв.3", Paths.get("3.pdf")));
        pages.add(new CourierPage("ул Кирова, д.4, кв.28", Paths.get("28.pdf")));

        pages.sort(new CourierPageComparator());

        assertEquals("ул Кирова, д.4, кв.3", pages.get(0).getAddress());
        assertEquals("ул Кирова, д.4, кв.28", pages.get(1).getAddress());
        assertEquals("ул Кирова, д.4, кв.29", pages.get(2).getAddress());
    }

    @Test
    void shouldSortHouseLettersAfterMainHouse() {
        List<CourierPage> pages = new ArrayList<>();
        pages.add(new CourierPage("ул Кирова, д.4А, кв.1", Paths.get("4a.pdf")));
        pages.add(new CourierPage("ул Кирова, д.4, кв.1", Paths.get("4.pdf")));

        pages.sort(new CourierPageComparator());

        assertEquals("ул Кирова, д.4, кв.1", pages.get(0).getAddress());
        assertEquals("ул Кирова, д.4А, кв.1", pages.get(1).getAddress());
    }
}