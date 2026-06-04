package org.vc.ui;

import org.vc.ui.component.FileChooserPanel;
import org.vc.ui.component.LogPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Главное окно приложения.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class ParserFrame extends JFrame implements ParserView {

    private static final String COURIERS_FOLDER_NAME = "Курьеры";

    private final Path couriersRoot = Paths.get(
        System.getProperty("user.home"),
        "Documents",
        COURIERS_FOLDER_NAME
    );

    private final FileChooserPanel excelChooserPanel = new FileChooserPanel(
        "Excel с адресами:",
        "Выбрать Excel",
        JFileChooser.FILES_ONLY
    );

    private final FileChooserPanel pdfFolderChooserPanel = new FileChooserPanel(
        "Папка с PDF:",
        "Выбрать папку",
        JFileChooser.DIRECTORIES_ONLY
    );

    private final FileChooserPanel xmlFolderChooserPanel = new FileChooserPanel(
        "Папка с XML:",
        "Выбрать папку",
        JFileChooser.DIRECTORIES_ONLY
    );

    private final FileChooserPanel xmlOutputFolderChooserPanel = new FileChooserPanel(
        "Куда сохранить PDF из XML:",
        "Выбрать папку",
        JFileChooser.DIRECTORIES_ONLY
    );
    private final JCheckBox duplexPrintingCheckBox = new JCheckBox("Двусторонняя печать");
    private final LogPanel logPanel = new LogPanel();
    private final ParserTaskRunner taskRunner;
    private final ParserPresenter presenter;

    public ParserFrame() {
        super("Courier Parser");

        taskRunner = new ParserTaskRunner(this);
        presenter = new ParserPresenter(this, taskRunner, couriersRoot);

        initWindow();
        initLogRedirect();
    }

    private void initWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 680));
        setSize(1040, 720);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(245, 247, 250));

        root.add(createHeaderPanel(), BorderLayout.NORTH);
        root.add(createMainPanel(), BorderLayout.CENTER);
        root.add(createFooterPanel(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);

        JLabel title = new JLabel("Парсер курьерских платёжек");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));

        JLabel subtitle = new JLabel("Формирование адресов курьеров и PDF-файлов по платёжкам");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitle.setForeground(new Color(90, 90, 90));

        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        panel.add(createSettingsCard(), BorderLayout.NORTH);
        panel.add(logPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSettingsCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 229, 235)),
            new EmptyBorder(14, 14, 14, 14)
        ));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(4, 4, 10, 4);

        constraints.gridy = 0;
        card.add(excelChooserPanel, constraints);

        constraints.gridy = 1;
        card.add(pdfFolderChooserPanel, constraints);

        constraints.gridy = 2;
        card.add(xmlFolderChooserPanel, constraints);

        constraints.gridy = 3;
        card.add(xmlOutputFolderChooserPanel, constraints);

        duplexPrintingCheckBox.setOpaque(false);
        duplexPrintingCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        duplexPrintingCheckBox.setText("Двусторонняя печать — добавлять к платёжке следующую страницу без распознавания");

        constraints.gridy = 4;
        constraints.insets = new Insets(4, 8, 10, 4);
        card.add(duplexPrintingCheckBox, constraints);

        JLabel resultLabel = new JLabel("Результат будет сохранён в: " + couriersRoot);
        resultLabel.setForeground(new Color(90, 90, 90));

        constraints.gridy = 5;
        constraints.insets = new Insets(4, 8, 2, 4);
        card.add(resultLabel, constraints);

        return card;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        JButton clearLogButton = createSecondaryButton("Очистить лог");
        clearLogButton.addActionListener(event -> presenter.clearLog());

        JButton createAddressesButton = createPrimaryButton("1. Сформировать адреса");
        createAddressesButton.addActionListener(event -> presenter.createCourierAddresses());

        JButton createPdfButton = createPrimaryButton("2. Сформировать PDF");
        createPdfButton.addActionListener(event -> presenter.createCourierPdfs());

        JButton createXmlPdfButton = createPrimaryButton("XML → PDF");
        createXmlPdfButton.addActionListener(event -> presenter.createPdfFromXml());

        panel.add(clearLogButton);
        panel.add(createAddressesButton);
        panel.add(createPdfButton);
        panel.add(createXmlPdfButton);

        return panel;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBackground(new Color(38, 115, 255));
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(9, 16, 9, 16));

        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setBackground(new Color(235, 238, 244));
        button.setForeground(new Color(40, 40, 40));
        button.setBorder(new EmptyBorder(9, 16, 9, 16));

        return button;
    }

    private void initLogRedirect() {
        logPanel.redirectSystemOutput();
    }

    @Override
    public String getSelectedExcelPath() {
        return excelChooserPanel.getSelectedPath();
    }

    @Override
    public String getSelectedPdfFolderPath() {
        return pdfFolderChooserPanel.getSelectedPath();
    }

    @Override
    public String getSelectedXmlFolderPath() {
        return xmlFolderChooserPanel.getSelectedPath();
    }

    @Override
    public String getSelectedXmlOutputFolderPath() {
        return xmlOutputFolderChooserPanel.getSelectedPath();
    }

    @Override
    public boolean isDuplexPrintingSelected() {
        return duplexPrintingCheckBox.isSelected();
    }

    @Override
    public void clearLog() {
        logPanel.clear();
    }

    @Override
    public void showError(String message) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                this,
                message,
                "Ошибка",
                JOptionPane.ERROR_MESSAGE
            )
        );
    }
}
