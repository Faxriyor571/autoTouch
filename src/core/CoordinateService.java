package core;

import model.Coordinate;

import java.awt.*;

public class CoordinateService {

    private static final int POINT_COUNT = 4;

    private final Coordinate[] points = new Coordinate[] {
            new Coordinate(0, 0, "Nuqta 1"),
            new Coordinate(0, 0, "Nuqta 2"),
            new Coordinate(0, 0, "Nuqta 3"),
            new Coordinate(0, 0, "Nuqta 4")
    };

    public Coordinate captureCurrentMousePosition() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point point = pointerInfo.getLocation();
        return new Coordinate(point.x, point.y);
    }

    public Coordinate[] getAllPoints() {
        return points;
    }

    public void savePoint(int index, int x, int y) {
        if (index < 0 || index >= POINT_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " noto'g'ri. 0-" + (POINT_COUNT - 1) + " oralig'ida bo'lishi kerak."
            );
        }
        points[index].setX(x);
        points[index].setY(y);
    }
}
