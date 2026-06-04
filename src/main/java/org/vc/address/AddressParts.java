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
        "^(.*?)(?:,?\\s*д\\.?\\s*)(\\d+)([а-яА-Яa-zA-Z]?).*$",
        Pattern.CASE_INSENSITIVE
    );

    private final String street;
    private final int houseNumber;
    private final String houseLetter;

    private AddressParts(String street, int houseNumber, String houseLetter) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.houseLetter = houseLetter;
    }

    /**
     * Разбирает адрес на улицу, номер дома и буквенный суффикс.
     */
    public static AddressParts parse(String address) {
        Matcher matcher = HOUSE_PATTERN.matcher(address.trim());

        if (!matcher.matches()) {
            return new AddressParts(address.trim(), Integer.MAX_VALUE, "");
        }

        String street = matcher.group(1).trim();
        int houseNumber = Integer.parseInt(matcher.group(2));
        String houseLetter = matcher.group(3) == null ? "" : matcher.group(3).trim();

        return new AddressParts(street, houseNumber, houseLetter);
    }

    
    public String getStreet() {
        return street;
    }

    
    public int getHouseNumber() {
        return houseNumber;
    }

    
    public String getHouseLetter() {
        return houseLetter;
    }
}
