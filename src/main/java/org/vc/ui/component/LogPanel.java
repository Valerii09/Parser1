package org.vc.ui.component;

import org.vc.ui.LogOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Показывает вывод парсера внутри интерфейса.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class LogPanel extends JPanel {

    private final JTextArea logArea = new JTextArea();

    public LogPanel() {
        init();
    }

    public void clear() {
        logArea.setText("");
    }

    /**
     * Перенаправляет стандартный вывод и поток ошибок в область лога.
     */
    public void redirectSystemOutput() {
        PrintStream printStream = new PrintStream(
            new LogOutputStream(logArea),
            true,
            StandardCharsets.UTF_8
        );

        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 229, 235)),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel title = new JLabel("Лог выполнения");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setBorder(new EmptyBorder(0, 0, 8, 0));

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(250, 251, 253));
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(225, 229, 235)));

        add(title, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
}
