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

    /**
     * Устанавливает значение.
     *
     * @param foundPdfFiles новое значение
     */
    public synchronized void setFoundPdfFiles(int foundPdfFiles) {
        this.foundPdfFiles = foundPdfFiles;
    }

    
    public synchronized void incrementProcessedPdfFiles() {
        processedPdfFiles++;
    }

    
    public synchronized void addTotalPdfPages(int pages) {
        totalPdfPages += pages;
    }

    
    public synchronized void addPaymentDocuments(int documents) {
        totalPaymentDocuments += documents;
    }

    
    public synchronized void incrementProcessedPages() {
        addProcessedPages(1);
    }

    public synchronized void addProcessedPages(int pages) {
        processedPages += pages;
    }

    
    public synchronized void incrementPagesWithoutAddress() {
        pagesWithoutAddress++;
    }

    
    public synchronized void incrementMatchedPages() {
        addMatchedPages(1);
    }

    public synchronized void addMatchedPages(int pages) {
        matchedPages += pages;
    }

    
    public synchronized void incrementUnmatchedPages() {
        addUnmatchedPages(1);
    }

    public synchronized void addUnmatchedPages(int pages) {
        unmatchedPages += pages;
    }

    
    public synchronized void incrementUnmatchedPagesWrittenToReport() {
        addUnmatchedPagesWrittenToReport(1);
    }

    public synchronized void addUnmatchedPagesWrittenToReport(int pages) {
        unmatchedPagesWrittenToReport += pages;
    }

    
    public synchronized void incrementUnmatchedPagesSkippedFromReport() {
        addUnmatchedPagesSkippedFromReport(1);
    }

    public synchronized void addUnmatchedPagesSkippedFromReport(int pages) {
        unmatchedPagesSkippedFromReport += pages;
    }

    
    public synchronized void incrementCreatedCourierPdfFiles() {
        incrementCreatedCourierPdfFiles(1);
    }

    
    public synchronized void incrementCreatedCourierPdfFiles(int count) {
        createdCourierPdfFiles += count;
    }

    
    public synchronized void addWrittenPages(int pages) {
        writtenPages += pages;
    }

    
    public synchronized void addCourierPage(String courierName) {
        addCourierPage(courierName, 1);
    }

    public synchronized void addCourierPage(String courierName, int pages) {
        pagesByCourier.merge(courierName, pages, Integer::sum);
    }

    
    public synchronized int getProcessedPages() {
        return processedPages;
    }

    /**
     * Печатает собранную итоговую статистику в стандартный вывод.
     */
    public synchronized void print() {
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
