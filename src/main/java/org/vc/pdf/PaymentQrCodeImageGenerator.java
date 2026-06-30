package org.vc.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.vc.payment.PaymentQrCode;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * Превращает платёжный QR из XML в PNG data URI для HTML-шаблона.
 * Data URI позволяет рендерить PDF без временных файлов с картинками.
 */
final class PaymentQrCodeImageGenerator {

    private static final int IMAGE_SIZE = 220;

    private PaymentQrCodeImageGenerator() {
    }

    static String toDataUri(PaymentQrCode qrCode) throws IOException {
        try {
            BitMatrix matrix = createQrMatrix(qrCode.getText());
            BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < IMAGE_SIZE; y++) {
                for (int x = 0; x < IMAGE_SIZE; x++) {
                    int matrixX = x * matrix.getWidth() / IMAGE_SIZE;
                    int matrixY = y * matrix.getHeight() / IMAGE_SIZE;
                    image.setRGB(x, y, matrix.get(matrixX, matrixY) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException exception) {
            throw new IOException("Не удалось сформировать QR-код для HTML-шаблона", exception);
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
