package org.vc.xml;

import org.vc.payment.PaymentDocument;

import java.io.IOException;

/**
 * Обработчик платёжного документа, прочитанного из XML.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
@FunctionalInterface
public interface PaymentDocumentConsumer {

    /**
     * Принимает платёжный документ для дальнейшей обработки.
     *
     * @param paymentDocument платёжный документ
     */
    void accept(PaymentDocument paymentDocument) throws IOException;
}
