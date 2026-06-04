package org.vc.xml;

import org.vc.payment.PaymentCharge;
import org.vc.payment.PaymentDocument;
import org.vc.payment.PaymentMeter;
import org.vc.payment.PaymentQrCode;
import org.vc.payment.PaymentRecipient;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Потоково читает XML с платёжными документами.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class XmlPaymentDocumentParser {

    private String currentExecutorName = "";
    private String currentExecutorAddress = "";
    private String currentExecutorInn = "";
    private String currentExecutorKpp = "";

    /**
     * Читает все узлы {@code Kvitanc} и передаёт каждый документ обработчику.
     */
    public int parse(Path xmlFile, PaymentDocumentConsumer consumer) throws IOException, XMLStreamException {
        XMLInputFactory factory = createInputFactory();
        int count = 0;

        try (InputStream inputStream = Files.newInputStream(xmlFile)) {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream, "UTF-8");

            try {
                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event == XMLStreamConstants.START_ELEMENT
                        && "Kvitanc".equals(reader.getLocalName())) {
                        consumer.accept(readPaymentDocument(reader));
                        count++;
                    }
                }
            } finally {
                reader.close();
            }
        }

        return count;
    }

    private XMLInputFactory createInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();

        setPropertyIfSupported(factory, XMLInputFactory.SUPPORT_DTD, false);
        setPropertyIfSupported(factory, "javax.xml.stream.isSupportingExternalEntities", false);

        return factory;
    }

    private void setPropertyIfSupported(XMLInputFactory factory, String propertyName, Object value) {
        if (factory.isPropertySupported(propertyName)) {
            factory.setProperty(propertyName, value);
        }
    }

    private PaymentDocument readPaymentDocument(XMLStreamReader reader) throws XMLStreamException {
        PaymentDocument paymentDocument = new PaymentDocument();
        resetCurrentExecutor();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                readElement(reader, paymentDocument);
            }

            if (event == XMLStreamConstants.END_ELEMENT
                && "Kvitanc".equals(reader.getLocalName())) {
                return paymentDocument;
            }
        }

        return paymentDocument;
    }

    private void readElement(XMLStreamReader reader, PaymentDocument paymentDocument) {
        switch (reader.getLocalName()) {
            case "LS" -> readMainInfo(reader, paymentDocument);
            case "Ispolnitel" -> readExecutor(reader);
            case "Poluchatel" -> paymentDocument.addRecipient(readRecipient(reader));
            case "StrNach" -> paymentDocument.addCharge(readCharge(reader));
            case "StrPrib" -> paymentDocument.addMeter(readMeter(reader));
            case "StrOPL" -> paymentDocument.setPaidAmount(attribute(reader, "SUMOPL"));
            case "Izveshenie" -> readSummary(reader, paymentDocument);
            default -> {
                // остальные теги для краткой PDF-формы не нужны
            }
        }
    }

    private void readMainInfo(XMLStreamReader reader, PaymentDocument paymentDocument) {
        paymentDocument.setAccount(attribute(reader, "ACCOUNT"));
        paymentDocument.setPayerName(attribute(reader, "FIO"));
        paymentDocument.setAddress(attribute(reader, "ADRESSF"));
        paymentDocument.setPostalIndex(attribute(reader, "PochtIndex"));
        paymentDocument.setCityType(attribute(reader, "VIDCITY"));
        paymentDocument.setCity(attribute(reader, "NASPUNKT"));
        paymentDocument.setStreetType(attribute(reader, "VIDSTREET"));
        paymentDocument.setStreet(attribute(reader, "ULICA"));
        paymentDocument.setHouse(attribute(reader, "DOM"));
        paymentDocument.setCorpus(attribute(reader, "KORPUS"));
        paymentDocument.setFlat(attribute(reader, "FLAT"));
        paymentDocument.setPeriod(attribute(reader, "DATEPR"));
        paymentDocument.setPaymentDueDate(attribute(reader, "DATESO"));
        paymentDocument.setAgent(attribute(reader, "AGENT"));
        paymentDocument.setAgentAddress(attribute(reader, "ADRESAG"));
        paymentDocument.setAgentPhone(attribute(reader, "TELAG"));
        paymentDocument.setResidentsCount(attribute(reader, "KOLZH"));
        paymentDocument.setRoomsCount(attribute(reader, "KOLK"));
        paymentDocument.setFlatArea(attribute(reader, "PLOSHFL"));
        paymentDocument.setHouseArea(attribute(reader, "PLOSHDOM"));
        paymentDocument.setCommonElectricArea(attribute(reader, "PLOSHMOP_EE"));
        paymentDocument.setCommonHotWaterArea(attribute(reader, "PLOSHMOP_GVS"));
        paymentDocument.setNonResidentialArea(attribute(reader, "PLOSHNJ"));
        paymentDocument.setElectricCommonShare(attribute(reader, "PLOSHDOL_EE"));
        paymentDocument.setHotWaterCommonShare(attribute(reader, "PLOSHDOL_GVS"));
        paymentDocument.setFirstQrDescription(attribute(reader, "QR1Desc"));
        paymentDocument.setSecondQrDescription(attribute(reader, "QR2Desc"));
        readQrCodes(reader, paymentDocument);
    }

    private void readQrCodes(XMLStreamReader reader, PaymentDocument paymentDocument) {
        for (int index = 1; index <= 8; index++) {
            paymentDocument.addQrCode(new PaymentQrCode(
                normalizeQrText(attribute(reader, "QR" + index + "Text")),
                attribute(reader, "QR" + index + "Desc")
            ));
        }
    }

    private String normalizeQrText(String qrText) {
        return qrText
            .replace("^brvbar;", "|")
            .replace("&brvbar;", "|")
            .replace("?", "|");
    }

    private PaymentRecipient readRecipient(XMLStreamReader reader) {
        return new PaymentRecipient(
            attribute(reader, "NAMEISP"),
            attribute(reader, "ACCOUNT"),
            attribute(reader, "NAMEPOL"),
            attribute(reader, "SUMISP")
        );
    }

    private PaymentCharge readCharge(XMLStreamReader reader) {
        return new PaymentCharge(
            attribute(reader, "USLUGA"),
            attribute(reader, "EDIZM"),
            firstNotBlank(attribute(reader, "KOLIND"), attribute(reader, "KOLODN")),
            attribute(reader, "KOLODN"),
            attribute(reader, "TARIF"),
            attribute(reader, "SUMIND"),
            attribute(reader, "SUMODN"),
            attribute(reader, "SUMNCH"),
            attribute(reader, "KODOSN"),
            attribute(reader, "OSNKOR"),
            firstNotBlank(attribute(reader, "SUMKOR"), attribute(reader, "SUMPK")),
            attribute(reader, "SUMOST"),
            firstNotBlank(attribute(reader, "SUMITOGWITHOPLAT"), attribute(reader, "SUMITOG")),
            currentExecutorName,
            currentExecutorAddress,
            currentExecutorInn,
            currentExecutorKpp
        );
    }

    private void readExecutor(XMLStreamReader reader) {
        currentExecutorName = attribute(reader, "NAMEISP");
        currentExecutorAddress = attribute(reader, "Adres");
        currentExecutorInn = attribute(reader, "INN");
        currentExecutorKpp = attribute(reader, "KPP");
    }

    private void resetCurrentExecutor() {
        currentExecutorName = "";
        currentExecutorAddress = "";
        currentExecutorInn = "";
        currentExecutorKpp = "";
    }

    private PaymentMeter readMeter(XMLStreamReader reader) {
        return new PaymentMeter(
            attribute(reader, "USLUGA"),
            attribute(reader, "EDIZM"),
            attribute(reader, "NPUIND"),
            attribute(reader, "PREDPIND"),
            attribute(reader, "TEKPIND"),
            attribute(reader, "RASIND"),
            attribute(reader, "NPUOD"),
            attribute(reader, "PREDPOD"),
            attribute(reader, "TEKPOD"),
            attribute(reader, "RASOD"),
            attribute(reader, "SUMRASFL"),
            attribute(reader, "SUMRASUL"),
            attribute(reader, "NORMIND"),
            attribute(reader, "NINDQUOT"),
            attribute(reader, "NORMOD")
        );
    }

    private void readSummary(XMLStreamReader reader, PaymentDocument paymentDocument) {
        paymentDocument.setDebtAmount(attribute(reader, "ITOGOST"));
        paymentDocument.setChargeAmount(attribute(reader, "SUMRASP"));
        paymentDocument.setTotalAmount(attribute(reader, "SUMKON"));
    }

    private String firstNotBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String attribute(XMLStreamReader reader, String name) {
        String value = reader.getAttributeValue(null, name);

        return value == null ? "" : value;
    }
}
