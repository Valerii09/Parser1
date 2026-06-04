package org.vc.payment;

/**
 * QR-код для оплаты, прочитанный из XML-платёжки.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class PaymentQrCode {

    private final String text;
    private final String description;

    public PaymentQrCode(String text, String description) {
        this.text = text;
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public String getDescription() {
        return description;
    }
}
