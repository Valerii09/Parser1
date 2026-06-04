package org.vc.pdf;

import org.vc.address.PaymentAddressParts;

import java.nio.file.Path;
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

    public CourierPage(String address, Path pageFile) {
        this(address, List.of(pageFile));
    }

    public CourierPage(String address, List<Path> pageFiles) {
        this.address = address;
        this.pageFiles = List.copyOf(pageFiles);
        this.addressParts = PaymentAddressParts.parse(address);
    }

    public String getAddress() {
        return address;
    }

    public Path getPageFile() {
        return pageFiles.get(0);
    }

    public List<Path> getPageFiles() {
        return pageFiles;
    }

    /**
     * Возвращает количество физических PDF-страниц, относящихся к одной платёжке.
     */
    public int getPhysicalPagesCount() {
        return pageFiles.size();
    }

    public PaymentAddressParts getAddressParts() {
        return addressParts;
    }
}
