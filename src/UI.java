import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

public class UI implements Runnable{
    JFrame mainFrame;
    MainPanel mainPanel;
    KeyBoard keyBoard;

    @Override
    public void run() {
        mainFrame = new JFrame("3D Effects");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainPanel = new MainPanel();
        keyBoard = new KeyBoard(mainPanel);

        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.addKeyListener(keyBoard);
        mainPanel.addKeyListener(keyBoard);

        mainFrame.setSize(800, 600);
        mainFrame.setVisible(true);

        keyBoard.start();

        SwingUtilities.invokeLater(() -> mainPanel.requestFocusInWindow());
    }
}
