package gameproject;

public class App {
    public static void main(String[] args) {
        System.setProperty("sun.java2d.d3d", "true");
        gameproject.meta.PlayerData.load();
        gameproject.meta.AchievementManager.getInstance();
        GamePanel gamePanel = new GamePanel();
        new GameWindow(gamePanel);
    }
}