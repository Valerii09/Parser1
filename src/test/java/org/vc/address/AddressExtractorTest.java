package org.vc.address;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressExtractorTest {

    private final AddressExtractor extractor = new AddressExtractor();

    @Test
    void shouldExtractFactorialInlineAddresses() {
        AddressExtractor factorialExtractor = new AddressExtractor(PaymentSupplier.FACTORIAL);

        assertEquals(
            List.of(
                "г.Иркутск, ул ЛЕРМОНТОВА, д. 136, корп. 4",
                "г.Иркутск, ул ЛЕРМОНТОВА, д. 297Б"
            ),
            factorialExtractor.extractAddresses("""
                1. г.Иркутск, ЛЕРМОНТОВА УЛ., д. 136, корп. 4, кв. 1
                2. г.Иркутск, ЛЕРМОНТОВА УЛ., д. 297Б, кв. 111
                """)
        );
    }

    @Test
    void shouldExtractFactorialVillageAddressInAutoMode() {
        AddressExtractor autoExtractor = new AddressExtractor(PaymentSupplier.AUTO);

        assertEquals(
            List.of("\u0433.\u0418\u0440\u043A\u0443\u0442\u0441\u043A, \u0443\u043B \u0414\u0410\u0427\u041D\u0410\u042F, \u0434. 1"),
            autoExtractor.extractAddresses("""
                \u0414\u041E\u041C\u041E\u0424\u041E\u041D\u041D\u0410\u042F \u0421\u0418\u0421\u0422\u0415\u041C\u0410 \"\u0424\u0410\u041A\u0422\u041E\u0420\u0418\u0410\u041B\" \u0418\u0417\u0412\u0415\u0429\u0415\u041D\u0418\u0415
                \u041E\u0431\u0441\u043B\u0443\u0436\u0438\u0432\u0430\u043D\u0438\u0435 \u041E\u041E\u041E \"\u0424\u0430\u043A\u0442\u043E\u0440\u0438\u0430\u043B \u0412\u043E\u0441\u0442\u043E\u043A\" \u041C\u0435\u0441\u044F\u0446, \u0433\u043E\u0434: \u0418\u042E\u041D\u042C 2026
                \u0433.\u0418\u0440\u043A\u0443\u0442\u0441\u043A, \u041F\u0418\u0412\u041E\u0412\u0410\u0420\u0418\u0425\u0410 \u0421\u0415\u041B\u041E, \u0414\u0410\u0427\u041D\u0410\u042F \u0423\u041B., \u0434. 1, \u043A\u0432. 1 \u041B\u0438\u0446\u0435\u0432\u043E\u0439 \u0441\u0447\u0435\u0442: 62511
                \u0422\u0435\u043A\u0443\u0449\u0430\u044F \u0430\u0431\u043E\u043D\u0435\u043D\u0442\u0441\u043A\u0430\u044F \u043F\u043B\u0430\u0442\u0430
                """)
        );
    }

    @Test
    void shouldExtractFactorialStreetAddressWithLocalNote() {
        AddressExtractor factorialExtractor = new AddressExtractor(PaymentSupplier.FACTORIAL);

        assertEquals(
            List.of("г.Иркутск, ул АНГАРСКАЯ, д. 11"),
            factorialExtractor.extractAddresses("""
                ООО "Факториал Восток"
                г.Иркутск, АНГАРСКАЯ УЛ. (Батар. ст.) , д. 11, кв. 32
                27975 263 ИЗВЕЩЕНИЕ ДОМОФОННАЯ СИСТЕМА "ФАКТОРИАЛ"
                """)
        );
    }

    @Test
    void shouldExtractFactorialLocalityAddressWithoutStreetType() {
        AddressExtractor factorialExtractor = new AddressExtractor(PaymentSupplier.FACTORIAL);

        assertEquals(
            List.of("г.Иркутск, 2-й ГОРОДОК, д. 16"),
            factorialExtractor.extractAddresses("""
                ООО "Факториал Восток"
                г.Иркутск, 2-й ГОРОДОК (Батар. ст.), д. 16, кв. 1
                74502 1 ИЗВЕЩЕНИЕ ДОМОФОННАЯ СИСТЕМА "ФАКТОРИАЛ"
                """)
        );
    }

    @Test
    void shouldExtractFactorialReverseLocalityAddress() {
        AddressExtractor factorialExtractor = new AddressExtractor(PaymentSupplier.FACTORIAL);

        assertEquals(
            List.of("г.Иркутск, рп. МАРКОВА, д. 1"),
            factorialExtractor.extractAddresses("""
                ООО "Факториал Восток"
                г.Иркутск, МАРКОВА Р.П. (Иркут. р-он), , д. 1, кв. 1
                53127 1 ИЗВЕЩЕНИЕ ДОМОФОННАЯ СИСТЕМА "ФАКТОРИАЛ"
                """)
        );
    }

    @Test
    void shouldExtractFactorialBareStreetAddress() {
        AddressExtractor factorialExtractor = new AddressExtractor(PaymentSupplier.FACTORIAL);

        assertEquals(
            List.of("г.Иркутск, ул ЗВЕЗДИНСКАЯ, д. 26"),
            factorialExtractor.extractAddresses("""
                ООО "Факториал Восток"
                г.Иркутск, ЗВЕЗДИНСКАЯ., д. 26, кв. 8
                36711 1643 ИЗВЕЩЕНИЕ ДОМОФОННАЯ СИСТЕМА "ФАКТОРИАЛ"
                """)
        );
    }

    @Test
    void shouldPreferPremiseAddressOverSupplierAddress() {
        String pageText = """
            Платежный документ
            Адрес помещения: г.Усть-Илимск, ул.Южный пер, д.1, кв.118
            Организация-поставщик ООО Управляющая компания
            Адрес: 666683, Иркутская обл, Усть-Илимск г, Мира пр-кт, дом 2, офис 9
            """;

        assertEquals(
            "г.Усть-Илимск, ул.Южный пер, д.1, кв.118",
            extractor.extractAddress(pageText)
        );
    }

    @Test
    void shouldExtractSeveralPremiseAddressesFromOnePage() {
        String pageText = """
            Лицевой счет: 137118
            Адрес помещения: г.Усть-Илимск, ул.Южный пер, д.1, кв.118
            Организация-поставщик ООО Управляющая компания
            Лицевой счет: 137119
            Адрес помещения: г.Усть-Илимск, ул.Мира, д.2, кв.7
            Организация-поставщик ООО Управляющая компания
            """;

        assertEquals(
            List.of(
                "г.Усть-Илимск, ул.Южный пер, д.1, кв.118",
                "г.Усть-Илимск, ул.Мира, д.2, кв.7"
            ),
            extractor.extractAddresses(pageText)
        );
    }

    @Test
    void shouldFallbackToSimpleAddressForPaymentDocumentWithoutPremiseAddress() {
        String pageText = """
            Лицевой счет: 137118
            Адрес помещения: г.Усть-Илимск, ул.Южный пер, д.1, кв.118
            Организация-поставщик ООО Управляющая компания
            Адрес: 666683, Иркутская обл, Усть-Илимск г, Мира пр-кт, дом 2, офис 9
            Лицевой счет: 137119
            Адрес: г.Иркутск, ул.Ленина, д.1, кв.2
            Плательщик: Иванов Иван
            """;

        assertEquals(
            List.of(
                "г.Усть-Илимск, ул.Южный пер, д.1, кв.118",
                "г.Иркутск, ул.Ленина, д.1, кв.2"
            ),
            extractor.extractAddresses(pageText)
        );
    }

    @Test
    void shouldIgnoreSupplierAddressWhenPremiseAddressExists() {
        String pageText = """
            Лицевой счет: 137118
            ФИО плательщика/собственника: Бовсуновский И А
            Адрес помещения: г.Усть-Илимск, ул.Южный пер, д.1, кв.118
            Организация-поставщик услуги: ООО «Домофон сервис»
            Адрес: 666683, Иркутская обл, Усть-Илимск г, Мира пр-кт, дом 2, корпус 3, офис 9
            ИНН /КПП 3817034299/381701001 БИК: 042520607
            """;

        assertEquals(
            List.of("г.Усть-Илимск, ул.Южный пер, д.1, кв.118"),
            extractor.extractAddresses(pageText)
        );
    }

    @Test
    void shouldUseOnlyFirstSimpleAddressWhenPremiseAddressIsMissing() {
        String pageText = """
            Лицевой счет: 137118
            Адрес: г.Усть-Илимск, ул.Южный пер, д.1, кв.118
            Организация-поставщик услуги: ООО «Домофон сервис»
            Адрес: 666683, Иркутская обл, Усть-Илимск г, Мира пр-кт, дом 2, корпус 3, офис 9
            """;

        assertEquals(
            List.of("г.Усть-Илимск, ул.Южный пер, д.1, кв.118"),
            extractor.extractAddresses(pageText)
        );
    }

    @Test
    void shouldNotUseSupplierAddressAsFallback() {
        String pageText = """
            Лицевой счет: 137118
            ФИО плательщика/собственника: Бовсуновский И А
            Организация-поставщик услуги: ООО «Домофон сервис»
            Адрес: 666683, Иркутская обл, Усть-Илимск г, Мира пр-кт, дом 2, корпус 3, офис 9
            """;

        assertEquals(List.of(), extractor.extractAddresses(pageText));
    }

    @Test
    void shouldKeepOldAddressFormatAsFallback() {
        String pageText = """
            Платежный документ
            Адрес: г.Иркутск, ул.Ленина, д.1, кв.2
            Плательщик: Иванов Иван
            """;

        assertEquals(
            "г.Иркутск, ул.Ленина, д.1, кв.2",
            extractor.extractAddress(pageText)
        );
    }

    @Test
    void shouldNotSplitXmlPaymentDocumentByRecipientAccounts() {
        String pageText = """
            \u041f\u043b\u0430\u0442\u0451\u0436\u043d\u044b\u0439 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442
            \u0410\u0434\u0440\u0435\u0441 \u0434\u043e\u0441\u0442\u0430\u0432\u043a\u0438: delivery address (\u0430\u0434\u0440\u0435\u0441 \u043f\u043e\u043c\u0435\u0449\u0435\u043d\u0438\u044f: premise address)
            \u041f\u043e\u0442\u0440\u0435\u0431\u0438\u0442\u0435\u043b\u044c: payer
            \u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f \u0434\u043b\u044f \u0432\u043d\u0435\u0441\u0435\u043d\u0438\u044f \u043f\u043b\u0430\u0442\u044b \u043f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u044e \u043f\u043b\u0430\u0442\u0435\u0436\u0430
            recipient
            \u041b\u0438\u0446\u0435\u0432\u043e\u0439 \u0441\u0447\u0435\u0442: account-1
            \u041f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u044c \u043f\u043b\u0430\u0442\u0435\u0436\u0430: recipient; \u0410\u0434\u0440\u0435\u0441: supplier address
            \u041b\u0438\u0446\u0435\u0432\u043e\u0439 \u0441\u0447\u0435\u0442: account-2
            """;

        assertEquals(
            List.of("premise address"),
            extractor.extractAddresses(pageText)
        );
    }

    @Test
    void shouldExtractMultilineSimpleAddress() {
        String pageText = """
        Платежный документ
        Адрес: 664050, обл Иркутская, г. Иркутск, ул. Байкальская, дом №
        12, кв. 5
        Плательщик: Иванов Иван
        """;

        assertEquals(
            "664050, обл Иркутская, г. Иркутск, ул. Байкальская, дом № 12, кв. 5",
            extractor.extractAddress(pageText)
        );
    }

    @Test
    void shouldExtractMultilinePremiseAddress() {
        String pageText = """
        Лицевой счет: 137118
        Адрес помещения: 664019, обл Иркутская, г. Иркутск, ул. Баррикад, дом №
        35, кв. 12
        Потребитель: Иванов Иван
        """;

        assertEquals(
            List.of("664019, обл Иркутская, г. Иркутск, ул. Баррикад, дом № 35, кв. 12"),
            extractor.extractAddresses(pageText)
        );
    }

    @Test
    void shouldExtractMultilineDeliveryAddress() {
        String pageText = """
        Платёжный документ
        Адрес доставки: 664078, обл Иркутская, г. Иркутск, мкр. Зеленый, дом
        6, кв. 44
        Потребитель: Иванов Иван
        """;

        assertEquals(
            List.of("664078, обл Иркутская, г. Иркутск, мкр. Зеленый, дом 6, кв. 44"),
            extractor.extractAddresses(pageText)
        );
    }

    @Test
    void shouldNotCutAddressByLineBreakBeforeHouseNumber() {
        String pageText = """
        Платежный документ
        Адрес: 664009, обл Иркутская, г. Иркутск, ул. 3-я Летчиков, дом
        15, кв. 1
        Плательщик: Иванов Иван
        """;

        assertEquals(
            "664009, обл Иркутская, г. Иркутск, ул. 3-я Летчиков, дом 15, кв. 1",
            extractor.extractAddress(pageText)
        );
    }

    @Test
    void shouldExtractSingleYaroblvodokanalAddressFromRepeatedReceiptBlocks() {
        AddressExtractor yaroblvodokanalExtractor = new AddressExtractor(PaymentSupplier.YAROBLVODOKANAL);
        String pageText = """
            ГП ЯО "Яроблводоканал", ИНН 7610012391, 152901, г. Рыбинск, Волжская набережная, д.10 к оплате до 15.07.26
            СЧЕТ 062675026061 за Июнь 2026 г.
            ЛИЦЕВОЙ СЧЕТ: 75026061
            Адрес: Расплетина, 7, кв.5
            Плательщик: Белов С.А. Счетчик 1 х.в. 4 г.в.
            ВСЕГО К ОПЛАТЕ: 37,15 р.
            К В И Т А Н Ц И Я за Июнь 2026 г.
            ЛИЦЕВОЙ СЧЕТ: 75026061
            Плательщик: Белов С.А. Адрес: Расплетина, 7, кв.5
            Кол-во прож.: 3 Кол-во собственников: 1 Кол-во льготников: 0
            """;

        assertEquals(
            List.of("Расплетина, 7, кв.5"),
            yaroblvodokanalExtractor.extractAddresses(pageText)
        );
    }

}
