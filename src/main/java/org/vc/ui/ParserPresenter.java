package org.vc.ui;

import org.vc.address.PaymentSupplier;
import org.vc.excel.CourierExcelReader;
import org.vc.repository.CourierAddressWriter;
import org.vc.service.CourierPaymentService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Управляет пользовательскими сценариями главного окна и не зависит от Swing-компонентов напрямую.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class ParserPresenter {

    private final ParserView view;
    private final ParserTaskRunner taskRunner;
    private final Path couriersRoot;
    private final CourierExcelReader courierExcelReader;
    private final CourierAddressWriter courierAddressWriter;
    private final CourierPaymentService courierPaymentService;

    
    public ParserPresenter(
        ParserView view,
        ParserTaskRunner taskRunner,
        Path couriersRoot
    ) {
        this(
            view,
            taskRunner,
            couriersRoot,
            new CourierExcelReader(),
            new CourierAddressWriter(),
            new CourierPaymentService()
        );
    }

    ParserPresenter(
        ParserView view,
        ParserTaskRunner taskRunner,
        Path couriersRoot,
        CourierExcelReader courierExcelReader,
        CourierAddressWriter courierAddressWriter,
        CourierPaymentService courierPaymentService
    ) {
        this.view = view;
        this.taskRunner = taskRunner;
        this.couriersRoot = couriersRoot;
        this.courierExcelReader = courierExcelReader;
        this.courierAddressWriter = courierAddressWriter;
        this.courierPaymentService = courierPaymentService;
    }

    
    public void clearLog() {
        view.clearLog();
    }

    
    public void createCourierAddresses() {
        taskRunner.run(() -> {
            Path excelPath = getRequiredPath(
                view.getSelectedExcelPath(),
                "Выберите Excel-файл с адресами"
            );

            if (!Files.exists(excelPath)) {
                throw new IllegalStateException("Excel-файл не найден: " + excelPath);
            }

            System.out.println("Формирование адресов курьеров...");
            System.out.println("Excel: " + excelPath);
            System.out.println("Папка результата: " + couriersRoot);

            Map<String, Set<String>> courierAddresses = courierExcelReader.readCourierAddresses(excelPath);
            courierAddressWriter.write(couriersRoot, courierAddresses);

            System.out.println("Адреса курьеров сформированы");
        });
    }

    
    public void createCourierPdfs() {
        taskRunner.run(() -> {
            Path pdfFolder = getRequiredPath(
                view.getSelectedPdfFolderPath(),
                "Выберите папку с PDF"
            );

            if (!Files.exists(pdfFolder)) {
                throw new IllegalStateException("Папка с PDF не найдена: " + pdfFolder);
            }

            System.out.println("Формирование PDF по курьерам...");
            System.out.println("Папка с PDF: " + pdfFolder);
            System.out.println("Папка курьеров: " + couriersRoot);

            refreshCourierAddressesIfExcelSelected();

            boolean duplexPrinting = view.isDuplexPrintingSelected();
            PaymentSupplier supplier = view.getSelectedPaymentSupplier();

            System.out.println("Двусторонняя печать: " + (duplexPrinting ? "да" : "нет"));
            System.out.println("Поставщик PDF: " + supplier.getDisplayName());

            courierPaymentService.process(pdfFolder, couriersRoot, duplexPrinting, supplier);

            System.out.println("PDF по курьерам сформированы");
        });
    }

    private void refreshCourierAddressesIfExcelSelected() throws Exception {
        String excelPathValue = view.getSelectedExcelPath();
        if (excelPathValue == null || excelPathValue.isBlank()) {
            return;
        }

        Path excelPath = Paths.get(excelPathValue.trim());
        if (!Files.exists(excelPath)) {
            throw new IllegalStateException("Excel-файл не найден: " + excelPath);
        }

        System.out.println("Обновление адресов курьеров из Excel перед сортировкой...");
        System.out.println("Excel: " + excelPath);

        Map<String, Set<String>> courierAddresses = courierExcelReader.readCourierAddresses(excelPath);
        courierAddressWriter.write(couriersRoot, courierAddresses);

        System.out.println("Адреса курьеров обновлены из Excel");
    }

    private Path getRequiredPath(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(errorMessage);
        }

        return Paths.get(value.trim());
    }
}
