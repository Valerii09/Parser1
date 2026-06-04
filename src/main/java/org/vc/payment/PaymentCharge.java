package org.vc.payment;

/**
 * Строка начисления услуги внутри платёжки из XML.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class PaymentCharge {

    private final String serviceName;
    private final String unit;
    private final String volume;
    private final String communalVolume;
    private final String tariff;
    private final String individualAmount;
    private final String communalAmount;
    private final String chargedAmount;
    private final String recalculationCode;
    private final String recalculationReason;
    private final String recalculationAmount;
    private final String debtAmount;
    private final String totalAmount;
    private final String executorName;
    private final String executorAddress;
    private final String executorInn;
    private final String executorKpp;

    
    public PaymentCharge(
        String serviceName,
        String unit,
        String volume,
        String tariff,
        String chargedAmount,
        String totalAmount
    ) {
        this(
            serviceName,
            unit,
            volume,
            "",
            tariff,
            chargedAmount,
            "",
            chargedAmount,
            "",
            "",
            "",
            "",
            totalAmount,
            "",
            "",
            "",
            ""
        );
    }

    
    public PaymentCharge(
        String serviceName,
        String unit,
        String volume,
        String communalVolume,
        String tariff,
        String individualAmount,
        String communalAmount,
        String chargedAmount,
        String recalculationCode,
        String recalculationReason,
        String recalculationAmount,
        String debtAmount,
        String totalAmount,
        String executorName,
        String executorAddress,
        String executorInn,
        String executorKpp
    ) {
        this.serviceName = serviceName;
        this.unit = unit;
        this.volume = volume;
        this.communalVolume = communalVolume;
        this.tariff = tariff;
        this.individualAmount = individualAmount;
        this.communalAmount = communalAmount;
        this.chargedAmount = chargedAmount;
        this.recalculationCode = recalculationCode;
        this.recalculationReason = recalculationReason;
        this.recalculationAmount = recalculationAmount;
        this.debtAmount = debtAmount;
        this.totalAmount = totalAmount;
        this.executorName = executorName;
        this.executorAddress = executorAddress;
        this.executorInn = executorInn;
        this.executorKpp = executorKpp;
    }

    
    public String getServiceName() {
        return serviceName;
    }

    
    public String getUnit() {
        return unit;
    }

    
    public String getVolume() {
        return volume;
    }

    
    public String getCommunalVolume() {
        return communalVolume;
    }

    
    public String getTariff() {
        return tariff;
    }

    
    public String getIndividualAmount() {
        return individualAmount;
    }

    
    public String getCommunalAmount() {
        return communalAmount;
    }

    
    public String getChargedAmount() {
        return chargedAmount;
    }

    
    public String getRecalculationCode() {
        return recalculationCode;
    }

    
    public String getRecalculationReason() {
        return recalculationReason;
    }

    
    public String getRecalculationAmount() {
        return recalculationAmount;
    }

    
    public String getDebtAmount() {
        return debtAmount;
    }

    
    public String getTotalAmount() {
        return totalAmount;
    }

    
    public String getExecutorName() {
        return executorName;
    }

    
    public String getExecutorAddress() {
        return executorAddress;
    }

    
    public String getExecutorInn() {
        return executorInn;
    }

    
    public String getExecutorKpp() {
        return executorKpp;
    }
}
