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

    public void setAccount(String account) {
        this.account = value(account);
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = value(payerName);
    }

    public String getAddress() {
        if (!address.isBlank()) {
            return extractPremiseAddress(address);
        }

        return formatStructuredAddress();
    }

    public void setAddress(String address) {
        this.address = value(address);
    }

    public String getSourceAddress() {
        if (!address.isBlank()) {
            return address;
        }

        return formatStructuredAddress();
    }

    public void setPostalIndex(String postalIndex) {
        this.postalIndex = value(postalIndex);
    }

    public void setCityType(String cityType) {
        this.cityType = value(cityType);
    }

    public void setCity(String city) {
        this.city = value(city);
    }

    public void setStreetType(String streetType) {
        this.streetType = value(streetType);
    }

    public void setStreet(String street) {
        this.street = value(street);
    }

    public void setHouse(String house) {
        this.house = value(house);
    }

    public void setCorpus(String corpus) {
        this.corpus = value(corpus);
    }

    public void setFlat(String flat) {
        this.flat = value(flat);
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = value(period);
    }

    public String getPaymentDueDate() {
        return paymentDueDate;
    }

    public void setPaymentDueDate(String paymentDueDate) {
        this.paymentDueDate = value(paymentDueDate);
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = value(agent);
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = value(agentAddress);
    }

    public String getAgentPhone() {
        return agentPhone;
    }

    public void setAgentPhone(String agentPhone) {
        this.agentPhone = value(agentPhone);
    }

    public String getFirstQrDescription() {
        return firstQrDescription;
    }

    public void setFirstQrDescription(String firstQrDescription) {
        this.firstQrDescription = value(firstQrDescription);
    }

    public String getSecondQrDescription() {
        return secondQrDescription;
    }

    public void setSecondQrDescription(String secondQrDescription) {
        this.secondQrDescription = value(secondQrDescription);
    }

    public String getResidentsCount() {
        return residentsCount;
    }

    public void setResidentsCount(String residentsCount) {
        this.residentsCount = value(residentsCount);
    }

    public String getRoomsCount() {
        return roomsCount;
    }

    public void setRoomsCount(String roomsCount) {
        this.roomsCount = value(roomsCount);
    }

    public String getFlatArea() {
        return flatArea;
    }

    public void setFlatArea(String flatArea) {
        this.flatArea = value(flatArea);
    }

    public String getHouseArea() {
        return houseArea;
    }

    public void setHouseArea(String houseArea) {
        this.houseArea = value(houseArea);
    }

    public String getCommonElectricArea() {
        return commonElectricArea;
    }

    public void setCommonElectricArea(String commonElectricArea) {
        this.commonElectricArea = value(commonElectricArea);
    }

    public String getCommonHotWaterArea() {
        return commonHotWaterArea;
    }

    public void setCommonHotWaterArea(String commonHotWaterArea) {
        this.commonHotWaterArea = value(commonHotWaterArea);
    }

    public String getNonResidentialArea() {
        return nonResidentialArea;
    }

    public void setNonResidentialArea(String nonResidentialArea) {
        this.nonResidentialArea = value(nonResidentialArea);
    }

    public String getElectricCommonShare() {
        return electricCommonShare;
    }

    public void setElectricCommonShare(String electricCommonShare) {
        this.electricCommonShare = value(electricCommonShare);
    }

    public String getHotWaterCommonShare() {
        return hotWaterCommonShare;
    }

    public void setHotWaterCommonShare(String hotWaterCommonShare) {
        this.hotWaterCommonShare = value(hotWaterCommonShare);
    }

    public String getDebtAmount() {
        return debtAmount;
    }

    public void setDebtAmount(String debtAmount) {
        this.debtAmount = value(debtAmount);
    }

    public String getChargeAmount() {
        return chargeAmount;
    }

    public void setChargeAmount(String chargeAmount) {
        this.chargeAmount = value(chargeAmount);
    }

    public String getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(String paidAmount) {
        this.paidAmount = value(paidAmount);
    }

    public String getTotalAmount() {
        return totalAmount;
    }

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
