package org.vc.ui.component;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Переиспользуемая панель выбора файла или каталога.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class FileChooserPanel extends JPanel {

    private final JTextField pathField = new JTextField();
    private final int selectionMode;

    
    public FileChooserPanel(String labelText, String buttonText, int selectionMode) {
        this.selectionMode = selectionMode;

        init(labelText, buttonText);
    }

    
    public String getSelectedPath() {
        return pathField.getText();
    }

    private void init(String labelText, String buttonText) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        pathField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        pathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 215, 225)),
            new EmptyBorder(7, 8, 7, 8)
        ));

        JButton chooseButton = new JButton(buttonText);
        chooseButton.setFocusPainted(false);
        chooseButton.setBorder(new EmptyBorder(8, 14, 8, 14));
        chooseButton.addActionListener(event -> choosePath());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        add(label, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        add(pathField, constraints);

        constraints.gridx = 2;
        constraints.gridy = 0;
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
}
