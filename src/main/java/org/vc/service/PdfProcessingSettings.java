package org.vc.service;

import org.apache.pdfbox.io.MemoryUsageSetting;

import java.nio.file.Path;

/**
 * Ограничения ресурсоёмких операций при сортировке PDF.
 *
 * <p>Значения по умолчанию рассчитаны на рабочую станцию с 12 ГБ оперативной памяти:
 * одновременно читаются не более двух исходных PDF, а итоговые файлы собираются
 * последовательно. Количество потоков можно изменить системным свойством
 * {@code paymentCourier.pdfThreads}, не меняя код приложения.</p>
 */
final class PdfProcessingSettings {

    private static final int DEFAULT_PDF_THREADS = 2;
    private static final long SOURCE_PDF_MEMORY_BUFFER_BYTES = 64L * 1024 * 1024;

    private final int pdfThreads;

    private PdfProcessingSettings(int pdfThreads) {
        this.pdfThreads = pdfThreads;
    }

    static PdfProcessingSettings systemDefaults() {
        int requestedThreads = Integer.getInteger("paymentCourier.pdfThreads", DEFAULT_PDF_THREADS);
        int availableProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
        int safeThreads = Math.max(1, Math.min(requestedThreads, availableProcessors));

        return new PdfProcessingSettings(safeThreads);
    }

    int pdfThreads() {
        return pdfThreads;
    }

    MemoryUsageSetting sourceMemoryUsage(Path tempRoot) {
        return MemoryUsageSetting
            .setupMixed(SOURCE_PDF_MEMORY_BUFFER_BYTES)
            .setTempDir(tempRoot.toFile());
    }
}
