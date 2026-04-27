package gameproject;

public class App {
    public static void main(String[] args) {
        GamePanel gamePanel = new GamePanel();
        new GameWindow(gamePanel);
    }
}