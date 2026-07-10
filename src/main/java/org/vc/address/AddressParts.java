package org.vc.address;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Выделяет из адреса улицу, номер дома и буквенный суффикс.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class AddressParts {

    private static final Pattern HOUSE_PATTERN = Pattern.compile(
        "^(.*?)(?:,?\\s*\\bд\\.?\\s*)((?:\\d+)(?:(?!(?:к(?=орп)|б(?=лок)))[а-яА-Яa-zA-Z])?(?:(/|-)\\d+[а-яА-Яa-zA-Z]?)?|[а-яА-Яa-zA-Z]).*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CORPUS_PATTERN = Pattern.compile(
        "(?iu)(?:\\b(?:корпус|корп\\.?|к\\.?)\\s*([\\dа-яa-z]+)|(?:^|,)\\s*([\\dа-яa-z]+)\\s*(?:корпус|корп\\.?|к\\.?))(?=$|[\\s,])"
    );

    private static final Pattern FLAT_PATTERN = Pattern.compile(
        "(?iu)\\bкв\\.?\\s*(\\d+)\\b"
    );

    private final String street;
    private final int houseNumber;
    private final String houseSuffix;
    private final String corpus;
    private final int flatNumber;

    private AddressParts(String street, int houseNumber, String houseSuffix, String corpus, int flatNumber) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.houseSuffix = houseSuffix;
        this.corpus = corpus;
        this.flatNumber = flatNumber;
    }

    /**
     * Разбирает адрес на улицу, номер дома и буквенный суффикс.
     */
    public static AddressParts parse(String address) {
        Matcher matcher = HOUSE_PATTERN.matcher(address.trim());

        if (!matcher.matches()) {
            return new AddressParts(address.trim(), Integer.MAX_VALUE, "", extractCorpus(address), extractFlatNumber(address));
        }

        String street = matcher.group(1).trim();
        String house = matcher.group(2);
        int houseNumber = parseHouseNumber(house);
        String houseSuffix = house.replaceFirst("^\\d+", "").trim();

        return new AddressParts(street, houseNumber, houseSuffix, extractCorpus(address), extractFlatNumber(address));
    }

    private static int parseHouseNumber(String house) {
        Matcher matcher = Pattern.compile("^\\d+").matcher(house);
        if (!matcher.find()) {
            return Integer.MAX_VALUE;
        }

        return Integer.parseInt(matcher.group());
    }

    private static String extractCorpus(String address) {
        Matcher matcher = CORPUS_PATTERN.matcher(address);

        if (!matcher.find()) {
            return "";
        }

        String corpus = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);

        return corpus == null ? "" : corpus.trim();
    }

    private static int extractFlatNumber(String address) {
        Matcher matcher = FLAT_PATTERN.matcher(address);

        if (!matcher.find()) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    
    public String getStreet() {
        return street;
    }

    
    public int getHouseNumber() {
        return houseNumber;
    }

    
    public String getHouseSuffix() {
        return houseSuffix;
    }

    
    public String getCorpus() {
        return corpus;
    }

    
    public int getFlatNumber() {
        return flatNumber;
    }
}
