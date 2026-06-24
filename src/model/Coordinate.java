package model;

public class Coordinate {

    private String name;
    private int x;
    private int y;

    public Coordinate(String name, int x, int y) {
        this.name = name;
        this.x    = x;
        this.y    = y;
    }

    public String getName()          { return name; }
    public int    getX()             { return x;    }
    public int    getY()             { return y;    }

    public void setName(String name) { this.name = name; }
    public void setX(int x)          { this.x = x;      }
    public void setY(int y)          { this.y = y;      }

    @Override
    public String toString() {
        return "X=" + x + "  Y=" + y;
    }
}
