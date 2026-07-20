package org.vc.ui;

import org.vc.address.PaymentSupplier;

/**
 * Контракт представления главного окна парсера.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public interface ParserView {

    /**
     * Возвращает путь к выбранному Excel-файлу.
     */
    String getSelectedExcelPath();

    /**
     * Возвращает путь к выбранной папке с PDF-файлами.
     */
    String getSelectedPdfFolderPath();

    /**
     * Возвращает признак включённой двусторонней печати.
     */
    boolean isDuplexPrintingSelected();

    /**
     * Возвращает выбранный формат поставщика PDF.
     */
    PaymentSupplier getSelectedPaymentSupplier();

    /**
     * Обновляет визуальное состояние текущей фоновой операции.
     */
    void showTaskState(ParserTaskState state, String message);

    /**
     * Очищает область лога.
     */
    void clearLog();

    /**
     * Показывает сообщение об ошибке пользователю.
     */
    void showError(String message);
}
