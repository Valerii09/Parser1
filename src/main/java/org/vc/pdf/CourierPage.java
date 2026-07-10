package org.vc.pdf;

import org.vc.address.PaymentAddressParts;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Представляет одну платёжку курьера и связанные с ней физические PDF-страницы.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierPage {

    private final String address;
    private final List<Path> pageFiles;
    private final PaymentAddressParts addressParts;
    private final List<String> registryAddresses;

    /**
     * Создаёт платёжку с одной физической страницей и одним адресом в реестре.
     */
    public CourierPage(String address, Path pageFile) {
        this(address, List.of(pageFile));
    }

    /**
     * Создаёт платёжку с несколькими физическими страницами и одним адресом в реестре.
     */
    public CourierPage(String address, List<Path> pageFiles) {
        this(address, pageFiles, List.of(address));
    }

    /**
     * Создаёт платёжку, в которой несколько лицевых счетов относятся к одному адресу.
     */
    public CourierPage(String address, List<Path> pageFiles, int paymentDocumentsCount) {
        this(
            address,
            pageFiles,
            Collections.nCopies(Math.max(paymentDocumentsCount, 1), address)
        );
    }

    /**
     * Создаёт платёжку с отдельным адресом сортировки и полным списком адресов для реестра.
     *
     * <p>Первый аргумент определяет курьера и положение платёжки в итоговом PDF.
     * Все элементы {@code registryAddresses} учитываются при подсчёте лицевых счетов
     * в реестре адресов.</p>
     */
    public CourierPage(String address, List<Path> pageFiles, List<String> registryAddresses) {
        this.address = address;
        this.pageFiles = List.copyOf(pageFiles);
        this.addressParts = PaymentAddressParts.parse(address);
        this.registryAddresses = registryAddresses == null || registryAddresses.isEmpty()
            ? List.of(address)
            : List.copyOf(registryAddresses);
    }

    /**
     * Возвращает адрес, по которому платёжка назначается курьеру и сортируется.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Возвращает первую физическую страницу платёжки.
     */
    public Path getPageFile() {
        return pageFiles.get(0);
    }

    /**
     * Возвращает все физические страницы платёжки.
     */
    public List<Path> getPageFiles() {
        return pageFiles;
    }

    /**
     * Возвращает количество физических PDF-страниц, относящихся к одной платёжке.
     */
    public int getPhysicalPagesCount() {
        return pageFiles.size();
    }

    public int getPaymentDocumentsCount() {
        return registryAddresses.size();
    }

    /**
     * Возвращает все адреса, которые должны попасть в реестр.
     */
    public List<String> getRegistryAddresses() {
        return registryAddresses;
    }

    /**
     * Возвращает разобранные части адреса сортировки.
     */
    public PaymentAddressParts getAddressParts() {
        return addressParts;
    }
}
