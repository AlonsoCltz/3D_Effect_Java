import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MapPanel extends JPanel {
    private static final int PADDING = 6;
    private static final Color BACKDROP = new Color(0, 0, 0, 160);
    private static final Color GRID_COLOR = new Color(255, 255, 255, 30);
    private static final Color PLAYER_COLOR = new Color(255, 80, 60);
    private static final Color VISITED_FLOOR = new Color(120, 160, 120, 110);

    private final int graphId;
    private int[][] map;
    private boolean[][] visited;
    private volatile float playerX;
    private volatile float playerY;
    private volatile float playerAngleDeg;
    private boolean expanded;
    private final Rectangle normalBounds = new Rectangle();

    public MapPanel(int graphID) {
        this.graphId = graphID;
        this.map = GraphStorage.getInstance().getGraph(graphID);
        if (map != null) {
            visited = new boolean[map.length][map[0].length];
        }
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpanded();
            }
        });
    }

    public void setState(int[][] newMap, float x, float y, float angleDeg) {
        if (newMap == null) return;

        // Re-init visited if the map reference changed or size differs.
        if (map != newMap || visited == null || visited.length != newMap.length || visited[0].length != newMap[0].length) {
            map = newMap;
            visited = new boolean[map.length][map[0].length];
        }

        playerX = x;
        playerY = y;
        playerAngleDeg = angleDeg;
        markVisited((int) y, (int) x);
        repaint();
    }

    public void setNormalBounds(Rectangle bounds) {
        normalBounds.setBounds(bounds);
        if (!expanded) {
            setBounds(normalBounds);
        }
    }

    public void handleParentResize() {
        applyBoundsForState();
    }

    private void markVisited(int row, int col) {
        if (visited == null || map == null) return;
        if (row < 0 || col < 0 || row >= visited.length || col >= visited[0].length) return;
        visited[row][col] = true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (map == null || visited == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        g2d.setColor(BACKDROP);
        g2d.fillRoundRect(0, 0, w, h, 12, 12);

        int mapCols = map[0].length;
        int mapRows = map.length;

        double usableW = Math.max(1, w - PADDING * 2);
        double usableH = Math.max(1, h - PADDING * 2);
        double scale = Math.min(usableW / mapCols, usableH / mapRows);
        double size = Math.max(1.0, scale);

        // Draw grid for orientation.
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1f));
        for (int r = 0; r <= mapRows; r++) {
            int y = (int) Math.round(PADDING + r * size);
            g2d.drawLine(PADDING, y, (int) Math.round(PADDING + mapCols * size), y);
        }
        for (int c = 0; c <= mapCols; c++) {
            int x = (int) Math.round(PADDING + c * size);
            g2d.drawLine(x, PADDING, x, (int) Math.round(PADDING + mapRows * size));
        }

        // Pass 1: draw visited floor so traveled alleys become visible.
        for (int r = 0; r < mapRows; r++) {
            for (int c = 0; c < mapCols; c++) {
                if (!visited[r][c]) continue;
                if (map[r][c] != 0) continue;
                int x = (int) Math.round(PADDING + c * size);
                int y = (int) Math.round(PADDING + r * size);
                int cellSize = (int) Math.ceil(size);

                g2d.setColor(VISITED_FLOOR);
                g2d.fillRect(x, y, cellSize, cellSize);
            }
        }

        // Pass 2: draw walls that are either visited or adjacent to a visited cell to reveal corridor edges.
        for (int r = 0; r < mapRows; r++) {
            for (int c = 0; c < mapCols; c++) {
                int tile = map[r][c];
                if (tile == 0) continue;
                if (!visited[r][c] && !isAdjacentToVisited(r, c)) continue;

                int x = (int) Math.round(PADDING + c * size);
                int y = (int) Math.round(PADDING + r * size);
                int cellSize = (int) Math.ceil(size);

                g2d.setColor(colorForTile(tile));
                g2d.fillRect(x, y, cellSize, cellSize);
            }
        }

        // Draw player indicator.
        double px = PADDING + playerX * size;
        double py = PADDING + playerY * size;
        double rad = Math.toRadians(playerAngleDeg);
        int radius = (int) Math.max(4, size * 1.5);
        int[] xs = new int[]{
            (int) Math.round(px + Math.cos(rad) * radius),
            (int) Math.round(px + Math.cos(rad + Math.PI * 0.75) * radius),
            (int) Math.round(px + Math.cos(rad - Math.PI * 0.75) * radius)
        };
        int[] ys = new int[]{
            (int) Math.round(py + Math.sin(rad) * radius),
            (int) Math.round(py + Math.sin(rad + Math.PI * 0.75) * radius),
            (int) Math.round(py + Math.sin(rad - Math.PI * 0.75) * radius)
        };
        g2d.setColor(PLAYER_COLOR);
        g2d.fillPolygon(xs, ys, 3);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawPolygon(xs, ys, 3);

        g2d.dispose();
    }

    private Color colorForTile(int tile) {
        switch (tile) {
            case 1:
                return new Color(170, 170, 170);
            case 2:
                return new Color(90, 90, 90);
            case 3:
                return new Color(200, 40, 40);
            case 4:
                return new Color(120, 20, 20);
            default:
                return new Color(200, 200, 200);
        }
    }

    private boolean isAdjacentToVisited(int row, int col) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (nr < 0 || nc < 0 || nr >= visited.length || nc >= visited[0].length) continue;
                if (visited[nr][nc]) return true;
            }
        }
        return false;
    }

    private void toggleExpanded() {
        expanded = !expanded;
        applyBoundsForState();
    }

    private void applyBoundsForState() {
        Container parent = getParent();
        if (parent == null) return;

        if (expanded) {
            int pw = parent.getWidth();
            int ph = parent.getHeight();
            int size = (int) (Math.min(pw, ph) * 0.8);
            int x = (pw - size) / 2;
            int y = (ph - size) / 2;
            setBounds(x, y, size, size);
        } else {
            setBounds(normalBounds);
        }

        revalidate();
        repaint();
    }
}
