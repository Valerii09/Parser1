package org.vc.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.vc.payment.PaymentQrCode;

import java.awt.Color;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Рисует QR-код платежа прямо в PDF, без промежуточных картинок.
 * Так сохраняется четкая сетка модулей, а банковские приложения лучше считывают код с распечатки.
 */
final class PdfQrCodeRenderer {

    private PdfQrCodeRenderer() {
    }

    static void draw(PDPageContentStream contentStream, PaymentQrCode qrCode, float x, float y, float size) throws IOException {
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

    private static BitMatrix createQrMatrix(String qrText) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        return new QRCodeWriter().encode(qrText, BarcodeFormat.QR_CODE, 110, 110, hints);
    }
}
