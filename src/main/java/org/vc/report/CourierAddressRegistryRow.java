package org.vc.report;

/**
 * Строка реестра адресов, сформированная после сортировки платёжек по курьерам.
 *
 * @author Valerii Trufanov
 * @since 02.06.2026
 */
public class CourierAddressRegistryRow {

    private final String address;
    private final int paymentDocumentsCount;
    private final String courierName;

    
    public CourierAddressRegistryRow(String address, int paymentDocumentsCount, String courierName) {
        this.address = address;
        this.paymentDocumentsCount = paymentDocumentsCount;
        this.courierName = courierName;
    }

    
    public String getAddress() {
        return address;
    }

    
    public int getPaymentDocumentsCount() {
        return paymentDocumentsCount;
    }

    
    public String getCourierName() {
        return courierName;
    }
}
