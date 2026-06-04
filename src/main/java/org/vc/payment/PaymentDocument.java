package org.vc.payment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Платёжный документ, прочитанный из одного XML-узла {@code Kvitanc}.
 *
 * @author Valerii Trufanov
 * @since 03.06.2026
 */
public class PaymentDocument {

    private String account = "";
    private String payerName = "";
    private String address = "";
    private String postalIndex = "";
    private String cityType = "";
    private String city = "";
    private String streetType = "";
    private String street = "";
    private String house = "";
    private String corpus = "";
    private String flat = "";
    private String period = "";
    private String paymentDueDate = "";
    private String agent = "";
    private String agentAddress = "";
    private String agentPhone = "";
    private String firstQrDescription = "";
    private String secondQrDescription = "";
    private String residentsCount = "";
    private String roomsCount = "";
    private String flatArea = "";
    private String houseArea = "";
    private String commonElectricArea = "";
    private String commonHotWaterArea = "";
    private String nonResidentialArea = "";
    private String electricCommonShare = "";
    private String hotWaterCommonShare = "";
    private String debtAmount = "";
    private String chargeAmount = "";
    private String paidAmount = "";
    private String totalAmount = "";

    private final List<PaymentRecipient> recipients = new ArrayList<>();
    private final List<PaymentCharge> charges = new ArrayList<>();
    private final List<PaymentMeter> meters = new ArrayList<>();
    private final List<PaymentQrCode> qrCodes = new ArrayList<>();

    
    public String getAccount() {
        return account;
    }

    /**
     * Устанавливает значение.
     *
     * @param account новое значение
     */
    public void setAccount(String account) {
        this.account = value(account);
    }

    
    public String getPayerName() {
        return payerName;
    }

    /**
     * Устанавливает значение.
     *
     * @param payerName новое значение
     */
    public void setPayerName(String payerName) {
        this.payerName = value(payerName);
    }

    
    public String getAddress() {
        if (!address.isBlank()) {
            return extractPremiseAddress(address);
        }

        return formatStructuredAddress();
    }

    /**
     * Устанавливает значение.
     *
     * @param address новое значение
     */
    public void setAddress(String address) {
        this.address = value(address);
    }

    
    public String getSourceAddress() {
        if (!address.isBlank()) {
            return address;
        }

        return formatStructuredAddress();
    }

    /**
     * Устанавливает значение.
     *
     * @param postalIndex новое значение
     */
    public void setPostalIndex(String postalIndex) {
        this.postalIndex = value(postalIndex);
    }

    /**
     * Устанавливает значение.
     *
     * @param cityType новое значение
     */
    public void setCityType(String cityType) {
        this.cityType = value(cityType);
    }

    /**
     * Устанавливает значение.
     *
     * @param city новое значение
     */
    public void setCity(String city) {
        this.city = value(city);
    }

    /**
     * Устанавливает значение.
     *
     * @param streetType новое значение
     */
    public void setStreetType(String streetType) {
        this.streetType = value(streetType);
    }

    /**
     * Устанавливает значение.
     *
     * @param street новое значение
     */
    public void setStreet(String street) {
        this.street = value(street);
    }

    /**
     * Устанавливает значение.
     *
     * @param house новое значение
     */
    public void setHouse(String house) {
        this.house = value(house);
    }

    /**
     * Устанавливает значение.
     *
     * @param corpus новое значение
     */
    public void setCorpus(String corpus) {
        this.corpus = value(corpus);
    }

    /**
     * Устанавливает значение.
     *
     * @param flat новое значение
     */
    public void setFlat(String flat) {
        this.flat = value(flat);
    }

    
    public String getPeriod() {
        return period;
    }

    /**
     * Устанавливает значение.
     *
     * @param period новое значение
     */
    public void setPeriod(String period) {
        this.period = value(period);
    }

    
    public String getPaymentDueDate() {
        return paymentDueDate;
    }

    /**
     * Устанавливает значение.
     *
     * @param paymentDueDate новое значение
     */
    public void setPaymentDueDate(String paymentDueDate) {
        this.paymentDueDate = value(paymentDueDate);
    }

    
    public String getAgent() {
        return agent;
    }

    /**
     * Устанавливает значение.
     *
     * @param agent новое значение
     */
    public void setAgent(String agent) {
        this.agent = value(agent);
    }

    
    public String getAgentAddress() {
        return agentAddress;
    }

    /**
     * Устанавливает значение.
     *
     * @param agentAddress новое значение
     */
    public void setAgentAddress(String agentAddress) {
        this.agentAddress = value(agentAddress);
    }

    
    public String getAgentPhone() {
        return agentPhone;
    }

    /**
     * Устанавливает значение.
     *
     * @param agentPhone новое значение
     */
    public void setAgentPhone(String agentPhone) {
        this.agentPhone = value(agentPhone);
    }

    
    public String getFirstQrDescription() {
        return firstQrDescription;
    }

    /**
     * Устанавливает значение.
     *
     * @param firstQrDescription новое значение
     */
    public void setFirstQrDescription(String firstQrDescription) {
        this.firstQrDescription = value(firstQrDescription);
    }

    
    public String getSecondQrDescription() {
        return secondQrDescription;
    }

    /**
     * Устанавливает значение.
     *
     * @param secondQrDescription новое значение
     */
    public void setSecondQrDescription(String secondQrDescription) {
        this.secondQrDescription = value(secondQrDescription);
    }

    
    public String getResidentsCount() {
        return residentsCount;
    }

    /**
     * Устанавливает значение.
     *
     * @param residentsCount новое значение
     */
    public void setResidentsCount(String residentsCount) {
        this.residentsCount = value(residentsCount);
    }

    
    public String getRoomsCount() {
        return roomsCount;
    }

    /**
     * Устанавливает значение.
     *
     * @param roomsCount новое значение
     */
    public void setRoomsCount(String roomsCount) {
        this.roomsCount = value(roomsCount);
    }

    
    public String getFlatArea() {
        return flatArea;
    }

    /**
     * Устанавливает значение.
     *
     * @param flatArea новое значение
     */
    public void setFlatArea(String flatArea) {
        this.flatArea = value(flatArea);
    }

    
    public String getHouseArea() {
        return houseArea;
    }

    /**
     * Устанавливает значение.
     *
     * @param houseArea новое значение
     */
    public void setHouseArea(String houseArea) {
        this.houseArea = value(houseArea);
    }

    
    public String getCommonElectricArea() {
        return commonElectricArea;
    }

    /**
     * Устанавливает значение.
     *
     * @param commonElectricArea новое значение
     */
    public void setCommonElectricArea(String commonElectricArea) {
        this.commonElectricArea = value(commonElectricArea);
    }

    
    public String getCommonHotWaterArea() {
        return commonHotWaterArea;
    }

    /**
     * Устанавливает значение.
     *
     * @param commonHotWaterArea новое значение
     */
    public void setCommonHotWaterArea(String commonHotWaterArea) {
        this.commonHotWaterArea = value(commonHotWaterArea);
    }

    
    public String getNonResidentialArea() {
        return nonResidentialArea;
    }

    /**
     * Устанавливает значение.
     *
     * @param nonResidentialArea новое значение
     */
    public void setNonResidentialArea(String nonResidentialArea) {
        this.nonResidentialArea = value(nonResidentialArea);
    }

    
    public String getElectricCommonShare() {
        return electricCommonShare;
    }

    /**
     * Устанавливает значение.
     *
     * @param electricCommonShare новое значение
     */
    public void setElectricCommonShare(String electricCommonShare) {
        this.electricCommonShare = value(electricCommonShare);
    }

    
    public String getHotWaterCommonShare() {
        return hotWaterCommonShare;
    }

    /**
     * Устанавливает значение.
     *
     * @param hotWaterCommonShare новое значение
     */
    public void setHotWaterCommonShare(String hotWaterCommonShare) {
        this.hotWaterCommonShare = value(hotWaterCommonShare);
    }

    
    public String getDebtAmount() {
        return debtAmount;
    }

    /**
     * Устанавливает значение.
     *
     * @param debtAmount новое значение
     */
    public void setDebtAmount(String debtAmount) {
        this.debtAmount = value(debtAmount);
    }

    
    public String getChargeAmount() {
        return chargeAmount;
    }

    /**
     * Устанавливает значение.
     *
     * @param chargeAmount новое значение
     */
    public void setChargeAmount(String chargeAmount) {
        this.chargeAmount = value(chargeAmount);
    }

    
    public String getPaidAmount() {
        return paidAmount;
    }

    /**
     * Устанавливает значение.
     *
     * @param paidAmount новое значение
     */
    public void setPaidAmount(String paidAmount) {
        this.paidAmount = value(paidAmount);
    }

    
    public String getTotalAmount() {
        return totalAmount;
    }

    /**
     * Устанавливает значение.
     *
     * @param totalAmount новое значение
     */
    public void setTotalAmount(String totalAmount) {
        this.totalAmount = value(totalAmount);
    }

    
    public List<PaymentRecipient> getRecipients() {
        return recipients;
    }

    
    public List<PaymentCharge> getCharges() {
        return charges;
    }

    
    public List<PaymentMeter> getMeters() {
        return meters;
    }

    
    public List<PaymentQrCode> getQrCodes() {
        return qrCodes;
    }

    
    public void addRecipient(PaymentRecipient recipient) {
        recipients.add(recipient);
    }

    
    public void addCharge(PaymentCharge charge) {
        charges.add(charge);
    }

    
    public void addMeter(PaymentMeter meter) {
        meters.add(meter);
    }

    
    public void addQrCode(PaymentQrCode qrCode) {
        if (qrCode.getText() == null || qrCode.getText().isBlank()) {
            return;
        }

        qrCodes.add(qrCode);
    }

    private String extractPremiseAddress(String sourceAddress) {
        String lowerAddress = sourceAddress.toLowerCase(Locale.ROOT);
        String marker = "адрес помещения:";
        int markerIndex = lowerAddress.indexOf(marker);

        if (markerIndex < 0) {
            return sourceAddress;
        }

        String premiseAddress = sourceAddress.substring(markerIndex + marker.length()).trim();
        int bracketIndex = premiseAddress.indexOf(')');

        if (bracketIndex >= 0) {
            premiseAddress = premiseAddress.substring(0, bracketIndex).trim();
        }

        return premiseAddress;
    }

    private String formatStructuredAddress() {
        StringBuilder result = new StringBuilder();

        appendPart(result, postalIndex);
        appendPart(result, cityType + " " + city);
        appendPart(result, streetType + " " + street);
        appendPart(result, "дом № " + house);

        if (!corpus.isBlank()) {
            appendPart(result, "корпус " + corpus);
        }

        if (!flat.isBlank()) {
            appendPart(result, "кв." + flat);
        }

        return result.toString();
    }

    private void appendPart(StringBuilder result, String part) {
        String preparedPart = value(part);

        if (preparedPart.isBlank()) {
            return;
        }

        if (!result.isEmpty()) {
            result.append(", ");
        }

        result.append(preparedPart);
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
