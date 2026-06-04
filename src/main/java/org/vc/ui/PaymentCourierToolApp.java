package org.vc.ui;

import javax.swing.SwingUtilities;

/**
 * Запускает приложение Swing.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class PaymentCourierToolApp {

    /**
     * Точка входа для запуска сценария.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParserFrame frame = new ParserFrame();
            frame.setVisible(true);
        });
    }
}
