package org.vc.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogOutputStreamTest {

    @Test
    void shouldLimitLogHistoryAndBatchUiUpdates() throws Exception {
        JTextArea textArea = new JTextArea();
        LogOutputStream outputStream = new LogOutputStream(textArea, 100);

        for (int index = 0; index < 50; index++) {
            byte[] line = ("Строка лога " + index + "\n").getBytes(StandardCharsets.UTF_8);
            outputStream.write(line);
        }

        outputStream.flush();
        SwingUtilities.invokeAndWait(() -> {
            // Ожидаем выполнения ранее поставленного пакетного обновления.
        });

        assertTrue(textArea.getDocument().getLength() <= 100);
        assertTrue(textArea.getText().contains("49"));
    }
}
