package core;

import model.Coordinate;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoordinateService {

    private final List<Coordinate> points = new ArrayList<>();

    public Coordinate addCurrentMousePosition() {
        Point p = MouseInfo.getPointerInfo().getLocation();
        int number = points.size() + 1;
        Coordinate coord = new Coordinate("Nuqta " + number, p.x, p.y);
        points.add(coord);
        System.out.println("[COORD] " + coord.getName() + " qo'shildi: X=" + p.x + " Y=" + p.y);
        return coord;
    }

    public void remove(Coordinate coord) {
        points.remove(coord);
        renumber();
        System.out.println("[COORD] Nuqta o'chirildi. Qolgan: " + points.size());
    }

    public List<Coordinate> getAll() {
        return Collections.unmodifiableList(points);
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    private void renumber() {
        for (int i = 0; i < points.size(); i++) {
            points.get(i).setName("Nuqta " + (i + 1));
        }
    }
}
