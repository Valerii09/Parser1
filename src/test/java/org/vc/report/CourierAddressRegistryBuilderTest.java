package org.vc.report;

import org.junit.jupiter.api.Test;
import org.vc.pdf.CourierPage;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourierAddressRegistryBuilderTest {

    private final CourierAddressRegistryBuilder builder = new CourierAddressRegistryBuilder();

    @Test
    void shouldGroupPaymentDocumentsByHouseAddress() {
        List<CourierPage> pages = List.of(
            page("г.Рыбинск, ул Шлюзовая, д.4, кв.1"),
            page("г.Рыбинск, ул Шлюзовая, д.4, кв.2"),
            page("г.Рыбинск, ул Шлюзовая, д.5, кв.1")
        );

        List<CourierAddressRegistryRow> rows = builder.build("Рыбинск курьер 1", pages);

        assertEquals(2, rows.size());
        assertEquals("г.рыбинск, ул шлюзовая, д.4", rows.get(0).getAddress());
        assertEquals(2, rows.get(0).getPaymentDocumentsCount());
        assertEquals("Рыбинск курьер 1", rows.get(0).getCourierName());
        assertEquals("г.рыбинск, ул шлюзовая, д.5", rows.get(1).getAddress());
        assertEquals(1, rows.get(1).getPaymentDocumentsCount());
    }

    @Test
    void shouldKeepCorpusAsPartOfHouseAddress() {
        List<CourierPage> pages = List.of(
            page("г.Рыбинск, ул Шлюзовая, д.4, корпусА, кв.1"),
            page("г.Рыбинск, ул Шлюзовая, д.4, корпусБ, кв.2")
        );

        List<CourierAddressRegistryRow> rows = builder.build("Рыбинск курьер 1", pages);

        assertEquals(2, rows.size());
        assertEquals("г.рыбинск, ул шлюзовая, д.4, к.а", rows.get(0).getAddress());
        assertEquals("г.рыбинск, ул шлюзовая, д.4, к.б", rows.get(1).getAddress());
    }

    @Test
    void shouldUseRawAddressWhenAddressCannotBeParsed() {
        List<CourierPage> pages = List.of(page("адрес без дома"));

        List<CourierAddressRegistryRow> rows = builder.build("Рыбинск курьер 1", pages);

        assertEquals(1, rows.size());
        assertEquals("адрес без дома", rows.get(0).getAddress());
    }

    @Test
    void shouldUsePaymentDocumentsCountFromCourierPage() {
        List<CourierPage> pages = List.of(
            new CourierPage(
                "г.Иркутск, ЛЕРМОНТОВА УЛ., д. 136, корп. 4, кв. 1",
                List.of(Path.of("page.pdf")),
                2
            ),
            new CourierPage(
                "г.Иркутск, ЛЕРМОНТОВА УЛ., д. 136, корп. 4, кв. 8",
                List.of(Path.of("page2.pdf")),
                3
            )
        );

        List<CourierAddressRegistryRow> rows = builder.build("ЗУ 10", pages);

        assertEquals(1, rows.size());
        assertEquals(5, rows.get(0).getPaymentDocumentsCount());
    }

    @Test
    void shouldWriteAllPageAddressesWhenSortingUsesOnlyFirstAddress() {
        CourierPage page = new CourierPage(
            "г. Иркутск, ул. Ленина, д. 1, кв. 10",
            List.of(Path.of("page.pdf")),
            List.of(
                "г. Иркутск, ул. Ленина, д. 1, кв. 10",
                "г. Иркутск, ул. Байкальская, д. 20, кв. 15"
            )
        );

        List<CourierAddressRegistryRow> rows = builder.build("Иркутск курьер 1", List.of(page));

        assertEquals("г.иркутск, ул ленина, д.1", rows.get(0).getAddress());
        assertEquals(1, rows.get(0).getPaymentDocumentsCount());
        assertEquals("г.иркутск, ул байкальская, д.20", rows.get(1).getAddress());
        assertEquals(1, rows.get(1).getPaymentDocumentsCount());
    }

    @Test
    void shouldCountBothPaymentDocumentsWhenRegistryAddressesAreEqual() {
        CourierPage page = new CourierPage(
            "г. Иркутск, ул. Ленина, д. 1, кв. 10",
            List.of(Path.of("page.pdf")),
            List.of(
                "г. Иркутск, ул. Ленина, д. 1, кв. 10",
                "г. Иркутск, ул. Ленина, д. 1, кв. 11"
            )
        );

        List<CourierAddressRegistryRow> rows = builder.build("Иркутск курьер 1", List.of(page));

        assertEquals(1, rows.size());
        assertEquals("г.иркутск, ул ленина, д.1", rows.get(0).getAddress());
        assertEquals(2, rows.get(0).getPaymentDocumentsCount());
    }

    private CourierPage page(String address) {
        return new CourierPage(address, Path.of("page.pdf"));
    }
}
