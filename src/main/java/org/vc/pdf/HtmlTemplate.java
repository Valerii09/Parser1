package org.vc.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Минимальный шаблонизатор для печатных HTML-макетов.
 * В проекте пока не нужен отдельный движок шаблонов: достаточно подстановки заранее
 * подготовленных и экранированных HTML-фрагментов.
 */
final class HtmlTemplate {

    private final String template;

    private HtmlTemplate(String template) {
        this.template = template;
    }

    static HtmlTemplate fromResource(String resourcePath) throws IOException {
        try (InputStream inputStream = HtmlTemplate.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Не найден HTML-шаблон: " + resourcePath);
            }

            return new HtmlTemplate(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    String render(Map<String, String> values) {
        String result = template;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }
}
