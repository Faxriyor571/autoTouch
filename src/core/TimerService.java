package core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TimerService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    private ScheduledFuture<?> clockFuture;
    private ScheduledFuture<?> countdownFuture;

    public void startClock(Consumer<String> onTick) {
        stopClock();
        clockFuture = scheduler.scheduleAtFixedRate(() -> {
            String time = LocalTime.now().format(FORMATTER);
            onTick.accept(time);
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    public void stopClock() {
        if (clockFuture != null && !clockFuture.isCancelled()) {
            clockFuture.cancel(false);
            clockFuture = null;
        }
    }

    public void startCountdown(String targetTime,
                               Consumer<Long> onTick,
                               Consumer<Long> onReached) {
        LocalTime target     = parse(targetTime);
        long      targetNano = target.toNanoOfDay();

        stopCountdown();

        countdownFuture = scheduler.scheduleAtFixedRate(() -> {

            long remainingMs = (targetNano - LocalTime.now().toNanoOfDay()) / 1_000_000;

            if (remainingMs > 1) {
                onTick.accept(remainingMs);

            } else if (remainingMs > -500) {
                stopCountdown();

                // System.nanoTime() anchor:
                // LocalTime.now() har chaqiruvda yangi obyekt → GC bosimi
                // System.nanoTime() primitive long qaytaradi → GC yo'q
                // Ikki soat bir xil emas — delta orqali o'tkazamiz
                long anchorLocalNano = LocalTime.now().toNanoOfDay();
                long anchorSysNano   = System.nanoTime();
                long targetSysNano   = anchorSysNano + (targetNano - anchorLocalNano);

                // Sof busy-wait — GC yo'q
                while (System.nanoTime() < targetSysNano) {
                    Thread.onSpinWait();
                }

                // Kechikish
                long delayMs = Math.max(0,
                        (System.nanoTime() - targetSysNano) / 1_000_000L);

                onReached.accept(delayMs);
            }

        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    public void stopCountdown() {
        if (countdownFuture != null && !countdownFuture.isCancelled()) {
            countdownFuture.cancel(false);
            countdownFuture = null;
        }
    }

    public void shutdown() {
        stopClock();
        stopCountdown();
        scheduler.shutdownNow();
    }

    public static LocalTime parse(String targetTime) {
        try {
            return LocalTime.parse(targetTime.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Vaqt formati noto'g'ri! To'g'ri format: HH:mm:ss.SSS   Masalan: 14:30:00.000"
            );
        }
    }

    public static String formatNow() {
        return LocalTime.now().format(FORMATTER);
    }

    public static String formatRemainingMs(long ms) {
        if (ms < 0) ms = 0;
        long minutes = ms / 60_000;
        long seconds = (ms % 60_000) / 1_000;
        long millis  = ms % 1_000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
