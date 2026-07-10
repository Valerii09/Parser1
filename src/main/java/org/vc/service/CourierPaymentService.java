package org.vc.service;

import org.vc.pdf.CourierPdfWriter;
import org.vc.pdf.PdfPageTempStorage;
import org.vc.repository.CourierAddressRepository;
import org.vc.report.ProcessingStats;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.vc.address.AddressExtractor;
import org.vc.address.PaymentSupplier;
import org.vc.pdf.CourierPage;
import org.vc.report.UnmatchedAddressExcelWriter;
import org.vc.report.UnmatchedAddressRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Координирует полный цикл обработки PDF-файлов.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierPaymentService {

    // TEMP_FAST_MODE: временное ускорение под большую оперативную память.
    // Откатить после срочной обработки: вернуть последовательную обработку PDF и setupTempFileOnly().
    private static final boolean TEMP_FAST_MODE = true;
    private static final int TEMP_FAST_PDF_THREADS = Integer.getInteger(
        "paymentCourier.fast.pdfThreads",
        Math.max(2, Math.min(12, Runtime.getRuntime().availableProcessors() - 1))
    );

    private final CourierAddressRepository addressRepository = new CourierAddressRepository();
    private final CourierMatcher courierMatcher = new CourierMatcher();
    private final PdfPageTempStorage tempStorage = new PdfPageTempStorage();
    private final CourierPdfWriter pdfWriter = new CourierPdfWriter();
    private final UnmatchedAddressExcelWriter unmatchedAddressExcelWriter = new UnmatchedAddressExcelWriter();

    /**
     * Обрабатывает PDF-платёжки, распределяет страницы по курьерам и записывает все результаты.
     */
    public void process(Path pdfFolder, Path couriersRoot, boolean duplexPrinting) throws IOException {
        process(pdfFolder, couriersRoot, duplexPrinting, PaymentSupplier.AUTO);
    }

    /**
     * Обрабатывает PDF-платёжки с учётом формата выбранного поставщика.
     */
    public void process(
        Path pdfFolder,
        Path couriersRoot,
        boolean duplexPrinting,
        PaymentSupplier supplier
    ) throws IOException {
        ProcessingStats stats = new ProcessingStats();
        UnmatchedAddressRegistry unmatchedAddressRegistry = new UnmatchedAddressRegistry();
        AddressExtractor addressExtractor = new AddressExtractor(supplier);

        Map<String, Set<String>> courierAddresses = addressRepository.readCourierAddresses(couriersRoot);

        if (courierAddresses.isEmpty()) {
            throw new IllegalStateException("Не найдены адреса курьеров в папке: " + couriersRoot);
        }

        courierMatcher.init(courierAddresses);

        Map<String, List<CourierPage>> courierPages = createCourierPageMap(courierAddresses.keySet());
        Path tempRoot = Files.createTempDirectory("courier-payment-pages-");

        try {
            processPdfFolder(
                pdfFolder,
                courierPages,
                tempRoot,
                stats,
                unmatchedAddressRegistry,
                duplexPrinting,
                addressExtractor,
                supplier.isFirstAddressOnly()
            );
            pdfWriter.writeCourierPdfs(couriersRoot, courierPages, stats);
            unmatchedAddressExcelWriter.write(couriersRoot, unmatchedAddressRegistry);
            stats.print();
        } finally {
            tempStorage.deleteTempDirectory(tempRoot);
        }

        System.out.println("Готово. PDF-файлы созданы в папках курьеров: " + couriersRoot);
    }

    /**
     * Обрабатывает PDF-платёжки в режиме односторонней печати.
     */
    public void process(Path pdfFolder, Path couriersRoot) throws IOException {
        process(pdfFolder, couriersRoot, false);
    }

    private Map<String, List<CourierPage>> createCourierPageMap(Set<String> courierNames) {
        Map<String, List<CourierPage>> result = new LinkedHashMap<>();

        for (String courierName : courierNames) {
            result.put(courierName, Collections.synchronizedList(new ArrayList<>()));
        }

        return result;
    }

    private void processPdfFolder(
        Path pdfFolder,
        Map<String, List<CourierPage>> courierPages,
        Path tempRoot,
        ProcessingStats stats,
        UnmatchedAddressRegistry unmatchedAddressRegistry,
        boolean duplexPrinting,
        AddressExtractor addressExtractor,
        boolean firstAddressOnly
    ) throws IOException {
        if (!Files.exists(pdfFolder)) {
            throw new IllegalStateException("Папка с PDF не найдена: " + pdfFolder);
        }

        List<Path> pdfFiles = findPdfFiles(pdfFolder);
        stats.setFoundPdfFiles(pdfFiles.size());

        System.out.println("Найдено PDF-файлов: " + pdfFiles.size());

        if (TEMP_FAST_MODE && pdfFiles.size() > 1) {
            processPdfFilesFast(
                pdfFiles,
                courierPages,
                tempRoot,
                stats,
                unmatchedAddressRegistry,
                duplexPrinting,
                addressExtractor,
                firstAddressOnly
            );
            return;
        }

        for (Path pdfFile : pdfFiles) {
            processPdfFile(
                pdfFile,
                courierPages,
                tempRoot,
                stats,
                unmatchedAddressRegistry,
                duplexPrinting,
                addressExtractor,
                firstAddressOnly
            );
        }
    }

    private void processPdfFilesFast(
        List<Path> pdfFiles,
        Map<String, List<CourierPage>> courierPages,
        Path tempRoot,
        ProcessingStats stats,
        UnmatchedAddressRegistry unmatchedAddressRegistry,
        boolean duplexPrinting,
        AddressExtractor addressExtractor,
        boolean firstAddressOnly
    ) throws IOException {
        int threads = Math.min(TEMP_FAST_PDF_THREADS, pdfFiles.size());

        System.out.println("TEMP_FAST_MODE: параллельная обработка PDF, потоков: " + threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (Path pdfFile : pdfFiles) {
            futures.add(executor.submit(() -> {
                processPdfFile(
                    pdfFile,
                    courierPages,
                    tempRoot,
                    stats,
                    unmatchedAddressRegistry,
                    duplexPrinting,
                    addressExtractor,
                    firstAddressOnly
                );
                return null;
            }));
        }

        executor.shutdown();
        waitFastTasks(futures);
    }

    private void waitFastTasks(List<Future<?>> futures) throws IOException {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Обработка PDF прервана", exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();

                if (cause instanceof IOException ioException) {
                    throw ioException;
                }

                throw new IOException("Ошибка параллельной обработки PDF", cause);
            }
        }
    }

    private List<Path> findPdfFiles(Path pdfFolder) throws IOException {
        try (Stream<Path> files = Files.walk(pdfFolder)) {
            return files
                .filter(Files::isRegularFile)
                .filter(this::isPdf)
                .toList();
        }
    }

    private boolean isPdf(Path file) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);

        return fileName.endsWith(".pdf");
    }

    private void processPdfFile(
        Path pdfFile,
        Map<String, List<CourierPage>> courierPages,
        Path tempRoot,
        ProcessingStats stats,
        UnmatchedAddressRegistry unmatchedAddressRegistry,
        boolean duplexPrinting,
        AddressExtractor addressExtractor,
        boolean firstAddressOnly
    ) throws IOException {
        System.out.println("Обрабатываю PDF: " + pdfFile);

        stats.incrementProcessedPdfFiles();

        try (PDDocument sourceDocument = PDDocument.load(
            pdfFile.toFile(),
            TEMP_FAST_MODE
                ? MemoryUsageSetting.setupMainMemoryOnly()
                : MemoryUsageSetting.setupTempFileOnly()
        )) {
            PDFTextStripper textStripper = new PDFTextStripper();
            int pageCount = sourceDocument.getNumberOfPages();

            stats.addTotalPdfPages(pageCount);

            if (duplexPrinting && pageCount % 2 != 0) {
                System.out.println(
                    "Предупреждение: включена двусторонняя печать, но количество страниц нечётное. PDF: "
                        + pdfFile
                        + ", страниц: "
                        + pageCount
                );
            }

            Map<String, Set<String>> contextualCouriersByAddress = new LinkedHashMap<>();
            Map<Integer, Set<String>> contextualCouriersByPage = new LinkedHashMap<>();
            List<PendingPaymentDocument> pendingDocuments = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex = getNextPaymentPageIndex(pageIndex, duplexPrinting)) {
                textStripper.setStartPage(pageIndex + 1);
                textStripper.setEndPage(pageIndex + 1);

                String pageText = textStripper.getText(sourceDocument);
                List<String> addresses = addressExtractor.extractAddresses(pageText);
                int paymentDocumentsOnPage = Math.max(addresses.size(), 1);

                stats.addPaymentDocuments(paymentDocumentsOnPage);

                if (addresses.isEmpty()) {
                    stats.incrementProcessedPages();
                    stats.incrementPagesWithoutAddress();
                    unmatchedAddressRegistry.addMissingAddressDocument(pdfFile, pageIndex + 1, pageText);
                    continue;
                }

                if (firstAddressOnly) {
                    processPaymentDocument(
                        sourceDocument,
                        pdfFile,
                        pageIndex,
                        pageCount,
                        tempRoot,
                        stats,
                        unmatchedAddressRegistry,
                        courierPages,
                        addresses.get(0),
                        addresses,
                        0,
                        1,
                        paymentDocumentsOnPage,
                        duplexPrinting,
                        contextualCouriersByAddress,
                        contextualCouriersByPage,
                        pendingDocuments
                    );
                    continue;
                }

                for (int paymentDocumentIndex = 0; paymentDocumentIndex < addresses.size(); paymentDocumentIndex++) {
                    processPaymentDocument(
                        sourceDocument,
                        pdfFile,
                        pageIndex,
                        pageCount,
                        tempRoot,
                        stats,
                        unmatchedAddressRegistry,
                        courierPages,
                        addresses.get(paymentDocumentIndex),
                        List.of(addresses.get(paymentDocumentIndex)),
                        paymentDocumentIndex,
                        paymentDocumentsOnPage,
                        1,
                        duplexPrinting,
                        contextualCouriersByAddress,
                        contextualCouriersByPage,
                        pendingDocuments
                    );
                }
            }

            processPendingDocuments(
                sourceDocument,
                pdfFile,
                pageCount,
                tempRoot,
                stats,
                unmatchedAddressRegistry,
                courierPages,
                duplexPrinting,
                contextualCouriersByAddress,
                contextualCouriersByPage,
                pendingDocuments
            );
        }
    }

    private void processPaymentDocument(
        PDDocument sourceDocument,
        Path pdfFile,
        int pageIndex,
        int pageCount,
        Path tempRoot,
        ProcessingStats stats,
        UnmatchedAddressRegistry unmatchedAddressRegistry,
        Map<String, List<CourierPage>> courierPages,
        String address,
        List<String> registryAddresses,
        int paymentDocumentIndex,
        int paymentDocumentsOnPage,
        int registryPaymentDocumentsCount,
        boolean duplexPrinting,
        Map<String, Set<String>> contextualCouriersByAddress,
        Map<Integer, Set<String>> contextualCouriersByPage,
        List<PendingPaymentDocument> pendingDocuments
    ) throws IOException {
        stats.addProcessedPages(registryPaymentDocumentsCount);

        String courierName = courierMatcher.findCourierByAddresses(registryAddresses);

        if (courierName == null) {
            pendingDocuments.add(new PendingPaymentDocument(
                pageIndex,
                address,
                List.copyOf(registryAddresses),
                paymentDocumentIndex,
                paymentDocumentsOnPage,
                registryPaymentDocumentsCount
            ));
            return;
        }

        rememberContext(
            courierName,
            pageIndex,
            registryAddresses,
            contextualCouriersByAddress,
            contextualCouriersByPage
        );

        writeMatchedPaymentDocument(
            sourceDocument,
            pageIndex,
            pageCount,
            tempRoot,
            stats,
            courierPages,
            address,
            registryAddresses,
            paymentDocumentIndex,
            paymentDocumentsOnPage,
            registryPaymentDocumentsCount,
            duplexPrinting,
            courierName
        );
    }

    private void writeMatchedPaymentDocument(
        PDDocument sourceDocument,
        int pageIndex,
        int pageCount,
        Path tempRoot,
        ProcessingStats stats,
        Map<String, List<CourierPage>> courierPages,
        String address,
        List<String> registryAddresses,
        int paymentDocumentIndex,
        int paymentDocumentsOnPage,
        int registryPaymentDocumentsCount,
        boolean duplexPrinting,
        String courierName
    ) throws IOException {

        List<Path> pageFiles = savePaymentPages(
            sourceDocument,
            pageIndex,
            pageCount,
            tempRoot,
            courierName,
            paymentDocumentIndex,
            paymentDocumentsOnPage,
            duplexPrinting
        );

        courierPages.get(courierName).add(new CourierPage(address, pageFiles, registryAddresses));

        stats.addMatchedPages(registryPaymentDocumentsCount);
        stats.addCourierPage(courierName, registryPaymentDocumentsCount);
    }

    private void processPendingDocuments(
        PDDocument sourceDocument,
        Path pdfFile,
        int pageCount,
        Path tempRoot,
        ProcessingStats stats,
        UnmatchedAddressRegistry unmatchedAddressRegistry,
        Map<String, List<CourierPage>> courierPages,
        boolean duplexPrinting,
        Map<String, Set<String>> contextualCouriersByAddress,
        Map<Integer, Set<String>> contextualCouriersByPage,
        List<PendingPaymentDocument> pendingDocuments
    ) throws IOException {
        for (PendingPaymentDocument pending : pendingDocuments) {
            String courierName = findContextCourier(
                pending.registryAddresses(),
                contextualCouriersByAddress
            );

            if (courierName == null) {
                courierName = findNearestPageCourier(pending.pageIndex(), contextualCouriersByPage);
            }

            if (courierName == null) {
                courierName = findDominantCourier(contextualCouriersByPage);
            }

            if (courierName == null) {
                registerUnmatchedAddress(
                    pending.address(),
                    pdfFile,
                    stats,
                    unmatchedAddressRegistry,
                    pending.registryPaymentDocumentsCount()
                );
                continue;
            }

            System.out.println(
                "PDF ПРИНУДИТЕЛЬНО РАСПРЕДЕЛЁН ПО КОНТЕКСТУ ФАЙЛА: "
                    + courierName + " / " + pending.address()
            );

            rememberContext(
                courierName,
                pending.pageIndex(),
                pending.registryAddresses(),
                contextualCouriersByAddress,
                contextualCouriersByPage
            );

            writeMatchedPaymentDocument(
                sourceDocument,
                pending.pageIndex(),
                pageCount,
                tempRoot,
                stats,
                courierPages,
                pending.address(),
                pending.registryAddresses(),
                pending.paymentDocumentIndex(),
                pending.paymentDocumentsOnPage(),
                pending.registryPaymentDocumentsCount(),
                duplexPrinting,
                courierName
            );
        }
    }

    private void rememberContext(
        String courierName,
        int pageIndex,
        List<String> addresses,
        Map<String, Set<String>> contextualCouriersByAddress,
        Map<Integer, Set<String>> contextualCouriersByPage
    ) {
        contextualCouriersByPage
            .computeIfAbsent(pageIndex, ignored -> new LinkedHashSet<>())
            .add(courierName);

        for (String address : addresses) {
            String key = contextualAddressKey(address);
            if (!key.isEmpty()) {
                contextualCouriersByAddress
                    .computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                    .add(courierName);
            }
        }
    }

    private String findContextCourier(
        List<String> addresses,
        Map<String, Set<String>> contextualCouriersByAddress
    ) {
        Set<String> couriers = new LinkedHashSet<>();
        for (String address : addresses) {
            Set<String> addressCouriers = contextualCouriersByAddress.get(contextualAddressKey(address));
            if (addressCouriers != null) {
                couriers.addAll(addressCouriers);
            }
        }
        return couriers.size() == 1 ? couriers.iterator().next() : null;
    }

    private String findNearestPageCourier(
        int pageIndex,
        Map<Integer, Set<String>> contextualCouriersByPage
    ) {
        int nearestDistance = Integer.MAX_VALUE;
        String nearestCourier = null;

        for (Map.Entry<Integer, Set<String>> entry : contextualCouriersByPage.entrySet()) {
            if (entry.getValue().size() != 1) {
                continue;
            }
            int distance = Math.abs(entry.getKey() - pageIndex);
            if (distance < nearestDistance || (distance == nearestDistance && entry.getKey() < pageIndex)) {
                nearestDistance = distance;
                nearestCourier = entry.getValue().iterator().next();
            }
        }
        return nearestCourier;
    }

    private String findDominantCourier(Map<Integer, Set<String>> contextualCouriersByPage) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Set<String> couriers : contextualCouriersByPage.values()) {
            if (couriers.size() == 1) {
                counts.merge(couriers.iterator().next(), 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private String contextualAddressKey(String address) {
        CourierAddressKey key = CourierAddressKey.from(address);
        String localityKey = key.localityFullHouseKey();
        return localityKey.isEmpty() ? key.fullHouseKey() : localityKey;
    }

    private record PendingPaymentDocument(
        int pageIndex,
        String address,
        List<String> registryAddresses,
        int paymentDocumentIndex,
        int paymentDocumentsOnPage,
        int registryPaymentDocumentsCount
    ) {
    }

    private int getNextPaymentPageIndex(int pageIndex, boolean duplexPrinting) {
        return pageIndex + (duplexPrinting ? 2 : 1);
    }

    private void registerUnmatchedAddress(
        String address,
        Path pdfFile,
        ProcessingStats stats,
        UnmatchedAddressRegistry unmatchedAddressRegistry,
        int paymentDocumentsCount
    ) {
        stats.addUnmatchedPages(paymentDocumentsCount);

        if (unmatchedAddressRegistry.add(address, pdfFile)) {
            stats.addUnmatchedPagesWrittenToReport(paymentDocumentsCount);
        } else {
            stats.addUnmatchedPagesSkippedFromReport(paymentDocumentsCount);
        }
    }

    private List<Path> savePaymentPages(
        PDDocument sourceDocument,
        int pageIndex,
        int pageCount,
        Path tempRoot,
        String courierName,
        int paymentDocumentIndex,
        int paymentDocumentsOnPage,
        boolean duplexPrinting
    ) throws IOException {
        List<Path> pageFiles = new ArrayList<>();

        pageFiles.add(savePaymentPage(sourceDocument, pageIndex, tempRoot, courierName, paymentDocumentIndex, paymentDocumentsOnPage));

        if (duplexPrinting) {
            int backPageIndex = pageIndex + 1;

            if (backPageIndex < pageCount) {
                pageFiles.add(savePaymentPage(sourceDocument, backPageIndex, tempRoot, courierName, paymentDocumentIndex, paymentDocumentsOnPage));
            }
        }

        return pageFiles;
    }

    private Path savePaymentPage(
        PDDocument sourceDocument,
        int pageIndex,
        Path tempRoot,
        String courierName,
        int paymentDocumentIndex,
        int paymentDocumentsOnPage
    ) throws IOException {
        if (paymentDocumentsOnPage == 1) {
            return tempStorage.savePageToTempPdf(sourceDocument, pageIndex, tempRoot, courierName);
        }

        return tempStorage.savePagePartToTempPdf(
            sourceDocument,
            pageIndex,
            tempRoot,
            courierName,
            paymentDocumentIndex,
            paymentDocumentsOnPage
        );
    }

}
