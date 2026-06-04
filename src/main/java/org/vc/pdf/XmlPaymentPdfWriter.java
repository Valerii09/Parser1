package org.vc.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;
import org.vc.payment.PaymentCharge;
import org.vc.payment.PaymentDocument;
import org.vc.payment.PaymentMeter;
import org.vc.payment.PaymentQrCode;
import org.vc.payment.PaymentRecipient;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Формирует PDF-файлы с платёжными документами из XML в печатном макете поставщика.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class XmlPaymentPdfWriter implements Closeable {

    private static final int DEFAULT_MAX_PAGES_PER_FILE = 1000;
    private static final String RESULT_FILE_PREFIX = "payment-documents";
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 31;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
    private static final float SMALL_FONT_SIZE = 3.65f;
    private static final float BODY_FONT_SIZE = 5.6f;
    private static final float HEADER_FONT_SIZE = 6.1f;
    private static final float METER_NUMBER_FONT_SIZE = 2.8f;
    private static final float TITLE_FONT_SIZE = 7.2f;
    private static final float QR_SIZE = 90;
    private static final Color HEADER_GRAY = new Color(218, 218, 218);
    private static final Color SECTION_GRAY = new Color(191, 191, 191);
    private static final float TABLE_LINE_WIDTH = 0.48f;
    private static final float DELIVERY_ADDRESS_FONT_SIZE = 6.8f;
    private static final float DELIVERY_ADDRESS_LINE_HEIGHT = 8.2f;
    private static final int DELIVERY_ADDRESS_MAX_LINES = 3;

    private final Path outputFolder;
    private final int maxPagesPerFile;

    private PDDocument document;
    private PDType0Font font;
    private PDType0Font boldFont;
    private int currentFilePages;
    private int fileNumber;
    private int writtenDocuments;

    public XmlPaymentPdfWriter(Path outputFolder) throws IOException {
        this(outputFolder, DEFAULT_MAX_PAGES_PER_FILE);
    }

    public XmlPaymentPdfWriter(Path outputFolder, int maxPagesPerFile) throws IOException {
        this.outputFolder = outputFolder;
        this.maxPagesPerFile = maxPagesPerFile;

        Files.createDirectories(outputFolder);
    }

    /**
     * Добавляет одну платёжку в PDF.
     */
    public void write(PaymentDocument paymentDocument) throws IOException {
        ensureDocument();

        addPaymentDocumentPage(paymentDocument);
        writtenDocuments++;

        if (currentFilePages >= maxPagesPerFile) {
            saveCurrentDocument();
        }
    }

    public int getWrittenDocuments() {
        return writtenDocuments;
    }

    public int getCreatedFilesCount() {
        return fileNumber + (document == null || currentFilePages == 0 ? 0 : 1);
    }

    @Override
    public void close() throws IOException {
        saveCurrentDocument();
    }

    private void ensureDocument() throws IOException {
        if (document != null) {
            return;
        }

        document = new PDDocument();
        font = PDType0Font.load(document, resolveFontPath("arial.ttf").toFile());
        boldFont = PDType0Font.load(document, resolveFontPath("arialbd.ttf").toFile());
        currentFilePages = 0;
    }

    private void addPaymentDocumentPage(PaymentDocument paymentDocument) throws IOException {
        PDPage page = addPdfPage();

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float y = drawPageBeforeMeters(contentStream, paymentDocument);
            drawMetersAndBottom(contentStream, paymentDocument, y - 6);
        }
    }

    private PDPage addPdfPage() {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        currentFilePages++;
        return page;
    }

    private float drawPageBeforeMeters(PDPageContentStream contentStream, PaymentDocument paymentDocument) throws IOException {
        drawHeader(contentStream, paymentDocument);

        float y = PAGE_HEIGHT - 144;
        drawConsumer(contentStream, paymentDocument, y);
        y -= 10;

        y = drawRecipientsBlock(contentStream, paymentDocument, y);
        y -= 12;

        return drawChargesTable(contentStream, paymentDocument, y);
    }

    private void drawMetersAndBottom(PDPageContentStream contentStream, PaymentDocument paymentDocument, float y) throws IOException {
        y = drawMetersTable(contentStream, paymentDocument, y);
        y -= 6;
        drawBottomReference(contentStream, paymentDocument, y);
    }

    private boolean shouldMoveMetersToNextPage(float y, PaymentDocument paymentDocument) {
        return y - metersTableHeight(paymentDocument) < 30;
    }

    private float metersTableHeight(PaymentDocument paymentDocument) {
        return 5 + 36 + paymentDocument.getMeters().size() * 20F + 30;
    }

    private void drawHeader(PDPageContentStream contentStream, PaymentDocument paymentDocument) throws IOException {
        float topY = PAGE_HEIGHT - 26;

        drawText(contentStream, "Платежный документ", MARGIN, topY, TITLE_FONT_SIZE, true);
        drawText(contentStream, paymentDocument.getPeriod(), MARGIN, topY - 9, TITLE_FONT_SIZE, true);
        drawWrappedText(
            contentStream,
            formatDeliveryAddress(paymentDocument.getSourceAddress()),
            165,
            topY,
            410,
            DELIVERY_ADDRESS_FONT_SIZE,
            DELIVERY_ADDRESS_MAX_LINES,
            DELIVERY_ADDRESS_LINE_HEIGHT,
            true
        );
        float qrY = PAGE_HEIGHT - 135;
        List<PaymentQrCode> qrCodes = paymentDocument.getQrCodes();

        for (int index = 0; index < Math.min(qrCodes.size(), 2); index++) {
            float qrX = 63 + index * 126;
            drawQrCode(contentStream, qrCodes.get(index), qrX, qrY, QR_SIZE);
            drawRotatedWrappedText(contentStream, caption(qrCodes.get(index), index), qrX - 7, qrY + 5, QR_SIZE - 6, 3.7f, 2);
        }

        String paymentHint = "Оплата по QR-коду — это быстро, удобно, надежно!";
        drawText(
            contentStream,
            paymentHint,
            MARGIN + CONTENT_WIDTH - textWidth(paymentHint, boldFont, HEADER_FONT_SIZE),
            PAGE_HEIGHT - 157,
            HEADER_FONT_SIZE,
            true
        );
    }

    private String caption(PaymentQrCode qrCode, int index) {
        String description = clean(qrCode.getDescription());

        if (!description.isBlank()) {
            return description;
        }

        return index == 0 ? "Для оплаты за электроэнергию" : "Для оплаты за Отопление и ГВС";
    }

    private void drawConsumer(PDPageContentStream contentStream, PaymentDocument paymentDocument, float y) throws IOException {
        drawText(contentStream, "Потребитель:", MARGIN, y, HEADER_FONT_SIZE, true);
        drawText(contentStream, paymentDocument.getPayerName(), MARGIN + 92, y, BODY_FONT_SIZE, false);
    }

    private float drawRecipientsBlock(PDPageContentStream contentStream, PaymentDocument paymentDocument, float y) throws IOException {
        drawText(contentStream, "Информация для внесения платы получателю платежа (получателям платежей)", MARGIN, y, HEADER_FONT_SIZE, true);

        float tableTop = y - 4;
        float[] widths = new float[] {110, 340, CONTENT_WIDTH - 450};
        float headerHeight = 7;

        drawTableCell(contentStream, MARGIN, tableTop, widths[0], headerHeight, "Исполнитель услуг", SMALL_FONT_SIZE, true, false, 1);
        drawTableCell(contentStream, MARGIN + widths[0], tableTop, widths[1], headerHeight, "Реквизиты Получателя для зачисления платежей", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, MARGIN + widths[0] + widths[1], tableTop, widths[2], headerHeight, "Итого к оплате, руб.", SMALL_FONT_SIZE, true, true, 1);

        float rowY = tableTop - headerHeight;

        for (PaymentRecipient recipient : paymentDocument.getRecipients()) {
            float rowHeight = isPenaltyRecipient(recipient) ? 11 : 19;
            drawRecipientRow(contentStream, rowY, widths, rowHeight, recipient);
            rowY -= rowHeight;
        }

        return rowY;
    }

    private void drawRecipientRow(
        PDPageContentStream contentStream,
        float y,
        float[] widths,
        float rowHeight,
        PaymentRecipient recipient
    ) throws IOException {
        float x = MARGIN;

        drawTableCellStyled(contentStream, x, y, widths[0], rowHeight, recipient.getName(), SMALL_FONT_SIZE, null, true, false, 2);
        x += widths[0];

        if (isPenaltyRecipient(recipient)) {
            drawTableCell(contentStream, x, y, widths[1], rowHeight, recipient.getDescription(), SMALL_FONT_SIZE, false, false, 2);
        } else {
            drawRecipientDetailsCell(contentStream, x, y, widths[1], rowHeight, recipient);
        }

        x += widths[1];
        drawTableCell(contentStream, x, y, widths[2], rowHeight, amount(recipient.getAmount()), SMALL_FONT_SIZE, false, true, 1);
    }

    private void drawRecipientDetailsCell(
        PDPageContentStream contentStream,
        float x,
        float y,
        float width,
        float height,
        PaymentRecipient recipient
    ) throws IOException {
        drawTableCellStyled(contentStream, x, y, width, height, "", SMALL_FONT_SIZE, null, false, false, 1);
        drawText(contentStream, fit("Лицевой счет: " + valueOrDash(recipient.getAccount()), width - 4, boldFont, SMALL_FONT_SIZE), x + 2, y - 5.6f, SMALL_FONT_SIZE, true);
        drawWrappedText(contentStream, recipient.getDescription(), x + 2, y - 11, width - 4, SMALL_FONT_SIZE, 2, 5.1f, false);
    }

    private boolean isPenaltyRecipient(PaymentRecipient recipient) {
        return clean(recipient.getName()).toLowerCase(java.util.Locale.ROOT).startsWith("пени");
    }

    private float drawChargesTable(PDPageContentStream contentStream, PaymentDocument paymentDocument, float y) throws IOException {
        drawText(contentStream, "Расчет размера платы за жилищно-коммунальные услуги", MARGIN, y, HEADER_FONT_SIZE, true);

        float tableTop = y - 5;
        float[] widths = chargeTableWidths();
        float headerHeight = 26;
        float rowHeight = 7;
        drawChargeHeader(contentStream, tableTop, widths, headerHeight);

        float rowY = tableTop - headerHeight;
        Map<String, List<PaymentCharge>> groups = groupCharges(paymentDocument.getCharges());

        for (Map.Entry<String, List<PaymentCharge>> entry : groups.entrySet()) {
            PaymentCharge firstCharge = entry.getValue().get(0);
            String executorInfo = formatExecutorInfo(firstCharge);
            drawTableCellStyled(contentStream, MARGIN, rowY, CONTENT_WIDTH, rowHeight, executorInfo, SMALL_FONT_SIZE, SECTION_GRAY, true, false, 1);
            rowY -= rowHeight;

            for (PaymentCharge charge : entry.getValue()) {
                float chargeHeight = chargeRowHeight(charge, widths[0]);
                drawChargeRow(contentStream, rowY, widths, chargeHeight, charge);
                rowY -= chargeHeight;
            }

            drawChargeTotalRow(contentStream, rowY, widths, rowHeight, "Итого по исполнителю", totalAmount(entry.getValue()));
            rowY -= rowHeight;
            drawChargePaidRow(contentStream, rowY, widths, rowHeight, "Оплачено в расчетном периоде", "0.00");
            rowY -= rowHeight;
        }

        return drawRecalculationLegend(contentStream, paymentDocument.getCharges(), rowY);
    }

    private void drawChargeHeader(PDPageContentStream contentStream, float y, float[] widths, float height) throws IOException {
        float h1 = 8;
        float h2 = 9;
        float h3 = 9;
        float x = MARGIN;

        drawTableCell(contentStream, x, y, widths[0], height, "Виды услуг", SMALL_FONT_SIZE, true, true, 2);
        x += widths[0];
        drawTableCell(contentStream, x, y, widths[1], height, "долг(+)/ переплата (-) на начало", SMALL_FONT_SIZE, true, true, 3);
        x += widths[1];
        drawTableCell(contentStream, x, y, widths[2], height, "Ед. изм.", SMALL_FONT_SIZE, true, true, 2);
        x += widths[2];

        drawTableCell(contentStream, x, y, widths[3] + widths[4], h1, "Объем услуг", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, x, y - h1, widths[3], h2 + h3, "индив. потребление", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, x + widths[3], y - h1, widths[4], h2 + h3, "общедомовые нужды", SMALL_FONT_SIZE, true, true, 2);
        x += widths[3] + widths[4];

        drawTableCell(contentStream, x, y, widths[5], height, "Тариф руб/ед.изм", SMALL_FONT_SIZE, true, true, 3);
        x += widths[5];

        drawTableCell(contentStream, x, y, widths[6] + widths[7], h1, "Размер платы, руб.", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, x, y - h1, widths[6], h2 + h3, "индив. потребление", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, x + widths[6], y - h1, widths[7], h2 + h3, "общедомовые нужды", SMALL_FONT_SIZE, true, true, 2);
        x += widths[6] + widths[7];

        drawTableCell(contentStream, x, y, widths[8], height, "Начислено за расчет. период, руб.", SMALL_FONT_SIZE, true, true, 3);
        x += widths[8];
        drawTableCell(contentStream, x, y, widths[9], height, "Размер превышения платы по повышающему коэффициенту, руб.", SMALL_FONT_SIZE, true, true, 4);
        x += widths[9];

        drawTableCell(contentStream, x, y, widths[10] + widths[11], h1, "Перерасчеты", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, x, y - h1, widths[10], h2 + h3, "Код основания", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, x + widths[10], y - h1, widths[11], h2 + h3, "Сумма, руб. (доначисл.,+ снятия -)", SMALL_FONT_SIZE, true, true, 3);
        x += widths[10] + widths[11];

        drawTableCell(contentStream, x, y, widths[12], height, "Итого начислено в расч. периоде, руб.", SMALL_FONT_SIZE, true, true, 4);
    }

    private void drawChargeRow(PDPageContentStream contentStream, float y, float[] widths, float rowHeight, PaymentCharge charge) throws IOException {
        List<String> cells = List.of(
            charge.getServiceName(),
            amount(charge.getDebtAmount()),
            charge.getUnit(),
            valueOrDash(charge.getVolume()),
            valueOrZero(charge.getCommunalVolume()),
            valueOrDash(charge.getTariff()),
            amount(firstNotBlank(charge.getIndividualAmount(), charge.getChargedAmount())),
            amount(charge.getCommunalAmount()),
            amount(charge.getChargedAmount()),
            "0.00",
            valueOrDash(charge.getRecalculationCode()),
            amount(charge.getRecalculationAmount()),
            amount(charge.getTotalAmount())
        );
        drawDataCells(contentStream, y, widths, rowHeight, cells, index -> index == 0 ? 2 : 1);
    }

    private void drawChargeTotalRow(PDPageContentStream contentStream, float y, float[] widths, float rowHeight, String title, String amount) throws IOException {
        List<String> cells = new ArrayList<>();
        cells.add(title);
        for (int index = 1; index < widths.length - 1; index++) {
            cells.add("");
        }
        cells.add(amount(amount));
        drawCells(contentStream, y, widths, rowHeight, cells, false);
    }

    private void drawChargePaidRow(
        PDPageContentStream contentStream,
        float y,
        float[] widths,
        float rowHeight,
        String title,
        String amount
    ) throws IOException {
        float amountWidth = widths[widths.length - 1];

        drawTableCell(contentStream, MARGIN, y, CONTENT_WIDTH - amountWidth, rowHeight, title, SMALL_FONT_SIZE, false, false, 1);
        drawTableCell(contentStream, MARGIN + CONTENT_WIDTH - amountWidth, y, amountWidth, rowHeight, amount(amount), SMALL_FONT_SIZE, false, true, 1);
    }

    private float drawMetersTable(PDPageContentStream contentStream, PaymentDocument paymentDocument, float y) throws IOException {
        drawText(contentStream, "Справочная информация", MARGIN, y, HEADER_FONT_SIZE, true);

        float tableTop = y - 5;
        float[] widths = metersTableWidths();
        float x = MARGIN;
        float headerHeight = 30;
        float rowHeight = 14;

        drawTableCell(contentStream, x, tableTop, widths[0], headerHeight, "Вид прибора/услуги", SMALL_FONT_SIZE, true, true, 2);
        x += widths[0];
        drawTableCell(contentStream, x, tableTop, widths[1], headerHeight, "Ед. изм.", SMALL_FONT_SIZE, true, true, 2);
        x += widths[1];

        float meterX = x;
        drawTableCell(contentStream, meterX, tableTop, sum(widths, 2, 6), 8, "Сведения о приборах учета (ПУ)", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, meterX, tableTop - 8, sum(widths, 2, 3), 8, "индивидуальные", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, meterX + sum(widths, 2, 3), tableTop - 8, sum(widths, 5, 3), 8, "общедомовые", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, meterX, tableTop - 16, widths[2], 14, "пред. показания", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, meterX + widths[2], tableTop - 16, widths[3], 14, "текущие показания", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, meterX + widths[2] + widths[3], tableTop - 16, widths[4], 14, "расход", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, meterX + sum(widths, 2, 3), tableTop - 16, widths[5], 14, "пред. показания", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, meterX + sum(widths, 2, 4), tableTop - 16, widths[6], 14, "текущие показания", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, meterX + sum(widths, 2, 5), tableTop - 16, widths[7], 14, "расход", SMALL_FONT_SIZE, true, true, 1);
        x += sum(widths, 2, 6);

        drawTableCell(contentStream, x, tableTop, sum(widths, 8, 2), 8, "Суммарный объем в доме", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, x, tableTop - 8, widths[8], 22, "в помещениях дома", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, x + widths[8], tableTop - 8, widths[9], 22, "на общие нужды дома", SMALL_FONT_SIZE, true, true, 2);
        x += sum(widths, 8, 2);

        drawTableCell(contentStream, x, tableTop, sum(widths, 10, 3), 8, "Нормы потребления", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, x, tableTop - 8, widths[10] + widths[11], 8, "индив. потребление", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, x + widths[10] + widths[11], tableTop - 8, widths[12], 22, "общедом. нужды", SMALL_FONT_SIZE, true, true, 2);
        drawTableCell(contentStream, x, tableTop - 16, widths[10], 14, "норматив", SMALL_FONT_SIZE, true, true, 1);
        drawTableCell(contentStream, x + widths[10], tableTop - 16, widths[11], 14, "повыш коэф-т", SMALL_FONT_SIZE, true, true, 1);

        float rowY = tableTop - headerHeight;
        for (PaymentMeter meter : paymentDocument.getMeters()) {
            drawMeterRow(contentStream, rowY, widths, rowHeight, meter);
            rowY -= rowHeight;
        }

        return rowY;
    }

    private void drawMeterRow(PDPageContentStream contentStream, float y, float[] widths, float height, PaymentMeter meter) throws IOException {
        float x = MARGIN;
        float halfHeight = height / 2;

        drawTableCell(contentStream, x, y, widths[0], height, meter.getServiceName(), SMALL_FONT_SIZE, false, false, 2);
        x += widths[0];
        drawTableCell(contentStream, x, y, widths[1], height, meter.getUnit(), SMALL_FONT_SIZE, false, true, 1);
        x += widths[1];

        drawMeterValuesGroup(
            contentStream,
            x,
            y,
            widths[2],
            widths[3],
            widths[4],
            halfHeight,
            meter.getMeterNumber(),
            meter.getPreviousValue(),
            meter.getCurrentValue(),
            meter.getConsumption()
        );
        x += widths[2] + widths[3] + widths[4];

        drawMeterValuesGroup(
            contentStream,
            x,
            y,
            widths[5],
            widths[6],
            widths[7],
            halfHeight,
            meter.getCommunalMeterNumber(),
            meter.getCommunalPreviousValue(),
            meter.getCommunalCurrentValue(),
            meter.getCommunalConsumption()
        );
        x += widths[5] + widths[6] + widths[7];

        drawTableCell(contentStream, x, y, widths[8], height, valueOrZero(meter.getHousePremiseVolume()), SMALL_FONT_SIZE, false, true, 1);
        x += widths[8];
        drawTableCell(contentStream, x, y, widths[9], height, valueOrZero(meter.getHouseCommonVolume()), SMALL_FONT_SIZE, false, true, 1);
        x += widths[9];
        drawTableCell(contentStream, x, y, widths[10], height, valueOrDash(meter.getIndividualNorm()), SMALL_FONT_SIZE, false, true, 1);
        x += widths[10];
        drawTableCell(contentStream, x, y, widths[11], height, valueOrDash(meter.getIncreasingRatio()), SMALL_FONT_SIZE, false, true, 1);
        x += widths[11];
        drawTableCell(contentStream, x, y, widths[12], height, valueOrZero(meter.getCommunalNorm()), SMALL_FONT_SIZE, false, true, 1);
    }

    private void drawMeterValuesGroup(
        PDPageContentStream contentStream,
        float x,
        float y,
        float firstWidth,
        float secondWidth,
        float thirdWidth,
        float halfHeight,
        String meterNumber,
        String previousValue,
        String currentValue,
        String consumption
    ) throws IOException {
        float groupWidth = firstWidth + secondWidth + thirdWidth;

        drawMeterNumberCell(contentStream, x, y, groupWidth, halfHeight, valueOrDash(meterNumber));
        drawTableCell(contentStream, x, y - halfHeight, firstWidth, halfHeight, valueOrDash(previousValue), SMALL_FONT_SIZE, false, true, 1);
        drawTableCell(contentStream, x + firstWidth, y - halfHeight, secondWidth, halfHeight, valueOrDash(currentValue), SMALL_FONT_SIZE, false, true, 1);
        drawTableCell(contentStream, x + firstWidth + secondWidth, y - halfHeight, thirdWidth, halfHeight, valueOrDash(consumption), SMALL_FONT_SIZE, false, true, 1);
    }

    private void drawMeterNumberCell(
        PDPageContentStream contentStream,
        float x,
        float y,
        float width,
        float height,
        String meterNumber
    ) throws IOException {
        drawTableCellStyled(contentStream, x, y, width, height, "", METER_NUMBER_FONT_SIZE, null, false, true, 1);
        String preparedMeterNumber = fit(meterNumber, width - 4, font, METER_NUMBER_FONT_SIZE);
        float textX = x + Math.max(0, width - textWidth(preparedMeterNumber, font, METER_NUMBER_FONT_SIZE)) / 2;
        drawText(contentStream, preparedMeterNumber, textX, y - cellTextOffset(height, 1), METER_NUMBER_FONT_SIZE, false);
    }

    private void drawBottomReference(PDPageContentStream contentStream, PaymentDocument paymentDocument, float y) throws IOException {
        float leftX = MARGIN;
        float valueX = MARGIN + 170;
        float lineY = y;
        float step = 7;

        drawLabelValue(contentStream, "Кол-во жильцов:", paymentDocument.getResidentsCount(), leftX, valueX, lineY);
        lineY -= step;
        drawLabelValue(contentStream, "Кол-во комнат:", paymentDocument.getRoomsCount(), leftX, valueX, lineY);
        lineY -= step;
        drawLabelValue(contentStream, "Площадь жилого помещения, м2:", paymentDocument.getFlatArea(), leftX, valueX, lineY);
        lineY -= step;
        drawLabelValue(contentStream, "Площадь всех жилых и нежил. помещений дома, м2:", paymentDocument.getHouseArea(), leftX, valueX, lineY);
        lineY -= step;
        drawLabelValue(contentStream, "Площадь ОИ по ЭЭ/Площадь ОИ по ГВ, м2:", slash(paymentDocument.getCommonElectricArea(), paymentDocument.getCommonHotWaterArea()), leftX, valueX, lineY);
        lineY -= step;
        drawLabelValue(contentStream, "Площадь нежил. помещ, м2:", paymentDocument.getNonResidentialArea(), leftX, valueX, lineY);
        lineY -= step;
        drawLabelValue(contentStream, "Площадь доли ОИ ЭЭ/Площадь доли ОИ ГВ, м2:", slash(paymentDocument.getElectricCommonShare(), paymentDocument.getHotWaterCommonShare()), leftX, valueX, lineY);

        String notice = "По вопросам обращаться: " + paymentDocument.getAgent()
            + ". Адрес: " + paymentDocument.getAgentAddress()
            + ". " + paymentDocument.getAgentPhone();
        drawWrappedText(contentStream, notice, MARGIN + 220, y, CONTENT_WIDTH - 220, SMALL_FONT_SIZE, 8, 5.2f, true);
    }

    private void drawLabelValue(PDPageContentStream contentStream, String label, String value, float x, float valueX, float y) throws IOException {
        drawText(contentStream, label, x, y, SMALL_FONT_SIZE, false);
        drawText(contentStream, valueOrDash(value), valueX, y, SMALL_FONT_SIZE, false);
    }

    private void drawCells(PDPageContentStream contentStream, float y, float[] widths, float height, List<String> cells, boolean header) throws IOException {
        float x = MARGIN;

        for (int index = 0; index < widths.length; index++) {
            String cell = index < cells.size() ? cells.get(index) : "";
            drawTableCell(contentStream, x, y, widths[index], height, cell, SMALL_FONT_SIZE, header, index > 0, header ? 2 : 1);
            x += widths[index];
        }
    }

    private void drawDataCells(
        PDPageContentStream contentStream,
        float y,
        float[] widths,
        float height,
        List<String> cells,
        LineLimitProvider lineLimitProvider
    ) throws IOException {
        float x = MARGIN;

        for (int index = 0; index < widths.length; index++) {
            String cell = index < cells.size() ? cells.get(index) : "";
            drawTableCell(contentStream, x, y, widths[index], height, cell, SMALL_FONT_SIZE, false, index > 0, lineLimitProvider.maxLines(index));
            x += widths[index];
        }
    }

    @FunctionalInterface
    private interface LineLimitProvider {
        int maxLines(int columnIndex);
    }

    private void drawTableCell(
        PDPageContentStream contentStream,
        float x,
        float y,
        float width,
        float height,
        String text,
        float fontSize,
        boolean header,
        boolean center,
        int maxLines
    ) throws IOException {
        if (header) {
            contentStream.setNonStrokingColor(HEADER_GRAY);
            contentStream.addRect(x, y - height, width, height);
            contentStream.fill();
        }

        contentStream.setStrokingColor(Color.BLACK);
        contentStream.setLineWidth(TABLE_LINE_WIDTH);
        contentStream.addRect(x, y - height, width, height);
        contentStream.stroke();
        drawCellText(contentStream, text, x + 2, y - cellTextOffset(height, maxLines), width - 4, fontSize, header, center, maxLines);
    }

    private void drawTableCellStyled(
        PDPageContentStream contentStream,
        float x,
        float y,
        float width,
        float height,
        String text,
        float fontSize,
        Color fillColor,
        boolean bold,
        boolean center,
        int maxLines
    ) throws IOException {
        if (fillColor != null) {
            contentStream.setNonStrokingColor(fillColor);
            contentStream.addRect(x, y - height, width, height);
            contentStream.fill();
        }

        contentStream.setStrokingColor(Color.BLACK);
        contentStream.setLineWidth(TABLE_LINE_WIDTH);
        contentStream.addRect(x, y - height, width, height);
        contentStream.stroke();
        drawCellText(contentStream, text, x + 2, y - cellTextOffset(height, maxLines), width - 4, fontSize, bold, center, maxLines);
    }

    private float cellTextOffset(float height, int maxLines) {
        if (maxLines > 1) {
            return Math.min(4.2f, Math.max(3.0f, height / 2.4f));
        }

        return Math.min(5.4f, Math.max(3.4f, height / 2 + 0.8f));
    }

    private void drawCellText(
        PDPageContentStream contentStream,
        String text,
        float x,
        float y,
        float width,
        float fontSize,
        boolean bold,
        boolean center,
        int maxLines
    ) throws IOException {
        String[] explicitLines = (text == null ? "" : text.replace('\u00A0', ' ').replace('\r', ' ')).split("\\n", -1);
        int writtenLines = 0;

        for (String explicitLine : explicitLines) {
            String remainingText = explicitLine.trim();

            if (remainingText.isBlank()) {
                writtenLines++;
                continue;
            }

            while (!remainingText.isBlank() && writtenLines < maxLines) {
                String line = maxLines == 1
                    ? fit(remainingText, width, bold ? boldFont : font, fontSize)
                    : cutLine(remainingText, width, bold ? boldFont : font, fontSize);
                float textX = center ? x + Math.max(0, width - textWidth(line, bold ? boldFont : font, fontSize)) / 2 : x;
                drawText(contentStream, line, textX, y - writtenLines * (fontSize + 0.35f), fontSize, bold);
                remainingText = remainingText.substring(Math.min(line.length(), remainingText.length())).trim();
                writtenLines++;
            }
        }
    }

    private float chargeRowHeight(PaymentCharge charge, float firstColumnWidth) throws IOException {
        return textWidth(charge.getServiceName(), font, SMALL_FONT_SIZE) > firstColumnWidth - 4 ? 9 : 7;
    }

    private float drawRecalculationLegend(PDPageContentStream contentStream, List<PaymentCharge> charges, float y) throws IOException {
        Map<String, String> legend = recalculationLegend(charges);

        if (legend.isEmpty()) {
            return y;
        }

        float textY = y - 6;
        drawText(contentStream, "Расшифровка кодов основания перерасчетов:", MARGIN, textY, SMALL_FONT_SIZE, false);
        float currentX = MARGIN + 165;

        for (Map.Entry<String, String> entry : legend.entrySet()) {
            String value = entry.getKey() + " - " + entry.getValue();
            float valueWidth = textWidth(value, font, SMALL_FONT_SIZE);

            if (currentX + valueWidth > MARGIN + CONTENT_WIDTH) {
                textY -= 8;
                currentX = MARGIN;
            }

            drawText(contentStream, value, currentX, textY, SMALL_FONT_SIZE, false);
            currentX += valueWidth + 14;
        }

        return textY - 4;
    }

    private Map<String, String> recalculationLegend(List<PaymentCharge> charges) {
        Map<String, String> result = new LinkedHashMap<>();

        for (PaymentCharge charge : charges) {
            String code = clean(charge.getRecalculationCode());
            String reason = clean(charge.getRecalculationReason());

            if (!code.isBlank() && !"-".equals(code) && !reason.isBlank()) {
                result.putIfAbsent(code, reason);
            }
        }

        return result;
    }

    private Map<String, List<PaymentCharge>> groupCharges(List<PaymentCharge> charges) {
        Map<String, List<PaymentCharge>> result = new LinkedHashMap<>();

        for (PaymentCharge charge : charges) {
            String key = clean(charge.getExecutorName()).isBlank() ? "Исполнитель" : charge.getExecutorName();
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(charge);
        }

        return result;
    }

    private String formatExecutorInfo(PaymentCharge charge) {
        StringBuilder result = new StringBuilder();
        appendRaw(result, charge.getExecutorName());
        appendRaw(result, charge.getExecutorAddress());
        appendRaw(result, "ИНН: " + charge.getExecutorInn());
        appendRaw(result, "КПП: " + charge.getExecutorKpp());

        return result.toString();
    }

    private void appendRaw(StringBuilder result, String value) {
        String preparedValue = clean(value);

        if (preparedValue.isBlank() || preparedValue.endsWith(": ")) {
            return;
        }

        if (!result.isEmpty()) {
            result.append(", ");
        }

        result.append(preparedValue);
    }

    private float[] chargeTableWidths() {
        return new float[] {60, 48, 27, 38, 38, 32, 36, 36, 37, 44, 30, 43, CONTENT_WIDTH - 469};
    }

    private float[] metersTableWidths() {
        return new float[] {84, 28, 35, 35, 35, 35, 35, 35, 47, 47, 38, 39, CONTENT_WIDTH - 493};
    }

    private float sum(float[] values, int startIndex, int count) {
        float result = 0;

        for (int index = startIndex; index < startIndex + count; index++) {
            result += values[index];
        }

        return result;
    }

    private String slash(String first, String second) {
        return valueOrZero(first) + "/" + valueOrZero(second);
    }

    private String twoLines(String first, String second) {
        String preparedFirst = clean(first);
        String preparedSecond = clean(second);

        if (preparedFirst.isBlank()) {
            return preparedSecond;
        }

        if (preparedSecond.isBlank()) {
            return preparedFirst;
        }

        return preparedFirst + "\n" + preparedSecond;
    }

    private String firstNotBlank(String first, String second) {
        return clean(first).isBlank() ? second : first;
    }

    private String totalAmount(List<PaymentCharge> charges) {
        double result = 0;

        for (PaymentCharge charge : charges) {
            result += parseAmount(charge.getTotalAmount());
        }

        return String.format(java.util.Locale.US, "%.2f", result);
    }

    private double parseAmount(String value) {
        String preparedValue = clean(value).replace(',', '.');

        if (preparedValue.isBlank() || "-".equals(preparedValue)) {
            return 0;
        }

        try {
            return Double.parseDouble(preparedValue);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String valueOrDash(String value) {
        String preparedValue = clean(value);

        return preparedValue.isBlank() ? "-" : preparedValue;
    }

    private String valueOrZero(String value) {
        String preparedValue = clean(value);

        return preparedValue.isBlank() ? "0" : preparedValue;
    }

    private String amount(String value) {
        String preparedValue = clean(value);

        return preparedValue.isBlank() ? "0.00" : preparedValue;
    }

    String formatDeliveryAddress(String sourceAddress) {
        String preparedAddress = cleanupAddressForPrint(sourceAddress);

        String premiseAddress = extractPremiseAddress(preparedAddress);
        if (!premiseAddress.isBlank()) {
            return "Адрес помещения: " + cleanupAddressForPrint(premiseAddress);
        }

        String lowerAddress = preparedAddress.toLowerCase(java.util.Locale.ROOT);

        if (lowerAddress.startsWith("адрес помещения:")
            || lowerAddress.startsWith("адрес доставки:")) {
            return preparedAddress;
        }

        return "Адрес доставки: " + preparedAddress;
    }

    private String extractPremiseAddress(String address) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("(?iu)\\(\\s*адрес\\s+помещения:\\s*(.+?)\\s*\\)")
            .matcher(address);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1);
    }

    private void drawWrappedText(
        PDPageContentStream contentStream,
        String text,
        float x,
        float y,
        float width,
        float fontSize,
        int maxLines,
        float lineHeight
    ) throws IOException {
        drawWrappedText(contentStream, text, x, y, width, fontSize, maxLines, lineHeight, false);
    }

    private void drawWrappedText(
        PDPageContentStream contentStream,
        String text,
        float x,
        float y,
        float width,
        float fontSize,
        int maxLines,
        float lineHeight,
        boolean bold
    ) throws IOException {
        String remainingText = clean(text);
        PDType0Font selectedFont = bold ? boldFont : font;

        for (int lineIndex = 0; lineIndex < maxLines && !remainingText.isBlank(); lineIndex++) {
            String line = cutLine(remainingText, width, selectedFont, fontSize);
            drawText(contentStream, line, x, y - lineIndex * lineHeight, fontSize, bold);
            remainingText = remainingText.substring(Math.min(line.length(), remainingText.length())).trim();
        }
    }

    private String cutLine(String text, float width, PDType0Font selectedFont, float fontSize) throws IOException {
        String preparedText = clean(text);

        if (textWidth(preparedText, selectedFont, fontSize) <= width) {
            return preparedText;
        }

        int splitIndex = preparedText.length();

        while (splitIndex > 0 && textWidth(preparedText.substring(0, splitIndex), selectedFont, fontSize) > width) {
            splitIndex--;
        }

        int spaceIndex = preparedText.lastIndexOf(' ', splitIndex);

        if (spaceIndex > 0) {
            splitIndex = spaceIndex;
        }

        return preparedText.substring(0, Math.max(1, splitIndex)).trim();
    }

    private String fit(String text, float width, PDType0Font selectedFont, float fontSize) throws IOException {
        String preparedText = clean(text);

        if (textWidth(preparedText, selectedFont, fontSize) <= width) {
            return preparedText;
        }

        String suffix = "...";
        int length = preparedText.length();

        while (length > 0 && textWidth(preparedText.substring(0, length) + suffix, selectedFont, fontSize) > width) {
            length--;
        }

        return preparedText.substring(0, Math.max(0, length)) + suffix;
    }

    private float textWidth(String text, PDType0Font selectedFont, float fontSize) throws IOException {
        return selectedFont.getStringWidth(clean(text)) / 1000 * fontSize;
    }

    private void drawText(PDPageContentStream contentStream, String text, float x, float y, float fontSize, boolean bold) throws IOException {
        contentStream.beginText();
        contentStream.setFont(bold ? boldFont : font, fontSize);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(clean(text));
        contentStream.endText();
    }

    private void drawRotatedText(PDPageContentStream contentStream, String text, float x, float y, float fontSize, boolean bold) throws IOException {
        contentStream.saveGraphicsState();
        contentStream.beginText();
        contentStream.setFont(bold ? boldFont : font, fontSize);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(90), x, y));
        contentStream.showText(clean(text));
        contentStream.endText();
        contentStream.restoreGraphicsState();
    }

    private void drawRotatedWrappedText(
        PDPageContentStream contentStream,
        String text,
        float x,
        float y,
        float width,
        float fontSize,
        int maxLines
    ) throws IOException {
        String remainingText = clean(text);

        for (int lineIndex = 0; lineIndex < maxLines && !remainingText.isBlank(); lineIndex++) {
            String line = cutLine(remainingText, width, font, fontSize);
            drawRotatedText(contentStream, line, x - lineIndex * 5, y, fontSize, false);
            remainingText = remainingText.substring(Math.min(line.length(), remainingText.length())).trim();
        }
    }

    private void drawQrCode(PDPageContentStream contentStream, PaymentQrCode qrCode, float x, float y, float size) throws IOException {
        try {
            BitMatrix matrix = createQrMatrix(qrCode.getText());
            float moduleSize = size / matrix.getWidth();

            contentStream.setNonStrokingColor(Color.WHITE);
            contentStream.addRect(x, y, size, size);
            contentStream.fill();

            contentStream.setNonStrokingColor(Color.BLACK);

            for (int matrixY = 0; matrixY < matrix.getHeight(); matrixY++) {
                for (int matrixX = 0; matrixX < matrix.getWidth(); matrixX++) {
                    if (matrix.get(matrixX, matrixY)) {
                        contentStream.addRect(
                            x + matrixX * moduleSize,
                            y + size - (matrixY + 1) * moduleSize,
                            moduleSize,
                            moduleSize
                        );
                    }
                }
            }

            contentStream.fill();
        } catch (WriterException exception) {
            throw new IOException("Не удалось сформировать QR-код", exception);
        }
    }

    private BitMatrix createQrMatrix(String qrText) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        return new QRCodeWriter().encode(qrText, BarcodeFormat.QR_CODE, 110, 110, hints);
    }

    private String clean(String text) {
        return text == null
            ? ""
            : text.replace('\u00A0', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void saveCurrentDocument() throws IOException {
        if (document == null || currentFilePages == 0) {
            return;
        }

        fileNumber++;
        Path resultFile = outputFolder.resolve("%s-%03d.pdf".formatted(RESULT_FILE_PREFIX, fileNumber));

        document.save(resultFile.toFile());
        document.close();
        document = null;
        font = null;
        boldFont = null;
        currentFilePages = 0;

        System.out.println("Создан PDF из XML: " + resultFile);
    }

    private Path resolveFontPath(String fileName) {
        Path windowsFont = Path.of(System.getenv("WINDIR"), "Fonts", fileName);

        if (Files.exists(windowsFont)) {
            return windowsFont;
        }

        return Path.of("C:", "Windows", "Fonts", fileName);
    }

    private String cleanupAddressForPrint(String address) {
        return clean(address)
            .replaceAll("\\s*,\\s*\\.\\s*,\\s*", ", ")
            .replaceAll("\\s*,\\s*\\.\\s+", ", ")
            .replaceAll("\\s+\\.\\s*,\\s*", ", ")
            .replaceAll(",\\s*,+", ", ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
