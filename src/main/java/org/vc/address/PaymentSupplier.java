package org.vc.address;

import java.util.Arrays;

/**
 * Payment document supplier whose PDF layout defines where the delivery address is placed.
 */
public enum PaymentSupplier {

    AUTO("Авто"),
    YAROBLVODOKANAL("Яроблводоканал");

    private final String displayName;

    PaymentSupplier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentSupplier fromName(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }

        String preparedValue = value.trim();

        return Arrays.stream(values())
            .filter(supplier -> supplier.name().equalsIgnoreCase(preparedValue)
                || supplier.displayName.equalsIgnoreCase(preparedValue))
            .findFirst()
            .orElse(AUTO);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
