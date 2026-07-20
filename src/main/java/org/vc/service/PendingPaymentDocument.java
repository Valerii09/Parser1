package org.vc.service;

import java.util.List;

/**
 * Платёжка, ожидающая назначения по контексту исходного PDF.
 */
record PendingPaymentDocument(
    int pageIndex,
    String address,
    List<String> registryAddresses,
    int paymentDocumentIndex,
    int paymentDocumentsOnPage,
    int registryPaymentDocumentsCount
) {
}
