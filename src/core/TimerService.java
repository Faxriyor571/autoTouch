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
    private volatile ScheduledFuture<?> countdownFuture;

    // ─── CLOCK ───────────────────────────────────────────────────

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

    // ─── COUNTDOWN ───────────────────────────────────────────────

    public void startCountdown(String targetTime,
                               Consumer<Long> onTick,
                               Consumer<Long> onReached) {
        LocalTime target     = parse(targetTime);
        long      targetNano = target.toNanoOfDay();

        stopCountdown();

        // Faza 1: Scheduler — 200ms qolguncha
        countdownFuture = scheduler.scheduleAtFixedRate(() -> {

            long remainingMs = (targetNano - LocalTime.now().toNanoOfDay()) / 1_000_000;

            if (remainingMs > 200) {
                onTick.accept(remainingMs);

            } else if (remainingMs >= 0) {
                // 200ms qoldi — schedulerni to'xtatib precision thread ishga tushiramiz
                stopCountdown();
                launchPrecisionThread(targetNano, onTick, onReached);
            }

        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    private void launchPrecisionThread(long targetNano,
                                       Consumer<Long> onTick,
                                       Consumer<Long> onReached) {
        Thread t = new Thread(() -> {

            // Faza 2: Thread.sleep(1) — 20ms qolguncha
            while (true) {
                long remainingMs = (targetNano - LocalTime.now().toNanoOfDay()) / 1_000_000;
                if (remainingMs <= 20) break;
                onTick.accept(remainingMs);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }

            // Faza 3: System.nanoTime busy-wait — oxirgi 20ms
            // LocalTime.now() obyekt yaratadi → GC bosimi
            // System.nanoTime() primitive long qaytaradi → GC yo'q
            long anchorLocalNano = LocalTime.now().toNanoOfDay();
            long anchorSysNano   = System.nanoTime();
            long targetSysNano   = anchorSysNano + (targetNano - anchorLocalNano);

            while (System.nanoTime() < targetSysNano) {
                Thread.onSpinWait();
            }

            // Kechikish
            long delayMs = Math.max(0,
                    (System.nanoTime() - targetSysNano) / 1_000_000L);

            onReached.accept(delayMs);

        }, "precision-trigger-thread");

        t.setPriority(Thread.MAX_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

    public void stopCountdown() {
        ScheduledFuture<?> f = countdownFuture;
        if (f != null && !f.isCancelled()) {
            f.cancel(false);
            countdownFuture = null;
        }
    }

    // ─── LIFECYCLE ───────────────────────────────────────────────

    public void shutdown() {
        stopClock();
        stopCountdown();
        scheduler.shutdownNow();
    }

    // ─── HELPERS ─────────────────────────────────────────────────

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
