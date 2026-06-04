package org.vc.payment;

/**
 * Получатель платежа внутри одной платёжки из XML.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class PaymentRecipient {

    private final String name;
    private final String account;
    private final String description;
    private final String amount;

    public PaymentRecipient(String name, String account, String description, String amount) {
        this.name = name;
        this.account = account;
        this.description = description;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public String getAccount() {
        return account;
    }

    public String getDescription() {
        return description;
    }

    public String getAmount() {
        return amount;
    }
}
