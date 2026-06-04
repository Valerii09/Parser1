package org.vc.ui;

import javax.swing.SwingUtilities;

/**
 * Запускает приложение Swing.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class ParserApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParserFrame frame = new ParserFrame();
            frame.setVisible(true);
        });
    }
}
