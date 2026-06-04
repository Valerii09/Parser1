package org.vc.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vc.payment.PaymentDocument;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmlPaymentDocumentParserTest {

    @TempDir
    private Path tempDir;

    private final XmlPaymentDocumentParser parser = new XmlPaymentDocumentParser();

    @Test
    void shouldParsePaymentDocumentsFromXml() throws IOException, XMLStreamException {
        Path xmlFile = tempDir.resolve("payments.xml");

        Files.writeString(xmlFile, """
            <asrn>
              <Kvitanc>
                <LS ACCOUNT="ИНПР0001" FIO="Иванов Иван Иванович" ADRESSF="Адрес доставки: 664000 (адрес помещения: 664000, обл Иркутская, г. Иркутск, ул. Ленина, дом № 1, кв.2)" DATEPR="Май 2026" DATESO="10.06.2026" AGENT="ООО Агент" QR1Text="ST00012^brvbar;Name=ООО Иркутскэнергосбыт|Sum=71640" QR1Desc="Для оплаты за электроэнергию"/>
                <ISP>
                  <Poluchatel ACCOUNT="ИНПР0001" NAMEISP="ООО Иркутскэнергосбыт" NAMEPOL="Получатель платежа" SUMISP="716.40"/>
                </ISP>
                <Nach>
                  <Ispolnitel>
                    <StrNach USLUGA="Электроэнергия" EDIZM="кВт.ч" KOLIND="398.00" TARIF="1.80" SUMNCH="716.40" KODOSN="9" OSNKOR="Начисление пени" SUMKOR="3.28" SUMITOGWITHOPLAT="716.40"/>
                  </Ispolnitel>
                </Nach>
                <PRIB>
                  <StrPrib USLUGA="Электросчетчик" EDIZM="кВт.ч" NPUIND="1873748" PREDPIND="14580.24" TEKPIND="14978" RASIND="397.76" NPUOD="ОДПУ-1" PREDPOD="28889" TEKPOD="29002" RASOD="4520" SUMRASFL="0" SUMRASUL="0" NORMIND="151" NINDQUOT="-" NORMOD="0"/>
                </PRIB>
                <Izveshenie ITOGOST="2419.20" SUMRASP="716.40" SUMKON="716.40"/>
              </Kvitanc>
            </asrn>
            """, StandardCharsets.UTF_8);

        List<PaymentDocument> documents = new ArrayList<>();

        int count = parser.parse(xmlFile, documents::add);

        assertEquals(1, count);
        assertEquals(1, documents.size());

        PaymentDocument document = documents.get(0);

        assertEquals("ИНПР0001", document.getAccount());
        assertEquals("Иванов Иван Иванович", document.getPayerName());
        assertEquals("664000, обл Иркутская, г. Иркутск, ул. Ленина, дом № 1, кв.2", document.getAddress());
        assertEquals("Май 2026", document.getPeriod());
        assertEquals("10.06.2026", document.getPaymentDueDate());
        assertEquals("716.40", document.getTotalAmount());
        assertEquals(1, document.getRecipients().size());
        assertEquals(1, document.getCharges().size());
        assertEquals(1, document.getMeters().size());
        assertEquals("9", document.getCharges().get(0).getRecalculationCode());
        assertEquals("Начисление пени", document.getCharges().get(0).getRecalculationReason());
        assertEquals("3.28", document.getCharges().get(0).getRecalculationAmount());
        assertEquals(1, document.getQrCodes().size());
        assertEquals("ST00012|Name=ООО Иркутскэнергосбыт|Sum=71640", document.getQrCodes().get(0).getText());
        assertEquals("Для оплаты за электроэнергию", document.getQrCodes().get(0).getDescription());
        assertEquals("ОДПУ-1", document.getMeters().get(0).getCommunalMeterNumber());
        assertEquals("28889", document.getMeters().get(0).getCommunalPreviousValue());
        assertEquals("151", document.getMeters().get(0).getIndividualNorm());
    }
}
