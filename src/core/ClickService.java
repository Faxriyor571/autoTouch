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
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public void clickAll(List<Coordinate> points, int delayMillis) {
        if (points == null || points.isEmpty()) return;
        for (Coordinate point : points) {
            robot.mouseMove(point.getX(), point.getY());
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            if (delayMillis > 0) robot.delay(delayMillis);
        }
    }
}
