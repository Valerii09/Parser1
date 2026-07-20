package org.vc.ui.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Рисует значок приложения без зависимости от внешних графических ресурсов.
 */
public final class ChromecoreAppIcon {

    private ChromecoreAppIcon() {
    }

    /**
     * Создаёт квадратный значок с хромированным кольцом и синим ядром маршрутизации.
     */
    public static BufferedImage create(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setPaint(new GradientPaint(0, 0, new Color(7, 12, 21), size, size, new Color(30, 43, 61)));
        g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);

        int margin = Math.max(5, size / 10);
        g2.setPaint(new GradientPaint(
            margin,
            margin,
            ChromecoreTheme.CHROME_LIGHT,
            size - margin,
            size - margin,
            ChromecoreTheme.CHROME_DARK
        ));
        g2.fillOval(margin, margin, size - margin * 2, size - margin * 2);

        int coreMargin = margin + Math.max(3, size / 14);
        g2.setPaint(new GradientPaint(
            coreMargin,
            coreMargin,
            ChromecoreTheme.ACCENT,
            size - coreMargin,
            size - coreMargin,
            ChromecoreTheme.ACCENT_DARK
        ));
        g2.fillOval(coreMargin, coreMargin, size - coreMargin * 2, size - coreMargin * 2);

        g2.setColor(new Color(231, 248, 255));
        g2.setStroke(new BasicStroke(Math.max(1.5f, size / 28f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int center = size / 2;
        int radius = size / 6;
        g2.drawArc(center - radius, center - radius, radius * 2, radius * 2, 35, 285);
        g2.drawLine(center + radius - 1, center - 2, center + radius + size / 12, center - size / 10);

        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(9, size / 5)));
        g2.drawString("P", center - size / 13, center + size / 13);
        g2.dispose();

        return image;
    }
}
