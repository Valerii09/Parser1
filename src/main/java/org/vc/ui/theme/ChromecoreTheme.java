package org.vc.ui.theme;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;

/**
 * Единая палитра и базовые настройки Chromecore-интерфейса.
 */
public final class ChromecoreTheme {

    public static final Color BACKGROUND_TOP = new Color(246, 249, 252);
    public static final Color BACKGROUND_BOTTOM = new Color(218, 226, 234);
    public static final Color PANEL_TOP = new Color(255, 255, 255);
    public static final Color PANEL_BOTTOM = new Color(231, 237, 243);
    public static final Color CHROME_LIGHT = new Color(255, 255, 255);
    public static final Color CHROME_MID = new Color(184, 196, 207);
    public static final Color CHROME_DARK = new Color(82, 101, 118);
    public static final Color TEXT_PRIMARY = new Color(20, 30, 40);
    public static final Color TEXT_SECONDARY = new Color(65, 82, 97);
    public static final Color ACCENT = new Color(0, 147, 216);
    public static final Color ACCENT_DARK = new Color(0, 91, 158);
    public static final Color FIELD_BACKGROUND = Color.WHITE;
    public static final Color SUCCESS = new Color(0, 112, 82);

    private ChromecoreTheme() {
    }

    /**
     * Настраивает системные Swing-компоненты до создания главного окна.
     */
    public static void install() {
        selectNimbusLookAndFeel();

        FontUIResource interfaceFont = new FontUIResource(new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("defaultFont", interfaceFont);
        UIManager.put("Label.font", interfaceFont);
        UIManager.put("ComboBox.font", interfaceFont);
        UIManager.put("CheckBox.font", interfaceFont);
        UIManager.put("OptionPane.messageFont", interfaceFont);
        UIManager.put("OptionPane.buttonFont", interfaceFont);
        UIManager.put("FileChooser.font", interfaceFont);
        UIManager.put("ToolTip.font", interfaceFont);

        UIManager.put("control", PANEL_BOTTOM);
        UIManager.put("info", PANEL_TOP);
        UIManager.put("text", TEXT_PRIMARY);
        UIManager.put("nimbusBase", CHROME_DARK);
        UIManager.put("nimbusBlueGrey", CHROME_MID);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusLightBackground", FIELD_BACKGROUND);
        UIManager.put("nimbusSelectionBackground", ACCENT_DARK);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusDisabledText", new Color(122, 132, 142));

        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Panel.background", PANEL_TOP);
        UIManager.put("Panel.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", FIELD_BACKGROUND);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ACCENT_DARK);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("CheckBox.background", PANEL_TOP);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.background", FIELD_BACKGROUND);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", FIELD_BACKGROUND);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", PANEL_TOP);
        UIManager.put("Viewport.background", FIELD_BACKGROUND);
        UIManager.put("OptionPane.background", PANEL_TOP);
        UIManager.put("OptionPane.foreground", TEXT_PRIMARY);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
    }

    private static void selectNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) {
            // При недоступном Nimbus приложение останется на системном Look and Feel.
        }
    }
}
