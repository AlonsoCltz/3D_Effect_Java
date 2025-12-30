import java.awt.Color;

public class CollectableObject {
    private final int x;
    private final int y;
    private final String name;
    private final Color color;
    boolean collected = false;
    public CollectableObject(int x, int y, String name, Color color) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.color = color;
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

    public Color getColor() {
        return color;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }
}