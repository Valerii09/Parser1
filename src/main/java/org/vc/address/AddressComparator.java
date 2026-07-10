package org.vc.address;

import java.util.Comparator;

/**
 * Компаратор для сортировки адресов по улице и номеру дома.
 *
 * @author Valerii Trufanov
 * @since 14.05.2026
 */
public class AddressComparator implements Comparator<String> {

    /**
     * Сравнивает адреса по улице, номеру дома и букве дома.
     */
    @Override
    public int compare(String firstAddress, String secondAddress) {
        AddressParts first = AddressParts.parse(firstAddress);
        AddressParts second = AddressParts.parse(secondAddress);

        int streetCompare = first.getStreet().compareToIgnoreCase(second.getStreet());
        if (streetCompare != 0) {
            return streetCompare;
        }

        int houseNumberCompare = Integer.compare(first.getHouseNumber(), second.getHouseNumber());
        if (houseNumberCompare != 0) {
            return houseNumberCompare;
        }

        int houseSuffixCompare = NaturalTextComparator.compare(first.getHouseSuffix(), second.getHouseSuffix());
        if (houseSuffixCompare != 0) {
            return houseSuffixCompare;
        }

        int corpusCompare = NaturalTextComparator.compare(first.getCorpus(), second.getCorpus());
        if (corpusCompare != 0) {
            return corpusCompare;
        }

        int flatNumberCompare = Integer.compare(first.getFlatNumber(), second.getFlatNumber());
        if (flatNumberCompare != 0) {
            return flatNumberCompare;
        }

        return NaturalTextComparator.compare(firstAddress, secondAddress);
    }
}
