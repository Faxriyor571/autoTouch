package core;

import model.Coordinate;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
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

    public void clickAll(List<Coordinate> points, int delayMillis) {
        if (points == null || points.isEmpty()) return;

        int last = points.size() - 1;

        for (int i = 0; i <= last; i++) {
            Coordinate point = points.get(i);
            robot.mouseMove(point.getX(), point.getY());
            press();

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
    }

    private void press() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
}
