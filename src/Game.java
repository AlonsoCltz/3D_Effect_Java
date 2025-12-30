public class Game {
    public static void main(String[] args) {
        UI ui = new UI();
        Thread uiThread = new Thread(ui);
        uiThread.start();
    }
}
