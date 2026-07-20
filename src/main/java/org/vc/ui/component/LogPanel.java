package org.vc.ui.component;

import org.vc.ui.LogOutputStream;
import org.vc.ui.theme.ChromePanel;
import org.vc.ui.theme.ChromecoreTheme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Показывает ограниченный по памяти журнал обработки в виде терминального блока.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class LogPanel extends ChromePanel {

    private final JTextArea logArea = new JTextArea();

    /**
     * Создаёт терминальную область журнала с Chromecore-оформлением.
     */
    public LogPanel() {
        super(Style.CARD);
        init();
    }

    /**
     * Удаляет текущую историю вывода из интерфейса.
     */
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
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(13, 13, 13, 13));

        JLabel title = new JLabel("●  ЖУРНАЛ ВЫПОЛНЕНИЯ // ПОСЛЕДНИЕ СОБЫТИЯ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(ChromecoreTheme.SUCCESS);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setForeground(ChromecoreTheme.TEXT_PRIMARY);
        logArea.setCaretColor(ChromecoreTheme.ACCENT);
        logArea.setBackground(ChromecoreTheme.FIELD_BACKGROUND);
        logArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.getViewport().setBackground(ChromecoreTheme.FIELD_BACKGROUND);
        scrollPane.setBorder(BorderFactory.createLineBorder(ChromecoreTheme.CHROME_DARK));

        add(title, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
}
