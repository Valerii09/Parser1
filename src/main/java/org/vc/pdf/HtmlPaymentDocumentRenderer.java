package org.vc.pdf;

import org.vc.payment.PaymentCharge;
import org.vc.payment.PaymentDocument;
import org.vc.payment.PaymentMeter;
import org.vc.payment.PaymentQrCode;
import org.vc.payment.PaymentRecipient;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Собирает HTML-фрагмент одной платёжки из доменной модели.
 * Java здесь отвечает за данные и повторяющиеся строки таблиц, а внешний вид задаётся HTML/CSS-шаблоном.
 */
final class HtmlPaymentDocumentRenderer {

    private final HtmlTemplate template;

    HtmlPaymentDocumentRenderer() throws IOException {
        this.template = HtmlTemplate.fromResource("/templates/payment/payment-document.html");
    }

    String render(PaymentDocument document) throws IOException {
        return template.render(Map.ofEntries(
            Map.entry("densityClass", densityClass(document)),
            Map.entry("period", HtmlEscaper.text(document.getPeriod())),
            Map.entry("deliveryAddress", HtmlEscaper.text(PaymentAddressPrintFormatter.format(document.getSourceAddress()))),
            Map.entry("payerName", HtmlEscaper.text(document.getPayerName())),
            Map.entry("qrCodes", qrCodes(document.getQrCodes())),
            Map.entry("recipientRows", recipientRows(document.getRecipients())),
            Map.entry("chargeRows", chargeRows(document.getCharges())),
            Map.entry("recalculationLegend", recalculationLegend(document.getCharges())),
            Map.entry("meterRows", meterRows(document.getMeters())),
            Map.entry("residentsCount", HtmlEscaper.textOrDash(document.getResidentsCount())),
            Map.entry("roomsCount", HtmlEscaper.textOrDash(document.getRoomsCount())),
            Map.entry("flatArea", HtmlEscaper.textOrDash(document.getFlatArea())),
            Map.entry("houseArea", HtmlEscaper.textOrDash(document.getHouseArea())),
            Map.entry("commonAreas", slash(document.getCommonElectricArea(), document.getCommonHotWaterArea())),
            Map.entry("commonShares", slash(document.getElectricCommonShare(), document.getHotWaterCommonShare())),
            Map.entry("referenceText", referenceText(document))
        ));
    }

    private String densityClass(PaymentDocument document) {
        int rows = document.getRecipients().size() * 2 + document.getCharges().size() + document.getMeters().size();

        return rows > 12 ? "dense" : "";
    }

    private String qrCodes(List<PaymentQrCode> qrCodes) throws IOException {
        StringBuilder result = new StringBuilder();
        int count = Math.min(qrCodes.size(), 2);

        for (int index = 0; index < count; index++) {
            PaymentQrCode qrCode = qrCodes.get(index);
            String description = HtmlEscaper.clean(qrCode.getDescription());
            String caption = HtmlEscaper.text(description.isBlank()
                ? "Для оплаты по QR-коду"
                : description);

            result.append("""
                <span class="qr-item">
                    <span class="qr-caption">%s</span>
                    <img class="qr-image" src="%s" alt="QR-код оплаты"/>
                </span>
                """.formatted(caption, PaymentQrCodeImageGenerator.toDataUri(qrCode)));
        }

        return result.toString();
    }

    private String recipientRows(List<PaymentRecipient> recipients) {
        StringBuilder result = new StringBuilder();

        for (PaymentRecipient recipient : recipients) {
            result.append("""
                <tr>
                    <td class="recipient-name">%s</td>
                    <td class="recipient-details">
                        <div class="recipient-account">Лицевой счет: %s</div>
                        <div>%s</div>
                    </td>
                    <td>%s</td>
                </tr>
                """.formatted(
                    HtmlEscaper.text(recipient.getName()),
                    HtmlEscaper.textOrDash(recipient.getAccount()),
                    HtmlEscaper.text(recipient.getDescription()),
                    HtmlEscaper.amount(recipient.getAmount())
                ));
        }

        return result.toString();
    }

    private String chargeRows(List<PaymentCharge> charges) {
        StringBuilder result = new StringBuilder();
        Map<String, List<PaymentCharge>> groups = groupCharges(charges);

        for (Map.Entry<String, List<PaymentCharge>> entry : groups.entrySet()) {
            result.append("<tr class=\"executor-row\"><td colspan=\"13\">")
                .append(HtmlEscaper.text(entry.getKey()))
                .append("</td></tr>");

            for (PaymentCharge charge : entry.getValue()) {
                result.append(chargeRow(charge));
            }

            result.append(totalRow(entry.getValue()));
            result.append("<tr class=\"paid-row\"><td colspan=\"12\">Оплачено в расчетном периоде</td><td>0.00</td></tr>");
        }

        return result.toString();
    }

    private String chargeRow(PaymentCharge charge) {
        return """
            <tr>
                <td class="service-name">%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>0.00</td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
            </tr>
            """.formatted(
            HtmlEscaper.text(charge.getServiceName()),
            HtmlEscaper.amount(charge.getDebtAmount()),
            HtmlEscaper.textOrDash(charge.getUnit()),
            HtmlEscaper.textOrZero(charge.getVolume()),
            HtmlEscaper.textOrZero(charge.getCommunalVolume()),
            HtmlEscaper.textOrDash(charge.getTariff()),
            HtmlEscaper.amount(charge.getIndividualAmount()),
            HtmlEscaper.amount(charge.getCommunalAmount()),
            HtmlEscaper.amount(charge.getChargedAmount()),
            HtmlEscaper.textOrDash(charge.getRecalculationCode()),
            HtmlEscaper.amount(charge.getRecalculationAmount()),
            HtmlEscaper.amount(charge.getTotalAmount())
        );
    }

    private String totalRow(List<PaymentCharge> charges) {
        return """
            <tr>
                <td>Итого по исполнителю</td>
                <td>%s</td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td>%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>0.00</td>
                <td></td>
                <td>%s</td>
                <td>%s</td>
            </tr>
            """.formatted(
            amount(sum(charges, PaymentCharge::getDebtAmount)),
            amount(sum(charges, PaymentCharge::getIndividualAmount)),
            amount(sum(charges, PaymentCharge::getCommunalAmount)),
            amount(sum(charges, PaymentCharge::getChargedAmount)),
            amount(sum(charges, PaymentCharge::getRecalculationAmount)),
            amount(sum(charges, PaymentCharge::getTotalAmount))
        );
    }

    private String recalculationLegend(List<PaymentCharge> charges) {
        Map<String, String> legend = new LinkedHashMap<>();

        for (PaymentCharge charge : charges) {
            String code = HtmlEscaper.clean(charge.getRecalculationCode());
            String reason = HtmlEscaper.clean(charge.getRecalculationReason());

            if (!code.isBlank() && !reason.isBlank()) {
                legend.put(code, reason);
            }
        }

        if (legend.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder("<div class=\"recalculation-legend\">Расшифровка кодов основания перерасчетов: ");
        boolean first = true;

        for (Map.Entry<String, String> entry : legend.entrySet()) {
            if (!first) {
                result.append("; ");
            }

            result.append(HtmlEscaper.text(entry.getKey()))
                .append(" - ")
                .append(HtmlEscaper.text(entry.getValue()));
            first = false;
        }

        return result.append("</div>").toString();
    }

    private String meterRows(List<PaymentMeter> meters) {
        StringBuilder result = new StringBuilder();

        for (PaymentMeter meter : meters) {
            result.append("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td class="meter-number">%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td class="meter-number">%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """.formatted(
                HtmlEscaper.text(meter.getServiceName()),
                HtmlEscaper.textOrDash(meter.getUnit()),
                HtmlEscaper.textOrDash(meter.getMeterNumber()) + "<br/>" + HtmlEscaper.textOrDash(meter.getPreviousValue()),
                HtmlEscaper.textOrDash(meter.getCurrentValue()),
                HtmlEscaper.textOrDash(meter.getConsumption()),
                HtmlEscaper.textOrDash(meter.getCommunalMeterNumber()) + "<br/>" + HtmlEscaper.textOrDash(meter.getCommunalPreviousValue()),
                HtmlEscaper.textOrDash(meter.getCommunalCurrentValue()),
                HtmlEscaper.textOrDash(meter.getCommunalConsumption()),
                HtmlEscaper.textOrZero(meter.getHousePremiseVolume()),
                HtmlEscaper.textOrZero(meter.getHouseCommonVolume()),
                HtmlEscaper.textOrDash(meter.getIndividualNorm()),
                HtmlEscaper.textOrDash(meter.getIncreasingRatio()),
                HtmlEscaper.textOrZero(meter.getCommunalNorm())
            ));
        }

        return result.toString();
    }

    private String referenceText(PaymentDocument document) {
        StringBuilder result = new StringBuilder();

        appendRaw(result, "По вопросам обращаться: ", document.getAgent(), ". ");
        appendRaw(result, "Адрес: ", document.getAgentAddress(), ". ");
        appendRaw(result, "Телефон: ", document.getAgentPhone(), ".");

        return HtmlEscaper.text(result.toString());
    }

    private Map<String, List<PaymentCharge>> groupCharges(List<PaymentCharge> charges) {
        Map<String, List<PaymentCharge>> result = new LinkedHashMap<>();

        for (PaymentCharge charge : charges) {
            result.computeIfAbsent(executorInfo(charge), key -> new java.util.ArrayList<>()).add(charge);
        }

        return result;
    }

    private String executorInfo(PaymentCharge charge) {
        StringBuilder result = new StringBuilder(HtmlEscaper.clean(charge.getExecutorName()));

        appendRaw(result, ", Адрес: ", charge.getExecutorAddress(), "");
        appendRaw(result, ", ИНН: ", charge.getExecutorInn(), "");
        appendRaw(result, ", КПП: ", charge.getExecutorKpp(), "");

        if (result.isEmpty()) {
            return "Исполнитель услуг";
        }

        return result.toString();
    }

    private void appendRaw(StringBuilder result, String prefix, String value, String suffix) {
        String preparedValue = HtmlEscaper.clean(value);

        if (preparedValue.isBlank()) {
            return;
        }

        result.append(prefix).append(preparedValue).append(suffix);
    }

    private String slash(String first, String second) {
        return HtmlEscaper.textOrZero(first) + "/" + HtmlEscaper.textOrZero(second);
    }

    private double sum(List<PaymentCharge> charges, java.util.function.Function<PaymentCharge, String> extractor) {
        return charges.stream()
            .map(extractor)
            .mapToDouble(this::parseAmount)
            .sum();
    }

    private double parseAmount(String value) {
        String preparedValue = HtmlEscaper.clean(value)
            .replace(',', '.')
            .replace(" ", "");

        if (preparedValue.isBlank() || "-".equals(preparedValue)) {
            return 0;
        }

        try {
            return Double.parseDouble(preparedValue);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String amount(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
