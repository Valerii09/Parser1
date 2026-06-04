package org.vc.report;

import java.util.Objects;

/**
 * Хранит структурированный адрес, который не был назначен курьеру.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class UnmatchedAddress {

    private final String city;
    private final String street;
    private final String houseNumber;
    private final String corpus;
    private final String index;

    
    public UnmatchedAddress(String city, String street, String houseNumber, String corpus) {
        this(city, street, houseNumber, corpus, "");
    }

    
    public UnmatchedAddress(String city, String street, String houseNumber, String corpus, String index) {
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.corpus = corpus;
        this.index = index;
    }

    
    public String getCity() {
        return city;
    }

    
    public String getStreet() {
        return street;
    }

    
    public String getHouseNumber() {
        return houseNumber;
    }

    
    public String getCorpus() {
        return corpus;
    }

    
    public String getIndex() {
        return index;
    }

    
    public String getFullAddress() {
        StringBuilder result = new StringBuilder();

        result.append(street)
            .append(" ")
            .append(houseNumber);

        if (corpus != null && !corpus.isBlank()) {
            result.append(" к. ").append(corpus);
        }

        return result.toString();
    }

    
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UnmatchedAddress that)) {
            return false;
        }
        return Objects.equals(city, that.city)
            && Objects.equals(street, that.street)
            && Objects.equals(houseNumber, that.houseNumber)
            && Objects.equals(corpus, that.corpus)
            && Objects.equals(index, that.index);
    }

    
    @Override
    public int hashCode() {
        return Objects.hash(city, street, houseNumber, corpus, index);
    }
}
