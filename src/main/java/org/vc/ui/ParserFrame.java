package org.vc.ui;

import org.vc.address.PaymentSupplier;
import org.vc.ui.component.FileChooserPanel;
import org.vc.ui.component.LogPanel;
import org.vc.ui.component.TaskActivityPanel;
import org.vc.ui.theme.ChromeButton;
import org.vc.ui.theme.ChromePanel;
import org.vc.ui.theme.ChromecoreAppIcon;
import org.vc.ui.theme.ChromecoreTheme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Главное окно сортировки платёжек с рабочим Chromecore-оформлением.
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
        "Excel с адресами",
        "Открыть Excel",
        JFileChooser.FILES_ONLY
    );

    private final FileChooserPanel pdfFolderChooserPanel = new FileChooserPanel(
        "Папка с PDF",
        "Открыть папку",
        JFileChooser.DIRECTORIES_ONLY
    );

    private final JCheckBox duplexPrintingCheckBox = new JCheckBox();
    private final JComboBox<PaymentSupplier> supplierComboBox = new JComboBox<>(PaymentSupplier.values());
    private final LogPanel logPanel = new LogPanel();
    private final TaskActivityPanel activityPanel = new TaskActivityPanel();
    private final ChromeButton createAddressesButton = new ChromeButton("1  Сформировать адреса", false);
    private final ChromeButton createPdfButton = new ChromeButton("2  Распределить PDF", true);
    private final ParserTaskRunner taskRunner;
    private final ParserPresenter presenter;

    /**
     * Создаёт главное окно и связывает представление с пользовательскими сценариями.
     */
    public ParserFrame() {
        super("Payment Courier Tool // Routing Core");

        taskRunner = new ParserTaskRunner(this);
        presenter = new ParserPresenter(this, taskRunner, couriersRoot);

        initWindow();
        initLogRedirect();
    }

    private void initWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setSize(1120, 780);
        setLocationRelativeTo(null);
        setIconImage(ChromecoreAppIcon.create(64));

        ChromePanel root = new ChromePanel(ChromePanel.Style.ROOT);
        root.setLayout(new BorderLayout(16, 16));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        root.add(createHeaderPanel(), BorderLayout.NORTH);
        root.add(createMainPanel(), BorderLayout.CENTER);
        root.add(createFooterPanel(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createHeaderPanel() {
        ChromePanel panel = new ChromePanel(ChromePanel.Style.HEADER);
        panel.setLayout(new BorderLayout(16, 6));
        panel.setBorder(new EmptyBorder(15, 20, 15, 18));

        JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);

        JLabel overline = new JLabel("PAYMENT COURIER // ROUTING CORE 26");
        overline.setFont(new Font("Segoe UI", Font.BOLD, 11));
        overline.setForeground(new Color(25, 48, 70));

        JLabel title = new JLabel("СОРТИРОВКА ПЛАТЁЖНЫХ ДОКУМЕНТОВ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(6, 13, 22));

        JLabel subtitle = new JLabel("Маршруты курьеров  /  распознавание адресов  /  PDF до 5000 страниц");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(28, 45, 62));

        textPanel.add(overline, BorderLayout.NORTH);
        textPanel.add(title, BorderLayout.CENTER);
        textPanel.add(subtitle, BorderLayout.SOUTH);

        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(activityPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setOpaque(false);
        panel.add(createSettingsCard(), BorderLayout.NORTH);
        panel.add(logPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSettingsCard() {
        ChromePanel card = new ChromePanel(ChromePanel.Style.CARD);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;

        JLabel section = new JLabel("01 // ВХОДНЫЕ ДАННЫЕ И РЕЖИМ ОБРАБОТКИ");
        section.setFont(new Font("Segoe UI", Font.BOLD, 12));
        section.setForeground(ChromecoreTheme.ACCENT);
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 5, 8, 5);
        card.add(section, constraints);

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 2, 0);
        card.add(excelChooserPanel, constraints);

        constraints.gridy = 2;
        card.add(pdfFolderChooserPanel, constraints);

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        optionsPanel.setOpaque(false);

        JLabel supplierLabel = new JLabel("ПОСТАВЩИК PDF");
        supplierLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        supplierLabel.setForeground(ChromecoreTheme.TEXT_SECONDARY);
        configureSupplierComboBox();

        duplexPrintingCheckBox.setOpaque(false);
        duplexPrintingCheckBox.setForeground(ChromecoreTheme.TEXT_PRIMARY);
        duplexPrintingCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        duplexPrintingCheckBox.setText("Двусторонняя печать — следующая страница относится к той же платёжке");

        optionsPanel.add(supplierLabel);
        optionsPanel.add(supplierComboBox);
        optionsPanel.add(duplexPrintingCheckBox);

        constraints.gridy = 3;
        constraints.insets = new Insets(10, 5, 8, 5);
        card.add(optionsPanel, constraints);

        JLabel resultLabel = new JLabel("ВЫХОДНОЙ КАТАЛОГ  ›  " + couriersRoot);
        resultLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultLabel.setForeground(ChromecoreTheme.SUCCESS);

        constraints.gridy = 4;
        constraints.insets = new Insets(2, 5, 0, 5);
        card.add(resultLabel, constraints);

        return card;
    }

    private void configureSupplierComboBox() {
        supplierComboBox.setForeground(ChromecoreTheme.TEXT_PRIMARY);
        supplierComboBox.setBackground(ChromecoreTheme.FIELD_BACKGROUND);
        supplierComboBox.setBorder(BorderFactory.createLineBorder(ChromecoreTheme.CHROME_DARK));
        supplierComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                Component component = super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
                );
                component.setForeground(ChromecoreTheme.TEXT_PRIMARY);
                component.setBackground(isSelected
                    ? ChromecoreTheme.ACCENT_DARK
                    : ChromecoreTheme.FIELD_BACKGROUND);
                return component;
            }
        });
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel hint = new JLabel("CORE STATUS  •  2 PDF THREADS  •  MEMORY SAFE MODE");
        hint.setFont(new Font("Segoe UI", Font.BOLD, 10));
        hint.setForeground(ChromecoreTheme.TEXT_SECONDARY);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);

        ChromeButton clearLogButton = new ChromeButton("Очистить лог", false);
        clearLogButton.addActionListener(event -> presenter.clearLog());

        createAddressesButton.addActionListener(event -> presenter.createCourierAddresses());

        createPdfButton.addActionListener(event -> presenter.createCourierPdfs());

        actions.add(clearLogButton);
        actions.add(createAddressesButton);
        actions.add(createPdfButton);

        panel.add(hint, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);
        return panel;
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
    public boolean isDuplexPrintingSelected() {
        return duplexPrintingCheckBox.isSelected();
    }

    @Override
    public PaymentSupplier getSelectedPaymentSupplier() {
        Object selectedItem = supplierComboBox.getSelectedItem();

        return selectedItem instanceof PaymentSupplier paymentSupplier
            ? paymentSupplier
            : PaymentSupplier.AUTO;
    }

    @Override
    public void showTaskState(ParserTaskState state, String message) {
        SwingUtilities.invokeLater(() -> {
            activityPanel.setState(state, message);
            setProcessingControlsEnabled(state != ParserTaskState.RUNNING);
        });
    }

    private void setProcessingControlsEnabled(boolean enabled) {
        excelChooserPanel.setSelectionEnabled(enabled);
        pdfFolderChooserPanel.setSelectionEnabled(enabled);
        supplierComboBox.setEnabled(enabled);
        duplexPrintingCheckBox.setEnabled(enabled);
        createAddressesButton.setEnabled(enabled);
        createPdfButton.setEnabled(enabled);
    }

    @Override
    public void clearLog() {
        logPanel.clear();
    }

    @Override
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
            this,
            message,
            "Ошибка обработки",
            JOptionPane.ERROR_MESSAGE
        ));
    }
}
