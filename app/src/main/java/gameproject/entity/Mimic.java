package gameproject.entity;

import gameproject.GamePanel;
import gameproject.ImageManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Mimic extends Enemy {
    private long spawnTime;

    public Mimic(float x, float y, int wave) {
        // Máu Mimic: 150 + wave * 40
        super(x, y, 70, 300 + (wave * 70), 3.5f, Color.MAGENTA);
        this.spawnTime = GamePanel.getTickTime();
        this.isBoss = false;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies,
            int screenW, int screenH, GamePanel panel) {
        // Sử dụng bộ não AI tập trung (Sliding Collision, Obstacle Avoidance)
        EnemyController.moveEnemy(this, panel, speedMultiplier);
    }

    @Override
    public void draw(Graphics g) {
        drawSprite(g, "mimic");
    }

    @Override
    public int getExpValue() {
        // Giảm EXP nhận được từ Mimic (chỉ bằng 1/4 máu tối đa) để cân bằng game
        return maxHp / 4;
    }
}
