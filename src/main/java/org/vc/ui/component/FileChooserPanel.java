package org.vc.ui.component;

import org.vc.ui.theme.ChromeButton;
import org.vc.ui.theme.ChromecoreTheme;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Строка выбора файла или каталога, оформленная в едином стиле приложения.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class FileChooserPanel extends JPanel {

    private final JTextField pathField = new JTextField();
    private final int selectionMode;
    private ChromeButton chooseButton;

    /**
     * Создаёт строку с подписью, редактируемым путём и кнопкой системного диалога.
     */
    public FileChooserPanel(String labelText, String buttonText, int selectionMode) {
        this.selectionMode = selectionMode;
        init(labelText, buttonText);
    }

    /**
     * Возвращает введённый или выбранный пользователем путь без дополнительного преобразования.
     */
    public String getSelectedPath() {
        return pathField.getText();
    }

    private void init(String labelText, String buttonText) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(ChromecoreTheme.TEXT_SECONDARY);

        pathField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pathField.setForeground(ChromecoreTheme.TEXT_PRIMARY);
        pathField.setDisabledTextColor(ChromecoreTheme.TEXT_SECONDARY);
        pathField.setCaretColor(ChromecoreTheme.ACCENT);
        pathField.setSelectionColor(ChromecoreTheme.ACCENT_DARK);
        pathField.setSelectedTextColor(ChromecoreTheme.TEXT_PRIMARY);
        pathField.setBackground(ChromecoreTheme.FIELD_BACKGROUND);
        pathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ChromecoreTheme.CHROME_DARK),
            new EmptyBorder(9, 11, 9, 11)
        ));

        chooseButton = new ChromeButton(buttonText, false);
        chooseButton.addActionListener(event -> choosePath());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.ipadx = 8;
        add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.ipadx = 0;
        add(pathField, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        add(chooseButton, constraints);
    }

    private void choosePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(selectionMode);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Блокирует изменение пути на время фоновой обработки.
     */
    public void setSelectionEnabled(boolean enabled) {
        pathField.setEnabled(enabled);
        chooseButton.setEnabled(enabled);
    }
}
