package core;

import model.Coordinate;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.time.Instant;
import java.util.List;

public class ClickService {

    private final Robot robot;

    public ClickService() {
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(0);
        } catch (AWTException e) {
            throw new RuntimeException("Robot yaratishda xato: " + e.getMessage(), e);
        }
    }

    public void clickAt(Coordinate coord) {
        robot.mouseMove(coord.getX(), coord.getY());
        press();
    }

    public void prepareFirstClick(Coordinate coord) {
        robot.mouseMove(coord.getX(), coord.getY());
    }

    public ClickReport clickAll(List<Coordinate> points, int delayMillis) {
        return clickAll(points, delayMillis, false);
    }

    public ClickReport clickAllPrepared(List<Coordinate> points, int delayMillis) {
        return clickAll(points, delayMillis, true);
    }

    private ClickReport clickAll(List<Coordinate> points,
                                 int delayMillis,
                                 boolean firstPointPrepared) {
        if (points == null || points.isEmpty()) {
            return new ClickReport(0, 0, 0, 0, null);
        }

        int last = points.size() - 1;
        long firstClickNanoTime = 0;
        long lastClickNanoTime  = 0;
        int completedClicks = 0;
        int previousX = firstPointPrepared ? points.get(0).getX() : Integer.MIN_VALUE;
        int previousY = firstPointPrepared ? points.get(0).getY() : Integer.MIN_VALUE;

        for (int i = 0; i <= last; i++) {
            Coordinate point = points.get(i);
            if (point.getX() != previousX || point.getY() != previousY) {
                robot.mouseMove(point.getX(), point.getY());
                previousX = point.getX();
                previousY = point.getY();
            }
            press();
            long clickNanoTime = System.nanoTime();
            if (completedClicks == 0) {
                firstClickNanoTime = clickNanoTime;
            }
            lastClickNanoTime = clickNanoTime;
            completedClicks++;

            // Faqat nuqtalar ORASIDA kutamiz — oxirgi nuqtadan keyin emas
            if (i < last && delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Wall-clock faqat barcha clicklar tugagach olinadi. Critical loop ichida
        // LocalTime/Instant obyektlari yaratilmaydi.
        long anchorStartNano = System.nanoTime();
        Instant wallClockAnchor = Instant.now();
        long anchorEndNano = System.nanoTime();
        long wallClockAnchorNanoTime =
                anchorStartNano + (anchorEndNano - anchorStartNano) / 2;

        return new ClickReport(
                completedClicks,
                firstClickNanoTime,
                lastClickNanoTime,
                wallClockAnchorNanoTime,
                wallClockAnchor
        );
    }

    public record ClickReport(int clickCount,
                              long firstClickNanoTime,
                              long lastClickNanoTime,
                              long wallClockAnchorNanoTime,
                              Instant wallClockAnchor) {}

    private void press() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
}
