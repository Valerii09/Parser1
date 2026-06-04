package org.vc.util;

/**
 * Содержит утилиты для безопасного формирования имён файлов.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public final class FileNameUtils {

    private FileNameUtils() {
    }

    /**
     * Заменяет символы, недопустимые в именах файлов Windows.
     */
    public static String safeFileName(String value) {
        return value
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .trim();
    }
}
