package model;

public class Coordinate {

    private int x;
    private int y;
    private String name;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
        this.name = "Nuqta";
    }

    public Coordinate(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getName() {
        return name;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "X=" + x + "  Y=" + y;
    }
}
