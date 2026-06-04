package org.vc.pdf;

import org.vc.address.PaymentAddressParts;

import java.util.Comparator;

/**
 * Сортирует платёжки курьеров по адресу и номеру квартиры.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class CourierPageComparator implements Comparator<CourierPage> {

    @Override
    public int compare(CourierPage firstPage, CourierPage secondPage) {
        PaymentAddressParts first = firstPage.getAddressParts();
        PaymentAddressParts second = secondPage.getAddressParts();

        int streetCompare = first.getStreet().compareToIgnoreCase(second.getStreet());
        if (streetCompare != 0) {
            return streetCompare;
        }

        int houseNumberCompare = Integer.compare(first.getHouseNumber(), second.getHouseNumber());
        if (houseNumberCompare != 0) {
            return houseNumberCompare;
        }

        int houseLetterCompare = first.getHouseLetter().compareToIgnoreCase(second.getHouseLetter());
        if (houseLetterCompare != 0) {
            return houseLetterCompare;
        }

        int corpusCompare = first.getCorpus().compareToIgnoreCase(second.getCorpus());
        if (corpusCompare != 0) {
            return corpusCompare;
        }

        int flatNumberCompare = Integer.compare(first.getFlatNumber(), second.getFlatNumber());
        if (flatNumberCompare != 0) {
            return flatNumberCompare;
        }

        return firstPage.getAddress().compareToIgnoreCase(secondPage.getAddress());
    }
}
