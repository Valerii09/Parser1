package org.vc.ui;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Перенаправляет текстовый вывод в область лога Swing.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class LogOutputStream extends OutputStream {

    private final JTextArea textArea;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public LogOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int value) {
        if (value == '\n') {
            flushBuffer();
        } else {
            buffer.write(value);
        }
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    private void flushBuffer() {
        if (buffer.size() == 0) {
            return;
        }

        String text = buffer.toString(StandardCharsets.UTF_8)
            .replace("\r", "");

        buffer.reset();

        SwingUtilities.invokeLater(() -> {
            textArea.append(text + System.lineSeparator());
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}
