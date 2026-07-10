package org.vc.address;

import java.util.Arrays;

/**
 * Поставщик PDF-платёжек.
 * От поставщика зависит, где искать адрес и нужно ли обрабатывать все адреса на странице
 * или только первый адрес.
 */
public enum PaymentSupplier {

    AUTO("Авто", false),
    YAROBLVODOKANAL("Яроблводоканал", false),
    FACTORIAL("Факториал", true);

    private final String displayName;
    private final boolean firstAddressOnly;

    PaymentSupplier(String displayName, boolean firstAddressOnly) {
        this.displayName = displayName;
        this.firstAddressOnly = firstAddressOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFirstAddressOnly() {
        return firstAddressOnly;
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
