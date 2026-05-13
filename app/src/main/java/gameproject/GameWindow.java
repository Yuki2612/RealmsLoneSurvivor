package gameproject;

import javax.swing.JFrame;

public class GameWindow {
    public GameWindow(GamePanel gamePanel) {
        // Khai báo cục bộ và dùng luôn, không cần biến toàn cục thừa thãi
        JFrame jframe = new JFrame();

        jframe.setUndecorated(true);
        jframe.setExtendedState(JFrame.MAXIMIZED_BOTH);
        jframe.setTitle("REALMS: LONE SURVIVOR");

        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.add(gamePanel);

        jframe.pack();
        jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);
    }
}