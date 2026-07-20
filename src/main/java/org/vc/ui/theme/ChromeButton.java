package org.vc.ui.theme;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Кнопка с хромированной рамкой и отдельным акцентом для основного действия.
 */
public class ChromeButton extends JButton {

    private final boolean primary;

    /**
     * Создаёт серебристую или акцентную кнопку в зависимости от роли действия.
     */
    public ChromeButton(String text, boolean primary) {
        super(text);
        this.primary = primary;

        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setForeground(primary ? Color.WHITE : new Color(8, 15, 25));
        setBorder(new EmptyBorder(10, 18, 10, 18));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, width - 2f, height - 2f, 18, 18);

        Color top = primary ? ChromecoreTheme.ACCENT : ChromecoreTheme.CHROME_LIGHT;
        Color bottom = primary ? ChromecoreTheme.ACCENT_DARK : ChromecoreTheme.CHROME_MID;

        if (!isEnabled()) {
            top = new Color(88, 98, 110);
            bottom = new Color(49, 57, 68);
        } else if (getModel().isPressed()) {
            Color swap = top;
            top = bottom.darker();
            bottom = swap.darker();
        } else if (getModel().isRollover()) {
            top = top.brighter();
            bottom = bottom.brighter();
        }

        g2.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
        g2.fill(shape);
        g2.setColor(primary ? new Color(188, 239, 255) : Color.WHITE);
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(shape);
        g2.dispose();

        setForeground(isEnabled()
            ? primary ? Color.WHITE : new Color(8, 15, 25)
            : new Color(183, 192, 201));
        super.paintComponent(graphics);
    }
}
