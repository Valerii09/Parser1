package org.vc.pdf;

/**
 * Экранирует текст перед вставкой в HTML-шаблон.
 * XML поставщика может содержать кавычки, амперсанды и служебные символы, поэтому
 * вставлять значения напрямую в HTML нельзя.
 */
final class HtmlEscaper {

    private HtmlEscaper() {
    }

    static String text(String value) {
        return clean(value)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    static String textOrDash(String value) {
        String preparedValue = text(value);

        return preparedValue.isBlank() ? "-" : preparedValue;
    }

    static String textOrZero(String value) {
        String preparedValue = text(value);

        return preparedValue.isBlank() ? "0" : preparedValue;
    }

    static String amount(String value) {
        String preparedValue = text(value);

        return preparedValue.isBlank() ? "0.00" : preparedValue;
    }

    static String clean(String value) {
        return value == null
            ? ""
            : value.replace('\u00A0', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
