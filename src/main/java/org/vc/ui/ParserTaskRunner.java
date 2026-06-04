package org.vc.ui;

import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Запускает задачи парсера в фоновом потоке.
 *
 * @author Valerii Trufanov
 * @since 18.05.2026
 */
public class ParserTaskRunner {

    private final ParserView view;
    private final AtomicBoolean running = new AtomicBoolean(false);

    
    public ParserTaskRunner(ParserView view) {
        this.view = view;
    }

    /**
     * Запускает задачу асинхронно и показывает ошибки через интерфейс.
     */
    public void run(ParserTask task) {
        if (!running.compareAndSet(false, true)) {
            System.out.println("Задача уже выполняется, дождитесь завершения текущей операции.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                exception.printStackTrace();
                Toolkit.getDefaultToolkit().beep();
                view.showError(exception.getMessage());
            } finally {
                running.set(false);
            }
        });

        thread.setName("parser-task-thread");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Контракт фоновой задачи.
     */
    public interface ParserTask {
        /**
         * Выполняет фоновую задачу.
         */
        void run() throws Exception;
    }
}
