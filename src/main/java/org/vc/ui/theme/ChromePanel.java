package org.vc.ui.theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Панель с металлическим градиентом и тонкой хромированной рамкой.
 */
public class ChromePanel extends JPanel {

    public enum Style {
        ROOT,
        CARD,
        HEADER
    }

    private final Style style;
    private final int cornerRadius;

    /**
     * Создаёт панель с радиусом, подходящим выбранному типу поверхности.
     */
    public ChromePanel(Style style) {
        this(style, style == Style.ROOT ? 0 : 20);
    }

    /**
     * Создаёт панель с явно заданным радиусом скругления.
     */
    public ChromePanel(Style style, int cornerRadius) {
        this.style = style;
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Float(
            0.5f,
            0.5f,
            Math.max(0, width - 1f),
            Math.max(0, height - 1f),
            cornerRadius,
            cornerRadius
        );

        Color top = style == Style.ROOT
            ? ChromecoreTheme.BACKGROUND_TOP
            : style == Style.HEADER ? Color.WHITE : ChromecoreTheme.PANEL_TOP;
        Color bottom = style == Style.ROOT
            ? ChromecoreTheme.BACKGROUND_BOTTOM
            : style == Style.HEADER ? new Color(205, 215, 224) : ChromecoreTheme.PANEL_BOTTOM;

        g2.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
        g2.fill(shape);

        if (style == Style.ROOT) {
            paintGrid(g2, width, height);
        } else {
            paintChromeBorder(g2, shape, width);
        }

        g2.dispose();
        super.paintComponent(graphics);
    }

    private void paintGrid(Graphics2D g2, int width, int height) {
        g2.setColor(new Color(45, 79, 104, 14));
        for (int y = 0; y < height; y += 6) {
            g2.drawLine(0, y, width, y);
        }

        g2.setColor(new Color(0, 121, 188, 42));
        g2.drawLine(0, 1, width, 1);
    }

    private void paintChromeBorder(Graphics2D g2, RoundRectangle2D shape, int width) {
        g2.setStroke(new BasicStroke(1.2f));
        g2.setPaint(new GradientPaint(
            0,
            0,
            ChromecoreTheme.CHROME_LIGHT,
            width,
            0,
            ChromecoreTheme.CHROME_DARK
        ));
        g2.draw(shape);
    }
}
