package org.vc.address;

/**
 * Утилита для базовой очистки адресных строк перед парсингом и сравнением.
 *
 * @author Valerii Trufanov
 * @since 14.05.2026
 */
public final class AddressCleaner {

    private AddressCleaner() {
    }

    /**
     * Нормализует пробелы, запятые, переносы строк и похожие латинские буквы.
     */
    public static String cleanup(String value) {
        if (value == null) {
            return "";
        }

        return normalizeLookAlikeLetters(value)
            .replace('ё', 'е')
            .replace('Ё', 'Е')
            .replaceAll("[\\r\\n\\t\\u00A0]", " ")
            .replaceAll("\\s+", " ")
            .replaceAll("\\s*,\\s*", ", ")
            .replaceAll("\\s*,\\s*\\.\\s*", ", ")
            .replaceAll("\\s+\\.\\s+", " ")
            .replaceAll("\\s+\\.\\s*,", ",")
            .replaceAll(",\\s*,+", ",")
            .replaceAll("^[\\s.,]+", "")
            .trim();
    }

    /**
     * Очищает название улицы от города, слова "дом" и лишних разделителей.
     */
    public static String cleanupStreetName(String value) {
        if (value == null) {
            return "";
        }

        return cleanup(value)
            .replaceAll("(?iu)\\bг\\.?\\s*рыбинск\\b", "")
            .replaceAll("(?iu)\\bгород\\s+рыбинск\\b", "")
            .replaceAll("(?iu)\\bг\\.?\\s*иркутск\\b", "")
            .replaceAll("(?iu)\\bгород\\s+иркутск\\b", "")
            .replaceAll("(?iu)\\bд\\.?$", "")
            .replaceAll("(?iu)\\bдом$", "")
            .replaceAll(",", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Заменяет латинские буквы, похожие на кириллические: A -> А, K -> К и т.д.
     */
    public static String normalizeLookAlikeLetters(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace('A', 'А')
            .replace('a', 'а')
            .replace('B', 'В')
            .replace('E', 'Е')
            .replace('e', 'е')
            .replace('K', 'К')
            .replace('k', 'к')
            .replace('M', 'М')
            .replace('H', 'Н')
            .replace('O', 'О')
            .replace('o', 'о')
            .replace('P', 'Р')
            .replace('p', 'р')
            .replace('C', 'С')
            .replace('c', 'с')
            .replace('T', 'Т')
            .replace('X', 'Х')
            .replace('x', 'х')
            .replace('Y', 'У')
            .replace('y', 'у');
    }
}