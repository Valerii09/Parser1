package org.vc.ui;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Пакетно переносит стандартный вывод в Swing и ограничивает историю лога,
 * чтобы длительная обработка не заполняла heap строками интерфейса.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class LogOutputStream extends OutputStream {

    private static final int DEFAULT_MAX_LOG_CHARACTERS = 200_000;

    private final JTextArea textArea;
    private final int maxLogCharacters;
    private final Object lock = new Object();
    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
    private final StringBuilder pendingText = new StringBuilder();

    private boolean updateScheduled;

    /**
     * Создаёт ограниченный поток для области лога приложения.
     */
    public LogOutputStream(JTextArea textArea) {
        this(textArea, DEFAULT_MAX_LOG_CHARACTERS);
    }

    LogOutputStream(JTextArea textArea, int maxLogCharacters) {
        if (maxLogCharacters <= 0) {
            throw new IllegalArgumentException("Лимит истории лога должен быть больше нуля");
        }

        this.textArea = textArea;
        this.maxLogCharacters = maxLogCharacters;
    }

    @Override
    public void write(int value) {
        synchronized (lock) {
            if (value == '\n') {
                flushLineLocked();
            } else {
                lineBuffer.write(value);
            }
        }
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        synchronized (lock) {
            int end = offset + length;
            for (int index = offset; index < end; index++) {
                int value = bytes[index] & 0xff;
                if (value == '\n') {
                    flushLineLocked();
                } else {
                    lineBuffer.write(value);
                }
            }
        }
    }

    @Override
    public void flush() {
        synchronized (lock) {
            flushLineLocked();
        }
    }

    private void flushLineLocked() {
        if (lineBuffer.size() == 0) {
            return;
        }

        String line = lineBuffer.toString(StandardCharsets.UTF_8).replace("\r", "");
        lineBuffer.reset();

        pendingText.append(line).append(System.lineSeparator());
        trimPendingText();
        scheduleUpdateLocked();
    }

    private void trimPendingText() {
        int excess = pendingText.length() - maxLogCharacters;
        if (excess > 0) {
            pendingText.delete(0, excess);
        }
    }

    private void scheduleUpdateLocked() {
        if (updateScheduled) {
            return;
        }

        updateScheduled = true;
        SwingUtilities.invokeLater(this::drainPendingText);
    }

    private void drainPendingText() {
        String text;

        synchronized (lock) {
            text = pendingText.toString();
            pendingText.setLength(0);
            updateScheduled = false;
        }

        appendBounded(text);

        synchronized (lock) {
            if (!pendingText.isEmpty()) {
                scheduleUpdateLocked();
            }
        }
    }

    private void appendBounded(String text) {
        textArea.append(text);
        Document document = textArea.getDocument();
        int excess = document.getLength() - maxLogCharacters;

        if (excess > 0) {
            try {
                document.remove(0, excess);
            } catch (BadLocationException exception) {
                textArea.setText(text.substring(Math.max(0, text.length() - maxLogCharacters)));
            }
        }

        textArea.setCaretPosition(document.getLength());
    }
}