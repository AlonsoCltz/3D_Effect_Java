import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MainPanel extends JPanel implements ActionListener {
    private final GraphStorage storage = GraphStorage.getInstance();
    private final Collection collection = new Collection();
    private final Timer timer;
    private final MapPanel miniMap;
    private int graphId = 1;
    private volatile float posX = 1f;
    private volatile float posY = 1f;
    private volatile float angleDeg = 0.0f;

    private static final int MINIMAP_SIZE = 180;
    private static final int MINIMAP_MARGIN = 10;

    public MainPanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        setLayout(null);
        miniMap = new MapPanel(graphId);
        collection.loadTestObjects();
        add(miniMap);
        positionMiniMap();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                positionMiniMap();
            }
        });

        timer = new Timer(16, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int[][] map = storage.getGraph(graphId);
        if (map == null || map.length == 0 || map[0].length == 0) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawFrame(g2d, map);
    }

    private void drawFrame(Graphics2D g2d, int[][] map) {
        int screenW = getWidth();
        int screenH = getHeight();
        double rad = Math.toRadians(angleDeg);
        double fov = Math.PI / 3.0; // 60 degrees
        double maxDepth = 50.0;
        double step = 0.02;
        int verticalOffset = screenH / 8; // push view down to simulate looking slightly upward

        // Base floor fill; no separate ceiling rendering.
        g2d.setColor(new Color(70, 70, 70));
        g2d.fillRect(0, 0, screenW, screenH);

        for (int col = 0; col < screenW; col++) {
            double rayAngle = (rad - fov * 0.5) + ((double) col / (double) screenW) * fov;
            double raySin = Math.sin(rayAngle);
            double rayCos = Math.cos(rayAngle);

            double distance = 0.0;
            boolean hit = false;
            int hitTile = 0;

            while (distance < maxDepth) {
                double sampleX = posX + rayCos * distance;
                double sampleY = posY + raySin * distance;

                if (sampleX < 0.0 || sampleX >= map[0].length || sampleY < 0.0 || sampleY >= map.length) {
                    distance = maxDepth;
                    break;
                }

                int tileValue = map[(int) sampleY][(int) sampleX];
                if (tileValue != 0) {
                    hit = true;
                    hitTile = tileValue;
                    break;
                }

                distance += step;
            }

            double perpendicular = hit ? distance * Math.cos(rayAngle - rad) : maxDepth;
            double clampedDist = Math.max(perpendicular, 0.0001);
            int wallHeight = (int) (screenH / clampedDist);
            // Keep walls starting at the top; only reduce their visible height to show more floor.
            int wallTop = 0;
            int wallBottom = Math.min(screenH - 1, wallHeight + verticalOffset);

            Color baseColor = colorForWall(hitTile);
            double shade = Math.max(0.2, 1.0 / (1.0 + clampedDist * 0.1));
            g2d.setColor(applyShade(baseColor, shade));
            g2d.drawLine(col, wallTop, col, wallBottom);

            if (wallBottom < screenH - 1) {
                double floorShade = Math.max(0.1, 0.8 - (double) wallBottom / screenH);
                g2d.setColor(applyShade(new Color(90, 90, 90), floorShade));
                g2d.drawLine(col, wallBottom + 1, col, screenH - 1);
            }
        }

        drawCollectables(g2d, screenW, screenH, rad, fov);
    }

    private Color colorForWall(int tileValue) {
        switch (tileValue) {
            case 1:
                return new Color(170, 170, 170);
            case 2:
                return new Color(90, 90, 90);
            case 3:
                return new Color(200, 40, 40);
            case 4:
                return new Color(120, 20, 20);
            default:
                return Color.WHITE;
        }
    }

    private Color applyShade(Color base, double factor) {
        factor = Math.max(0.0, Math.min(1.0, factor));
        int r = (int) (base.getRed() * factor);
        int g = (int) (base.getGreen() * factor);
        int b = (int) (base.getBlue() * factor);
        return new Color(r, g, b);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int[][] map = storage.getGraph(graphId);
        if (map != null) {
            miniMap.setState(map, posX, posY, angleDeg);
        }
        handleCollectables(map);
        repaint();
    }

    public void move(float forwardAmount, float strafeAmount) {
        int[][] map = storage.getGraph(graphId);
        if (map == null) return;

        double rad = Math.toRadians(angleDeg);
        float dx = (float) (Math.cos(rad) * forwardAmount - Math.sin(rad) * strafeAmount);
        float dy = (float) (Math.sin(rad) * forwardAmount + Math.cos(rad) * strafeAmount);
        tryMove(posX + dx, posY + dy, map);
    }

    public void rotate(float deltaDeg) {
        angleDeg = wrapAngle(angleDeg + deltaDeg);
    }

    private void tryMove(float nextX, float nextY, int[][] map) {
        if (nextX < 1 || nextY < 1 || nextX >= map[0].length - 1 || nextY >= map.length - 1) return;
        if (map[(int) nextY][(int) nextX] == 0) {
            posX = nextX;
            posY = nextY;
        }
    }

    private float wrapAngle(float deg) {
        float wrapped = deg % 360.0f;
        return wrapped < 0 ? wrapped + 360.0f : wrapped;
    }

    private void positionMiniMap() {
        int size = Math.min(MINIMAP_SIZE, Math.min(getWidth() / 3, getHeight() / 3));
        if (size <= 0) return;
        int x = Math.max(MINIMAP_MARGIN, getWidth() - size - MINIMAP_MARGIN);
        int y = MINIMAP_MARGIN;
        Rectangle bounds = new Rectangle(x, y, size, size);
        miniMap.setNormalBounds(bounds);
        miniMap.handleParentResize();
    }

    private void drawCollectables(Graphics2D g2d, int screenW, int screenH, double playerRad, double fov) {
        for (CollectableObject obj : collection.getWorldObjects()) {
            if (obj.isCollected()) continue;

            double dx = (obj.getX() + 0.5) - posX;
            double dy = (obj.getY() + 0.5) - posY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            double objAngle = Math.atan2(dy, dx);
            double diff = normalizeAngle(objAngle - playerRad);

            // Direction from cube toward player, normalized; used to decide which faces are visible.
            double toPlayerX = -dx;
            double toPlayerY = -dy;
            double len = Math.max(0.0001, Math.hypot(toPlayerX, toPlayerY));
            toPlayerX /= len;
            toPlayerY /= len;

            if (Math.abs(diff) > fov * 0.6) continue;

            double screenX = (diff / fov + 0.5) * screenW;
            int size = (int) Math.max(6, 160 / Math.max(distance, 0.1));
            int x = (int) Math.round(screenX - size * 0.5);
            int y = screenH / 2 - size / 2;

            // Keep cube orientation fixed in world; side visibility depends on where the player stands
            drawCube(g2d, obj.getColor(), x, y, size, toPlayerX, toPlayerY);
        }
    }

    private void handleCollectables(int[][] map) {
        if (map == null) return;
        for (CollectableObject obj : collection.getWorldObjects()) {
            if (obj.isCollected()) continue;
            if (!isWalkable(map, obj.getX(), obj.getY())) continue;

            double dx = (obj.getX() + 0.5) - posX;
            double dy = (obj.getY() + 0.5) - posY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 0.6) {
                collection.collect(obj);
            }
        }
    }

    private boolean isWalkable(int[][] map, int x, int y) {
        if (x < 0 || y < 0 || y >= map.length || x >= map[0].length) return false;
        return map[y][x] == 0;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2.0;
        while (angle < -Math.PI) angle += Math.PI * 2.0;
        return angle;
    }

    private void drawCube(Graphics2D g2d, Color base, int x, int y, int size, double toPlayerX, double toPlayerY) {
        // toPlayerX/Y: normalized vector from cube center toward player.
        int depthX = (int) Math.round(toPlayerX * size * 0.4);
        int depthY = (int) Math.round(toPlayerY * size * 0.4);

        int fx0 = x;
        int fx1 = x + size;
        int fy0 = y;
        int fy1 = y + size;

        int[] frontX = {fx0, fx1, fx1, fx0};
        int[] frontY = {fy0, fy0, fy1, fy1};
        double frontShade = 0.35 + 0.55 * ((toPlayerX * 0.5) + 0.5);
        g2d.setColor(applyShade(base, frontShade));
        g2d.fillPolygon(frontX, frontY, 4);

        int[] sideX = {fx1, fx1 - depthX, fx1 - depthX, fx1};
        int[] sideY = {fy0, fy0 - depthY, fy1 - depthY, fy1};
        double sideShade = 0.45 + 0.35 * ((toPlayerY * 0.5) + 0.5);
        g2d.setColor(applyShade(base, sideShade));
        g2d.fillPolygon(sideX, sideY, 4);

        int[] topX = {fx0, fx0 - depthX, fx1 - depthX, fx1};
        int[] topY = {fy0, fy0 - depthY, fy0 - depthY, fy0};
        g2d.setColor(applyShade(base, 0.7));
        g2d.fillPolygon(topX, topY, 4);

        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(frontX, frontY, 4);
        g2d.drawPolygon(sideX, sideY, 4);
        g2d.drawPolygon(topX, topY, 4);
    }
}
