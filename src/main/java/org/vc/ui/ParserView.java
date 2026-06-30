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
     * Возвращает путь к выбранной папке с XML-файлами.
     */
    String getSelectedXmlFolderPath();

    /**
     * Возвращает путь к выбранной папке результата для PDF из XML.
     */
    String getSelectedXmlOutputFolderPath();

    /**
     * Возвращает признак включённой двусторонней печати.
     */
    boolean isDuplexPrintingSelected();

    /**
     * Возвращает выбранный формат поставщика PDF.
     */
    PaymentSupplier getSelectedPaymentSupplier();

    /**
     * Очищает область лога.
     */
    void clearLog();

    /**
     * Показывает сообщение об ошибке пользователю.
     */
    void showError(String message);
}
