import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyBoard implements Runnable, KeyListener {
    private final MainPanel panel;
    private final Thread moveThread;
    private final Thread rotateThread;
    private volatile boolean running = true;

    private volatile boolean forward;
    private volatile boolean backward;
    private volatile boolean strafeLeft;
    private volatile boolean strafeRight;
    private volatile boolean turnLeft;
    private volatile boolean turnRight;

    private final float moveSpeed = 3.0f;      // world units per second
    private final float rotateSpeed = 90.0f;   // degrees per second

    public KeyBoard(MainPanel panel) {
        this.panel = panel;
        moveThread = new Thread(this::movementLoop, "MoveLoop");
        rotateThread = new Thread(this::rotationLoop, "RotateLoop");
        moveThread.setDaemon(true);
        rotateThread.setDaemon(true);
    }

    @Override
    public void run() {
        start();
    }

    public void start() {
        moveThread.start();
        rotateThread.start();
    }

    public void stop() {
        running = false;
    }

    private void movementLoop() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;

            float forwardAmt = 0.0f;
            float strafeAmt = 0.0f;

            if (forward) forwardAmt += moveSpeed * dt;
            if (backward) forwardAmt -= moveSpeed * dt;
            if (strafeLeft) strafeAmt -= moveSpeed * dt;
            if (strafeRight) strafeAmt += moveSpeed * dt;

            if (forwardAmt != 0.0f || strafeAmt != 0.0f) {
                panel.move(forwardAmt, strafeAmt);
            }

            sleepBriefly();
        }
    }

    private void rotationLoop() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;

            float delta = 0.0f;
            if (turnLeft) delta -= rotateSpeed * dt;
            if (turnRight) delta += rotateSpeed * dt;

            if (delta != 0.0f) {
                panel.rotate(delta);
            }

            sleepBriefly();
        }
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                forward = true;
                break;
            case KeyEvent.VK_S:
                backward = true;
                break;
            case KeyEvent.VK_A:
                strafeLeft = true;
                break;
            case KeyEvent.VK_D:
                strafeRight = true;
                break;
            case KeyEvent.VK_LEFT:
                turnLeft = true;
                break;
            case KeyEvent.VK_RIGHT:
                turnRight = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                forward = false;
                break;
            case KeyEvent.VK_S:
                backward = false;
                break;
            case KeyEvent.VK_A:
                strafeLeft = false;
                break;
            case KeyEvent.VK_D:
                strafeRight = false;
                break;
            case KeyEvent.VK_LEFT:
                turnLeft = false;
                break;
            case KeyEvent.VK_RIGHT:
                turnRight = false;
                break;
            default:
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
