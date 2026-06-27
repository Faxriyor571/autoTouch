package core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public class TimerService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final long COUNTDOWN_TICK_MS = 25;
    private static final long PRECISION_UI_TICK_MS = 10;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);
    private final ThreadPoolExecutor precisionExecutor;

    private ScheduledFuture<?> clockFuture;
    private volatile ScheduledFuture<?> countdownFuture;
    private volatile Future<?> precisionFuture;
    private final AtomicLong countdownGeneration = new AtomicLong();

    public TimerService() {
        precisionExecutor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread thread = new Thread(runnable, "precision-trigger-thread");
                    thread.setPriority(Thread.MAX_PRIORITY);
                    thread.setDaemon(true);
                    return thread;
                }
        );
        // Critical vaqtga yaqin yangi OS thread yaratish jitterini yo'qotadi.
        precisionExecutor.prestartAllCoreThreads();
    }

    // ─── CLOCK ───────────────────────────────────────────────────

    public void startClock(Consumer<String> onTick) {
        startClock(onTick, () -> false);
    }

    public void startClock(Consumer<String> onTick,
                           BooleanSupplier paused) {
        stopClock();
        clockFuture = scheduler.scheduleAtFixedRate(() -> {
            if (paused.getAsBoolean()) return;
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

    public void startCountdownAt(long targetNanoTime,
                                 Consumer<Long> onTick,
                                 Runnable onPrepare,
                                 Consumer<Long> onReached) {
        startCountdownDynamic(() -> targetNanoTime, onTick, onPrepare, onReached);
    }

    public void startCountdownDynamic(LongSupplier targetNanoTimeSupplier,
                                      Consumer<Long> onTick,
                                      Runnable onPrepare,
                                      Consumer<Long> onReached) {

        stopCountdown();
        long generation = countdownGeneration.incrementAndGet();

        // Faza 1: Scheduler — 200ms qolguncha
        countdownFuture = scheduler.scheduleAtFixedRate(() -> {
            if (generation != countdownGeneration.get()) return;

            long targetNanoTime = targetNanoTimeSupplier.getAsLong();
            long remainingMs =
                    (targetNanoTime - System.nanoTime()) / 1_000_000;

            if (remainingMs > 200) {
                onTick.accept(remainingMs);

            } else {
                // 200ms qoldi — schedulerni to'xtatib precision thread ishga tushiramiz
                cancelScheduledCountdown();
                launchPrecisionThread(
                        generation,
                        targetNanoTime,
                        onTick,
                        onPrepare,
                        onReached
                );
            }

        }, 0, COUNTDOWN_TICK_MS, TimeUnit.MILLISECONDS);
    }

    private void launchPrecisionThread(long generation,
                                       long targetNanoTime,
                                       Consumer<Long> onTick,
                                       Runnable onPrepare,
                                       Consumer<Long> onReached) {
        Runnable precisionTask = () -> {

            // Faza 2: Thread.sleep(1) — 20ms qolguncha
            long lastReportedMs = Long.MAX_VALUE;
            while (true) {
                if (isCancelled(generation)) return;
                long remainingMs =
                        (targetNanoTime - System.nanoTime()) / 1_000_000;
                if (remainingMs <= 20) break;
                if (lastReportedMs == Long.MAX_VALUE
                        || lastReportedMs - remainingMs >= PRECISION_UI_TICK_MS) {
                    onTick.accept(remainingMs);
                    lastReportedMs = remainingMs;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }

            // Critical window: UI yangilanmaydi, birinchi click joyi tayyorlanadi.
            if (isCancelled(generation)) return;
            onPrepare.run();

            // Faza 3: server vaqti bilan anchor qilingan monotonic clock.
            while (System.nanoTime() < targetNanoTime) {
                if (isCancelled(generation)) return;
                Thread.onSpinWait();
            }

            // Kechikish
            long delayMs = Math.max(0,
                    (System.nanoTime() - targetNanoTime) / 1_000_000L);

            if (!isCancelled(generation)) {
                onReached.accept(delayMs);
            }

        };

        if (generation != countdownGeneration.get()) return;
        precisionFuture = precisionExecutor.submit(precisionTask);
    }

    public void stopCountdown() {
        countdownGeneration.incrementAndGet();
        cancelScheduledCountdown();

        Future<?> f = precisionFuture;
        if (f != null) {
            f.cancel(true);
            precisionFuture = null;
        }
    }

    private void cancelScheduledCountdown() {
        ScheduledFuture<?> f = countdownFuture;
        if (f != null && !f.isCancelled()) {
            f.cancel(false);
            countdownFuture = null;
        }
    }

    private boolean isCancelled(long generation) {
        return Thread.currentThread().isInterrupted()
                || generation != countdownGeneration.get();
    }

    // ─── LIFECYCLE ───────────────────────────────────────────────

    public void shutdown() {
        stopClock();
        stopCountdown();
        scheduler.shutdownNow();
        precisionExecutor.shutdownNow();
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
