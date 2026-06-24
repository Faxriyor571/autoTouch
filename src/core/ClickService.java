package core;

import model.Coordinate;

import java.awt.*;
import java.awt.event.InputEvent;

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

    public Point getCurrentMousePosition() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        return pointerInfo.getLocation();
    }

    public void clickAll(Coordinate[] points, int delayMillis) {
        if (points == null) return;

        for (Coordinate point : points) {
            if (point.getX() == 0 && point.getY() == 0) continue;

            clickAt(point.getX(), point.getY());

            if (delayMillis > 0) {
                robot.delay(delayMillis);
            }
        }
    }

    public void clickAt(int x, int y) {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
}
