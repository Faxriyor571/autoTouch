package core;

import model.Coordinate;

import java.awt.*;

public class CoordinateService {

    public Coordinate captureCurrentMousePosition() {

        PointerInfo pointerInfo =
                MouseInfo.getPointerInfo();

        Point point = pointerInfo.getLocation();

        return new Coordinate(
                point.x,
                point.y
        );
    }
}