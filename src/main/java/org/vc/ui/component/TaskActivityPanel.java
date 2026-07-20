package org.vc.ui.component;

import org.vc.ui.ParserTaskState;
import org.vc.ui.theme.ChromecoreTheme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Показывает состояние фоновой задачи и анимированный Chromecore-индикатор.
 */
public class TaskActivityPanel extends JPanel {

    private static final int COMPLETION_DISPLAY_MILLIS = 2800;

    private final JLabel statusLabel = new JLabel("●  СИСТЕМА ГОТОВА", SwingConstants.CENTER);
    private final ActivityBar activityBar = new ActivityBar();
    private final Timer resetTimer;

    /**
     * Создаёт компактный статусный блок для металлической шапки окна.
     */
    public TaskActivityPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 8));
        setPreferredSize(new Dimension(230, 74));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(42, 72, 94)),
            new EmptyBorder(9, 12, 9, 12)
        ));

        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(new Color(0, 57, 82));

        add(statusLabel, BorderLayout.CENTER);
        add(activityBar, BorderLayout.SOUTH);

        resetTimer = new Timer(COMPLETION_DISPLAY_MILLIS, event ->
            setState(ParserTaskState.IDLE, "Система готова")
        );
        resetTimer.setRepeats(false);
    }

    /**
     * Переключает текст, цвет и анимацию без создания новых таймеров.
     */
    public void setState(ParserTaskState state, String message) {
        resetTimer.stop();
        activityBar.setState(state);

        String preparedMessage = message == null || message.isBlank()
            ? defaultMessage(state)
            : message;
        statusLabel.setText(symbol(state) + "  " + preparedMessage.toUpperCase());
        statusLabel.setForeground(statusColor(state));

        if (state == ParserTaskState.SUCCESS || state == ParserTaskState.ERROR) {
            resetTimer.restart();
        }
    }

    private String defaultMessage(ParserTaskState state) {
        return switch (state) {
            case RUNNING -> "Выполняется";
            case SUCCESS -> "Операция завершена";
            case ERROR -> "Ошибка обработки";
            case IDLE -> "Система готова";
        };
    }

    private String symbol(ParserTaskState state) {
        return switch (state) {
            case RUNNING -> ">>";
            case SUCCESS -> "●";
            case ERROR -> "▲";
            case IDLE -> "●";
        };
    }

    private Color statusColor(ParserTaskState state) {
        return switch (state) {
            case RUNNING -> new Color(0, 79, 116);
            case SUCCESS -> new Color(0, 92, 68);
            case ERROR -> new Color(142, 28, 42);
            case IDLE -> new Color(0, 57, 82);
        };
    }

    private static final class ActivityBar extends JComponent {

        private final Timer animationTimer;
        private ParserTaskState state = ParserTaskState.IDLE;
        private int phase;

        private ActivityBar() {
            setPreferredSize(new Dimension(200, 8));
            animationTimer = new Timer(32, event -> {
                phase = (phase + 4) % 200;
                repaint();
            });
        }

        private void setState(ParserTaskState state) {
            this.state = state;
            phase = 0;

            if (state == ParserTaskState.RUNNING) {
                animationTimer.start();
            } else {
                animationTimer.stop();
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            RoundRectangle2D track = new RoundRectangle2D.Float(0, 0, width - 1f, height - 1f, height, height);

            g2.setColor(new Color(12, 24, 35, 150));
            g2.fill(track);
            g2.setColor(new Color(44, 75, 96));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(track);

            if (state == ParserTaskState.RUNNING) {
                int pulseWidth = Math.max(42, width / 3);
                int travel = width + pulseWidth;
                int x = phase * travel / 200 - pulseWidth;
                RoundRectangle2D pulse = new RoundRectangle2D.Float(x, 1, pulseWidth, height - 2f, height, height);
                g2.setPaint(new GradientPaint(
                    x,
                    0,
                    new Color(35, 116, 196),
                    x + pulseWidth,
                    0,
                    ChromecoreTheme.ACCENT
                ));
                g2.fill(pulse);
            } else if (state == ParserTaskState.SUCCESS) {
                g2.setColor(ChromecoreTheme.SUCCESS);
                g2.fill(track);
            } else if (state == ParserTaskState.ERROR) {
                g2.setColor(new Color(255, 77, 103));
                g2.fill(track);
            } else {
                g2.setColor(new Color(104, 142, 165));
                g2.fill(new RoundRectangle2D.Float(1, 1, width / 5f, height - 2f, height, height));
            }

            g2.dispose();
        }
    }
}
