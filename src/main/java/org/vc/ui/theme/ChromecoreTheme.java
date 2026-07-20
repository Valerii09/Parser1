package org.vc.ui.theme;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;

/**
 * Единая палитра и базовые настройки Chromecore-интерфейса.
 */
public final class ChromecoreTheme {

    public static final Color BACKGROUND_TOP = new Color(5, 9, 16);
    public static final Color BACKGROUND_BOTTOM = new Color(20, 28, 40);
    public static final Color PANEL_TOP = new Color(43, 52, 67);
    public static final Color PANEL_BOTTOM = new Color(13, 19, 29);
    public static final Color CHROME_LIGHT = new Color(238, 244, 249);
    public static final Color CHROME_MID = new Color(137, 150, 166);
    public static final Color CHROME_DARK = new Color(55, 65, 79);
    public static final Color TEXT_PRIMARY = new Color(239, 246, 252);
    public static final Color TEXT_SECONDARY = new Color(160, 177, 194);
    public static final Color ACCENT = new Color(76, 203, 255);
    public static final Color ACCENT_DARK = new Color(15, 91, 178);
    public static final Color FIELD_BACKGROUND = new Color(6, 12, 21);
    public static final Color SUCCESS = new Color(99, 255, 202);

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

        UIManager.put("ComboBox.background", FIELD_BACKGROUND);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ACCENT_DARK);
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
        UIManager.put("CheckBox.background", PANEL_BOTTOM);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("OptionPane.background", PANEL_BOTTOM);
        UIManager.put("Panel.background", PANEL_BOTTOM);
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
