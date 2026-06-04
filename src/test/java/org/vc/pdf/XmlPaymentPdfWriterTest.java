package org.vc.pdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vc.payment.PaymentCharge;
import org.vc.payment.PaymentDocument;
import org.vc.payment.PaymentMeter;
import org.vc.payment.PaymentQrCode;
import org.vc.payment.PaymentRecipient;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlPaymentPdfWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldWritePaymentDocumentsToPdfParts() throws Exception {
        try (XmlPaymentPdfWriter writer = new XmlPaymentPdfWriter(tempDir, 2)) {
            writer.write(paymentDocument("ИНПР0001"));
            writer.write(paymentDocument("ИНПР0002"));
            writer.write(paymentDocument("ИНПР0003"));
        }

        assertTrue(Files.exists(tempDir.resolve("payment-documents-001.pdf")));
        assertTrue(Files.exists(tempDir.resolve("payment-documents-002.pdf")));
        assertEquals(2, Files.list(tempDir).filter(path -> path.toString().endsWith(".pdf")).count());
    }
    @Test
    void shouldPreferPremiseAddressFromCombinedAddress() throws Exception {
        try (XmlPaymentPdfWriter writer = new XmlPaymentPdfWriter(tempDir)) {
            String sourceAddress = "Адрес доставки: 664007, обл Иркутская, г. Иркутск, ул. Ямская, дом № 51, 56 "
                + "(адрес помещения: 664000, обл Иркутская, г. Иркутск, тер. СНТ. Восход, . , кв.28)";

            assertEquals(
                "Адрес помещения: 664000, обл Иркутская, г. Иркутск, тер. СНТ. Восход, кв.28",
                writer.formatDeliveryAddress(sourceAddress)
            );
        }
    }

    @Test
    void shouldKeepDeliveryAddressWhenPremiseAddressIsMissing() throws Exception {
        try (XmlPaymentPdfWriter writer = new XmlPaymentPdfWriter(tempDir)) {
            String sourceAddress = "Адрес доставки: 664009, обл Иркутская, г. Иркутск, проезд Космический, дом № 5";

            assertEquals(
                "Адрес доставки: 664009, обл Иркутская, г. Иркутск, проезд Космический, дом № 5",
                writer.formatDeliveryAddress(sourceAddress)
            );
        }
    }

    @Test
    void shouldAddDeliveryPrefixForPlainAddress() throws Exception {
        try (XmlPaymentPdfWriter writer = new XmlPaymentPdfWriter(tempDir)) {
            String sourceAddress = "664009, обл Иркутская, г. Иркутск, ул. Ленина, дом № 1";

            assertEquals(
                "Адрес доставки: 664009, обл Иркутская, г. Иркутск, ул. Ленина, дом № 1",
                writer.formatDeliveryAddress(sourceAddress)
            );
        }
    }

    private PaymentDocument paymentDocument(String account) {
        PaymentDocument document = new PaymentDocument();

        document.setAccount(account);
        document.setPayerName("Иванов Иван Иванович");
        document.setAddress("Адрес помещения: 664000, обл Иркутская, г. Иркутск, ул. Ленина, дом № 1, кв.2");
        document.setPeriod("Май 2026");
        document.setPaymentDueDate("10.06.2026");
        document.setAgent("ООО Агент");
        document.setDebtAmount("0.00");
        document.setChargeAmount("716.40");
        document.setTotalAmount("716.40");
        document.addRecipient(new PaymentRecipient("ООО Иркутскэнергосбыт", account, "Получатель платежа", "716.40"));
        document.addCharge(new PaymentCharge("Электроэнергия", "кВт.ч", "398.00", "1.80", "716.40", "716.40"));
        document.addMeter(new PaymentMeter(
            "Электросчетчик",
            "кВт.ч",
            "1873748",
            "14580.24",
            "14978",
            "397.76",
            "ОДПУ-1",
            "28889",
            "29002",
            "4520",
            "0",
            "0",
            "151",
            "-",
            "0"
        ));
        document.addQrCode(new PaymentQrCode("ST00012|Name=ООО Иркутскэнергосбыт|Sum=71640", "Для оплаты за электроэнергию"));

        return document;
    }
}
