package org.vc.ui;

import org.junit.jupiter.api.Test;
import org.vc.address.PaymentSupplier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTaskRunnerTest {

    @Test
    void shouldPublishRunningAndSuccessStates() throws Exception {
        RecordingView view = new RecordingView();
        ParserTaskRunner runner = new ParserTaskRunner(view);

        runner.run("Распределение PDF", () -> {
            // Успешная короткая операция.
        });

        assertTrue(view.finished.await(2, TimeUnit.SECONDS));
        assertEquals(List.of(ParserTaskState.RUNNING, ParserTaskState.SUCCESS), view.states);
        assertEquals("Готово: Распределение PDF", view.lastMessage);
    }

    private static final class RecordingView implements ParserView {

        private final List<ParserTaskState> states = new CopyOnWriteArrayList<>();
        private final CountDownLatch finished = new CountDownLatch(1);
        private volatile String lastMessage;

        @Override
        public String getSelectedExcelPath() {
            return "";
        }

        @Override
        public String getSelectedPdfFolderPath() {
            return "";
        }

        @Override
        public boolean isDuplexPrintingSelected() {
            return false;
        }

        @Override
        public PaymentSupplier getSelectedPaymentSupplier() {
            return PaymentSupplier.AUTO;
        }

        @Override
        public void showTaskState(ParserTaskState state, String message) {
            states.add(state);
            lastMessage = message;
            if (state == ParserTaskState.SUCCESS || state == ParserTaskState.ERROR) {
                finished.countDown();
            }
        }

        @Override
        public void clearLog() {
        }

        @Override
        public void showError(String message) {
        }
    }
}
