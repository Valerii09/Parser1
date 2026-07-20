package org.vc.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PdfFileRoutingContextTest {

    @Test
    void shouldResolveRepeatedAddressByConfirmedAssignment() {
        PdfFileRoutingContext context = new PdfFileRoutingContext();
        context.remember("Иркутск курьер 2", 10, List.of("г. Иркутск, ул. Чекалина, д. 18, кв. 1"));

        String courier = context.resolve(
            List.of("г Иркутск, улица Чекалина, дом 18, квартира 54"),
            100
        );

        assertEquals("Иркутск курьер 2", courier);
    }

    @Test
    void shouldUseNearestConfirmedPageWhenAddressIsUnknown() {
        PdfFileRoutingContext context = new PdfFileRoutingContext();
        context.remember("Курьер 1", 5, List.of("ул. Ленина, д. 1"));
        context.remember("Курьер 2", 20, List.of("ул. Мира, д. 2"));

        assertEquals("Курьер 2", context.resolve(List.of("неизвестный адрес"), 18));
    }

    @Test
    void shouldNotResolveEmptyContext() {
        assertNull(new PdfFileRoutingContext().resolve(List.of("ул. Ленина, д. 1"), 1));
    }
}
