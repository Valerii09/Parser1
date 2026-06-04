package org.vc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlPaymentPdfGenerationServiceTest {

    @TempDir
    private Path tempDir;

    private final XmlPaymentPdfGenerationService service = new XmlPaymentPdfGenerationService();

    @Test
    void shouldGeneratePdfForEveryXmlFileInFolder() throws Exception {
        Path xmlFolder = tempDir.resolve("xml");
        Path outputFolder = tempDir.resolve("result");
        Files.createDirectories(xmlFolder);

        Files.writeString(xmlFolder.resolve("first.xml"), xmlWithAccount("ИНПР0001"), StandardCharsets.UTF_8);
        Files.writeString(xmlFolder.resolve("second.xml"), xmlWithAccount("ИНПР0002"), StandardCharsets.UTF_8);

        int generatedDocuments = service.generateFromFolder(xmlFolder, outputFolder, 1000);

        assertEquals(2, generatedDocuments);
        assertTrue(Files.exists(outputFolder.resolve("first").resolve("payment-documents-001.pdf")));
        assertTrue(Files.exists(outputFolder.resolve("second").resolve("payment-documents-001.pdf")));
    }

    private String xmlWithAccount(String account) {
        return """
            <asrn>
              <Kvitanc>
                <LS ACCOUNT="%s" FIO="Иванов Иван Иванович" ADRESSF="Адрес помещения: 664000, обл Иркутская, г. Иркутск, ул. Ленина, дом № 1, кв.2" DATEPR="Май 2026" DATESO="10.06.2026" AGENT="ООО Агент"/>
                <ISP>
                  <Poluchatel ACCOUNT="%s" NAMEISP="ООО Иркутскэнергосбыт" NAMEPOL="Получатель платежа" SUMISP="716.40"/>
                </ISP>
                <Nach>
                  <Ispolnitel>
                    <StrNach USLUGA="Электроэнергия" EDIZM="кВт.ч" KOLIND="398.00" TARIF="1.80" SUMNCH="716.40" SUMITOGWITHOPLAT="716.40"/>
                    <StrOPL SUMOPL="0.00"/>
                  </Ispolnitel>
                </Nach>
                <PRIB/>
                <Izveshenie ITOGOST="0.00" SUMRASP="716.40" SUMKON="716.40"/>
              </Kvitanc>
            </asrn>
            """.formatted(account, account);
    }
}
