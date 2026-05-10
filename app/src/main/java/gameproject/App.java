package gameproject;

public class App {
    public static void main(String[] args) {
        gameproject.meta.PlayerData.load();
        gameproject.meta.AchievementManager.getInstance();
        GamePanel gamePanel = new GamePanel();
        new GameWindow(gamePanel);
    }
}