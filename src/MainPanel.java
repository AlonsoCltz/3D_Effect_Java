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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        double[] zBuffer = new double[screenW];

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
            zBuffer[col] = perpendicular;

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

        drawCollectables(g2d, screenW, screenH, rad, fov, zBuffer, verticalOffset);
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

    private void drawCollectables(Graphics2D g2d, int screenW, int screenH, double playerRad, double fov, double[] zBuffer, int verticalOffset) {
        List<CollectableObject> objects = new ArrayList<>(collection.getWorldObjects());
        objects.sort((o1, o2) -> {
            double d1 = Math.pow(o1.getX() - posX, 2) + Math.pow(o1.getY() - posY, 2);
            double d2 = Math.pow(o2.getX() - posX, 2) + Math.pow(o2.getY() - posY, 2);
            return Double.compare(d2, d1);
        });

        double objSize = 0.5; // side length of the cube in world units

        for (CollectableObject obj : objects) {
            if (obj.isCollected()) continue;

            double centerX = obj.getX() + 0.5;
            double centerY = obj.getY() + 0.5;
            double dx = centerX - posX;
            double dy = centerY - posY;
            double distance = Math.hypot(dx, dy);
            // Don't render if too close (will be collected soon anyway)
            if (distance < 0.8) continue;

            double angleToObj = Math.atan2(dy, dx);
            double relAngle = normalizeAngle(angleToObj - playerRad);
            if (Math.abs(relAngle) > fov * 0.6) continue;

            double half = objSize * 0.5;
            double minX = centerX - half;
            double maxX = centerX + half;
            double minY = centerY - half;
            double maxY = centerY + half;

            // Track per-column top edges to build a proper top-face polygon
            int[] topEdgeY = new int[screenW];
            int[] topEdgeFarY = new int[screenW];
            boolean[] columnVisible = new boolean[screenW];
            int leftCol = -1, rightCol = -1;

            for (int col = 0; col < screenW; col++) {
                double rayAngle = (playerRad - fov * 0.5) + ((double) col / (double) screenW) * fov;
                double rayDirX = Math.cos(rayAngle);
                double rayDirY = Math.sin(rayAngle);

                double tNear = Double.NEGATIVE_INFINITY;
                double tFar = Double.POSITIVE_INFINITY;
                int hitFace = 0;

                // X slabs
                if (Math.abs(rayDirX) < 1e-9) {
                    if (posX < minX || posX > maxX) continue;
                } else {
                    double t1 = (minX - posX) / rayDirX;
                    double t2 = (maxX - posX) / rayDirX;
                    double tEnter = Math.min(t1, t2);
                    double tExit = Math.max(t1, t2);
                    if (tEnter > tNear) {
                        tNear = tEnter;
                        hitFace = (t1 < t2) ? -1 : 1;
                    }
                    tFar = Math.min(tFar, tExit);
                }

                // Y slabs
                if (Math.abs(rayDirY) < 1e-9) {
                    if (posY < minY || posY > maxY) continue;
                } else {
                    double t1 = (minY - posY) / rayDirY;
                    double t2 = (maxY - posY) / rayDirY;
                    double tEnter = Math.min(t1, t2);
                    double tExit = Math.max(t1, t2);
                    if (tEnter > tNear) {
                        tNear = tEnter;
                        hitFace = (t1 < t2) ? -2 : 2;
                    }
                    tFar = Math.min(tFar, tExit);
                }

                if (tNear > tFar || tFar < 0 || tNear <= 0) continue;

                double perpNear = tNear * Math.cos(rayAngle - playerRad);
                if (perpNear <= 0.8) continue;
                if (perpNear >= zBuffer[col]) continue;

                int fullWallHeightNear = (int) (screenH / perpNear);
                int wallBottomNear = Math.min(screenH - 1, fullWallHeightNear + verticalOffset);
                int objHeight = Math.max(2, (int) (fullWallHeightNear * objSize));
                int objTopNear = Math.max(0, wallBottomNear - objHeight);

                if (wallBottomNear <= objTopNear) continue;

                double perpFar = tFar * Math.cos(rayAngle - playerRad);
                perpFar = Math.max(perpFar, 0.8);
                int fullWallHeightFar = (int) (screenH / perpFar);
                int wallBottomFar = Math.min(screenH - 1, fullWallHeightFar + verticalOffset);
                int objHeightFar = Math.max(2, (int) (fullWallHeightFar * objSize));
                int objTopFar = Math.max(0, wallBottomFar - objHeightFar);

                // Track top edge for top face
                topEdgeY[col] = objTopNear;
                topEdgeFarY[col] = objTopFar;
                columnVisible[col] = true;
                if (leftCol < 0) leftCol = col;
                rightCol = col;

                Color faceColor = obj.getColor();
                switch (hitFace) {
                    case 1:
                        faceColor = faceColor.darker();
                        break;
                    case -1:
                        faceColor = new Color(
                            Math.max(0, faceColor.getRed() - 40),
                            Math.max(0, faceColor.getGreen() - 40),
                            Math.max(0, faceColor.getBlue() - 40)
                        );
                        break;
                    case 2:
                        break;
                    case -2:
                        faceColor = faceColor.brighter();
                        break;
                    default:
                        break;
                }
                double shade = Math.max(0.3, 1.0 / (1.0 + perpNear * 0.08));
                g2d.setColor(applyShade(faceColor, shade));
                g2d.drawLine(col, objTopNear, col, wallBottomNear);
            }

            if (leftCol >= 0 && rightCol > leftCol) {
                int visibleCount = 0;
                for (int col = leftCol; col <= rightCol; col++) {
                    if (columnVisible[col]) visibleCount++;
                }

                if (visibleCount > 1) {
                    int[] xPts = new int[visibleCount * 2];
                    int[] yPts = new int[visibleCount * 2];
                    int idx = 0;

                    for (int col = leftCol; col <= rightCol; col++) {
                        if (!columnVisible[col]) continue;
                        xPts[idx] = col;
                        yPts[idx] = topEdgeY[col];
                        idx++;
                    }
                    for (int col = rightCol; col >= leftCol; col--) {
                        if (!columnVisible[col]) continue;
                        xPts[idx] = col;
                        yPts[idx] = Math.min(topEdgeY[col], topEdgeFarY[col]);
                        idx++;
                    }

                    Color topColor = obj.getColor().brighter();
                    double shade = Math.max(0.5, 1.0 / (1.0 + distance * 0.06));
                    g2d.setColor(applyShade(topColor, shade));
                    g2d.fillPolygon(xPts, yPts, idx);
                }
            }
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

            // Collect when very close
            if (distance < 0.5) {
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
}
