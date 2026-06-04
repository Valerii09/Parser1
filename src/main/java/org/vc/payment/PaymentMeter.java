package org.vc.payment;

/**
 * Строка прибора учёта внутри платёжки из XML.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class PaymentMeter {

    private final String serviceName;
    private final String unit;
    private final String meterNumber;
    private final String previousValue;
    private final String currentValue;
    private final String consumption;
    private final String communalMeterNumber;
    private final String communalPreviousValue;
    private final String communalCurrentValue;
    private final String communalConsumption;
    private final String housePremiseVolume;
    private final String houseCommonVolume;
    private final String individualNorm;
    private final String increasingRatio;
    private final String communalNorm;

    public PaymentMeter(
        String serviceName,
        String unit,
        String meterNumber,
        String previousValue,
        String currentValue,
        String consumption,
        String communalMeterNumber,
        String communalPreviousValue,
        String communalCurrentValue,
        String communalConsumption,
        String housePremiseVolume,
        String houseCommonVolume,
        String individualNorm,
        String increasingRatio,
        String communalNorm
    ) {
        this.serviceName = serviceName;
        this.unit = unit;
        this.meterNumber = meterNumber;
        this.previousValue = previousValue;
        this.currentValue = currentValue;
        this.consumption = consumption;
        this.communalMeterNumber = communalMeterNumber;
        this.communalPreviousValue = communalPreviousValue;
        this.communalCurrentValue = communalCurrentValue;
        this.communalConsumption = communalConsumption;
        this.housePremiseVolume = housePremiseVolume;
        this.houseCommonVolume = houseCommonVolume;
        this.individualNorm = individualNorm;
        this.increasingRatio = increasingRatio;
        this.communalNorm = communalNorm;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUnit() {
        return unit;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public String getConsumption() {
        return consumption;
    }

    public String getCommunalMeterNumber() {
        return communalMeterNumber;
    }

    public String getCommunalPreviousValue() {
        return communalPreviousValue;
    }

    public String getCommunalCurrentValue() {
        return communalCurrentValue;
    }

    public String getCommunalConsumption() {
        return communalConsumption;
    }

    public String getHousePremiseVolume() {
        return housePremiseVolume;
    }

    public String getHouseCommonVolume() {
        return houseCommonVolume;
    }

    public String getIndividualNorm() {
        return individualNorm;
    }

    public String getIncreasingRatio() {
        return increasingRatio;
    }

    public String getCommunalNorm() {
        return communalNorm;
    }
}
