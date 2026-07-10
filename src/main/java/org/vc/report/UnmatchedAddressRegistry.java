package org.vc.report;

import org.vc.address.PaymentAddressParts;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Собирает нераспределённые и неразобранные адреса для отчётности.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class UnmatchedAddressRegistry {

    private static final Pattern INDEX_PATTERN = Pattern.compile("^(\\d{6})\\b.*");
    private static final int MISSING_ADDRESS_TEXT_LIMIT = 1500;

    private final List<UnmatchedAddress> addresses = new LinkedList<>();
    private final List<String> unparsedAddresses = new LinkedList<>();
    private final List<String> missingAddressDocuments = new LinkedList<>();

    /**
     * Добавляет адрес без привязки к исходному файлу.
     */
    public boolean add(String rawAddress) {
        return add(rawAddress, null);
    }

    /**
     * Добавляет адрес и возвращает признак успешного разбора.
     */
    public boolean add(String rawAddress, Path pdfFile) {
        PaymentAddressParts addressParts = PaymentAddressParts.parse(rawAddress);

        if (addressParts.isEmpty()) {
            unparsedAddresses.add(formatUnparsedAddress(rawAddress, pdfFile));
            return false;
        }

        UnmatchedAddress address = new UnmatchedAddress(
            addressParts.getCity(),
            addressParts.getStreet(),
            getFullHouseNumber(addressParts),
            addressParts.getCorpus(),
            extractIndex(pdfFile)
        );

        addresses.add(address);
        return true;
    }

    /**
     * Запоминает платёжку, где адрес вообще не удалось найти в тексте PDF.
     */
    public void addMissingAddressDocument(Path pdfFile, int pageNumber, String pageText) {
        missingAddressDocuments.add(formatMissingAddressDocument(pdfFile, pageNumber, pageText));
    }

    
    public boolean isEmpty() {
        return addresses.isEmpty() && unparsedAddresses.isEmpty() && missingAddressDocuments.isEmpty();
    }

    
    public boolean hasMatchedUnassignedAddresses() {
        return !addresses.isEmpty();
    }

    
    public boolean hasUnparsedAddresses() {
        return !unparsedAddresses.isEmpty();
    }

    
    public boolean hasMissingAddressDocuments() {
        return !missingAddressDocuments.isEmpty();
    }

    
    public List<UnmatchedAddress> getAddresses() {
        return addresses;
    }

    
    public List<String> getUnparsedAddresses() {
        return unparsedAddresses;
    }

    
    public List<String> getMissingAddressDocuments() {
        return missingAddressDocuments;
    }

    /**
     * Возвращает количество квартир, учтённых для каждого нераспределённого адреса.
     */
    public Map<UnmatchedAddress, Integer> getFlatCountByAddress() {
        Map<UnmatchedAddress, Integer> result = new LinkedHashMap<>();

        for (UnmatchedAddress address : addresses) {
            result.merge(address, 1, Integer::sum);
        }

        return result;
    }

    private String getFullHouseNumber(PaymentAddressParts addressParts) {
        return addressParts.getHouseNumber() + addressParts.getHouseLetter();
    }

    private String extractIndex(Path pdfFile) {
        if (pdfFile == null || pdfFile.getFileName() == null) {
            return "";
        }

        Matcher matcher = INDEX_PATTERN.matcher(pdfFile.getFileName().toString());

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1);
    }

    private String formatUnparsedAddress(String rawAddress, Path pdfFile) {
        String index = extractIndex(pdfFile);
        String fileName = pdfFile == null || pdfFile.getFileName() == null
            ? ""
            : pdfFile.getFileName().toString();

        return "Индекс: " + index
            + " | Файл: " + fileName
            + " | Адрес: " + rawAddress;
    }

    private String formatMissingAddressDocument(Path pdfFile, int pageNumber, String pageText) {
        String fileName = pdfFile == null ? "" : pdfFile.toString();

        return "Файл: " + fileName
            + System.lineSeparator()
            + "Страница: " + pageNumber
            + System.lineSeparator()
            + "Встреченный текст: " + cleanupPageText(pageText)
            + System.lineSeparator();
    }

    private String cleanupPageText(String pageText) {
        if (pageText == null || pageText.isBlank()) {
            return "";
        }

        String preparedText = pageText
            .replaceAll("[\\r\\n\\t\\u00A0]+", " ")
            .replaceAll("\\s+", " ")
            .trim();

        if (preparedText.length() <= MISSING_ADDRESS_TEXT_LIMIT) {
            return preparedText;
        }

        return preparedText.substring(0, MISSING_ADDRESS_TEXT_LIMIT) + "...";
    }
}
