package core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class TimerService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Timer clockTimer;
    private Timer countdownTimer;

    public void startClock(Consumer<String> onTick) {
        stopClock();
        clockTimer = new Timer("clock-timer", true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String time = LocalTime.now().format(FORMATTER);
                onTick.accept(time);
            }
        }, 0, 100);
    }

    public void stopClock() {
        if (clockTimer != null) {
            clockTimer.cancel();
            clockTimer = null;
        }
    }

    public void startCountdown(String targetTime, Runnable onReached) {
        LocalTime target;
        try {
            target = LocalTime.parse(targetTime.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Vaqt formati noto'g'ri! To'g'ri format: HH:mm:ss.SSS  Masalan: 14:30:00.000"
            );
        }

        stopCountdown();
        countdownTimer = new Timer("countdown-timer", true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                if (!now.isBefore(target)) {
                    stopCountdown();
                    onReached.run();
                }
            }
        }, 0, 100);
    }

    public void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }
}
