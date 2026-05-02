package gameproject.entity;

import gameproject.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class TankBoss extends Enemy {
    private int surviveTime;
    private boolean[] thresholds = { false, false, false }; // 75%, 50%, 25%
    private java.util.List<Enemy> spawnedEnemies = new java.util.ArrayList<>();

    public TankBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 80, (int) ((600 + (surviveTimeSeconds * 4)) * 1.5f), 1f, Color.DARK_GRAY);
        this.isBoss = true;
        this.surviveTime = surviveTimeSeconds;
    }

    @Override
    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
        // Miễn nhiễm đẩy lùi
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {
        // Kiểm tra ngưỡng máu để triệu hồi
        float hpPercent = (float) hp / maxHp;
        if (hpPercent <= 0.75f && !thresholds[0]) {
            triggerSummon();
            thresholds[0] = true;
        } else if (hpPercent <= 0.50f && !thresholds[1]) {
            triggerSummon();
            thresholds[1] = true;
        } else if (hpPercent <= 0.25f && !thresholds[2]) {
            triggerSummon();
            thresholds[2] = true;
        }

        // Điều khiển di chuyển bằng AI tập trung
        EnemyController.moveEnemy(this, panel, speedMultiplier);
    }

    private void triggerSummon() {
        int tier = Math.min(5, 1 + surviveTime / 60); // Tăng cấp độ quái mỗi 2 phút
        // Triệu hồi đa dạng chủng loại quái tương xứng với giai đoạn
        spawnedEnemies.add(new NormalEnemy(x - 40, y, tier, surviveTime));
        spawnedEnemies.add(new ShooterEnemy(x + 40, y, tier, surviveTime));

        if (tier >= 2) {
            spawnedEnemies.add(new NormalEnemy(x, y - 40, tier, surviveTime));
        }
        if (tier >= 3) {
            spawnedEnemies.add(new CannoneerEnemy(x, y + 40, tier, surviveTime));
        }
        if (tier >= 4) {
            spawnedEnemies.add(new ShooterEnemy(x - 60, y - 60, tier, surviveTime));
            spawnedEnemies.add(new CannoneerEnemy(x + 60, y + 60, tier, surviveTime));
        }

        // Hiệu ứng âm thanh khi triệu hồi
        gameproject.SoundManager.play("explosion");
    }

    @Override
    public java.util.List<Enemy> summon() {
        if (spawnedEnemies.isEmpty())
            return null;
        java.util.List<Enemy> result = new java.util.ArrayList<>(spawnedEnemies);
        spawnedEnemies.clear();
        return result;
    }

    @Override
    public void draw(Graphics g) {
        drawSprite(g, "boss3");
    }
}