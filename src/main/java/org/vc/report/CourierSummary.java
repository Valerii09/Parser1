package org.vc.report;

/**
 * Итог обработки одного курьера для сводного отчёта.
 */
public record CourierSummary(
    String courierName,
    int inputPagesCount,
    int outputPagesCount,
    String firstAddress,
    String lastAddress
) {
}
