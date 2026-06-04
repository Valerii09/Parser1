package org.vc.report;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Накапливает и печатает статистику обработки PDF-файлов.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class ProcessingStats {

    private int foundPdfFiles;
    private int processedPdfFiles;
    private int totalPdfPages;
    private int totalPaymentDocuments;
    private int processedPages;
    private int pagesWithoutAddress;
    private int matchedPages;
    private int unmatchedPages;
    private int unmatchedPagesWrittenToReport;
    private int unmatchedPagesSkippedFromReport;
    private int createdCourierPdfFiles;
    private int writtenPages;

    private final Map<String, Integer> pagesByCourier = new LinkedHashMap<>();

    public void setFoundPdfFiles(int foundPdfFiles) {
        this.foundPdfFiles = foundPdfFiles;
    }

    public void incrementProcessedPdfFiles() {
        processedPdfFiles++;
    }

    public void addTotalPdfPages(int pages) {
        totalPdfPages += pages;
    }

    public void addPaymentDocuments(int documents) {
        totalPaymentDocuments += documents;
    }

    public void incrementProcessedPages() {
        processedPages++;
    }

    public void incrementPagesWithoutAddress() {
        pagesWithoutAddress++;
    }

    public void incrementMatchedPages() {
        matchedPages++;
    }

    public void incrementUnmatchedPages() {
        unmatchedPages++;
    }

    public void incrementUnmatchedPagesWrittenToReport() {
        unmatchedPagesWrittenToReport++;
    }

    public void incrementUnmatchedPagesSkippedFromReport() {
        unmatchedPagesSkippedFromReport++;
    }

    public void incrementCreatedCourierPdfFiles() {
        incrementCreatedCourierPdfFiles(1);
    }

    public void incrementCreatedCourierPdfFiles(int count) {
        createdCourierPdfFiles += count;
    }

    public void addWrittenPages(int pages) {
        writtenPages += pages;
    }

    public void addCourierPage(String courierName) {
        pagesByCourier.merge(courierName, 1, Integer::sum);
    }

    public int getProcessedPages() {
        return processedPages;
    }

    /**
     * Печатает собранную итоговую статистику в стандартный вывод.
     */
    public void print() {
        printSummary();
        printPageProcessingCheck();
        printPageDistributionCheck();
        printResultWritingCheck();
        printCourierSummary();
    }

    private void printSummary() {
        System.out.println();
        System.out.println("========== ИТОГ ==========");
        System.out.println("Найдено PDF-файлов: " + foundPdfFiles);
        System.out.println("Обработано PDF-файлов: " + processedPdfFiles);
        System.out.println("Всего физических страниц в PDF: " + totalPdfPages);
        System.out.println("Всего платёжек по тексту PDF: " + totalPaymentDocuments);
        System.out.println("Обработано платёжек: " + processedPages);
        System.out.println("Платёжек без найденного адреса: " + pagesWithoutAddress);
        System.out.println("Найдено платёжек по адресам курьеров: " + matchedPages);
        System.out.println("Платёжек без найденного курьера: " + unmatchedPages);
        System.out.println("Записано строк в отчёт нераспределённых: " + unmatchedPagesWrittenToReport);
        System.out.println("Не удалось записать в отчёт нераспределённых: " + unmatchedPagesSkippedFromReport);
        System.out.println("Создано PDF-файлов по курьерам: " + createdCourierPdfFiles);
        System.out.println("Записано физических страниц в PDF курьеров: " + writtenPages);
    }

    private void printPageProcessingCheck() {
        System.out.println();
        System.out.println("Проверка обработки платёжек:");

        if (totalPaymentDocuments == processedPages) {
            System.out.println("Все платёжки обработаны: да");
        } else {
            System.out.println("Все платёжки обработаны: нет");
            System.out.println("Не обработано платёжек: " + (totalPaymentDocuments - processedPages));
        }
    }

    private void printPageDistributionCheck() {
        System.out.println();
        System.out.println("Проверка распределения платёжек:");

        int accountedPages = matchedPages + unmatchedPages + pagesWithoutAddress;

        System.out.println("Учтено по статусам: " + accountedPages + " из " + processedPages);

        if (accountedPages == processedPages) {
            System.out.println("Все обработанные платёжки учтены: да");
        } else {
            System.out.println("Все обработанные платёжки учтены: нет");
            System.out.println("Не учтено платёжек: " + (processedPages - accountedPages));
        }
    }

    private void printResultWritingCheck() {
        System.out.println();
        System.out.println("Проверка записи результатов:");

        int reportCheck = matchedPages + unmatchedPagesWrittenToReport + unmatchedPagesSkippedFromReport + pagesWithoutAddress;

        System.out.println("PDF курьеров + отчёт + без адреса: " + reportCheck + " из " + processedPages);

        if (reportCheck == processedPages) {
            System.out.println("Все платёжки либо записаны, либо объяснены: да");
        } else {
            System.out.println("Все платёжки либо записаны, либо объяснены: нет");
            System.out.println("Разница: " + (processedPages - reportCheck));
        }
    }

    private void printCourierSummary() {
        System.out.println();
        System.out.println("По курьерам:");

        for (Map.Entry<String, Integer> entry : pagesByCourier.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " платёжек");
        }

        System.out.println("==========================");
        System.out.println();
    }

}
