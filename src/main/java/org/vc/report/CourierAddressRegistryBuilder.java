package org.vc.report;

import org.vc.address.PaymentAddressParts;
import org.vc.pdf.CourierPage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Собирает реестр адресов из уже отсортированных платёжек курьера.
 *
 * @author Valerii Trufanov
 * @since 02.06.2026
 */
public class CourierAddressRegistryBuilder {

    /**
     * Группирует платёжки по адресу дома и считает количество ЛС/ПД на каждом адресе.
     */
    public List<CourierAddressRegistryRow> build(String courierName, List<CourierPage> pages) {
        Map<String, Integer> paymentCountByAddress = new LinkedHashMap<>();

        for (CourierPage page : pages) {
            for (String registryAddress : page.getRegistryAddresses()) {
                paymentCountByAddress.merge(getHouseAddress(registryAddress), 1, Integer::sum);
            }
        }

        List<CourierAddressRegistryRow> rows = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : paymentCountByAddress.entrySet()) {
            rows.add(new CourierAddressRegistryRow(entry.getKey(), entry.getValue(), courierName));
        }

        return rows;
    }

    private String getHouseAddress(String sourceAddress) {
        PaymentAddressParts addressParts = PaymentAddressParts.parse(sourceAddress);

        if (addressParts.isEmpty()) {
            return sourceAddress;
        }

        StringBuilder address = new StringBuilder();

        if (!addressParts.getCity().isBlank()) {
            address.append("г.").append(addressParts.getCity()).append(", ");
        }

        address.append(addressParts.getStreet())
            .append(", д.")
            .append(addressParts.getHouseNumber())
            .append(addressParts.getHouseLetter());

        if (!addressParts.getCorpus().isBlank()) {
            address.append(", к.").append(addressParts.getCorpus());
        }

        return address.toString();
    }
}
