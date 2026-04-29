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
        super(startX, startY, 80, (int) ((500 + (surviveTimeSeconds * 3)) * 1.5f), 1f, Color.DARK_GRAY);
        this.isBoss = true;
        this.surviveTime = surviveTimeSeconds;
    }

    @Override
    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
        // Miễn nhiễm đẩy lùi
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH) {
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

        float dx = playerX - x;
        float dy = playerY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float currentSpeed = speed * speedMultiplier;
        float moveX = 0, moveY = 0;
        if (distance > 0) {
            moveX = (dx / distance) * currentSpeed;
            moveY = (dy / distance) * currentSpeed;
        }
        applyPhysicsAndBounds(moveX, moveY, screenW, screenH);
    }

    private void triggerSummon() {
        int tier = Math.min(5, 1 + surviveTime / 120); // Tăng cấp độ quái mỗi 2 phút
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
        java.awt.image.BufferedImage img = ImageManager.get("boss3");
        if (img != null) {
            g.drawImage(img, (int) x - 20, (int) y - 40, size + 40, size + 40, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect((int) x, (int) y, size, size);
        }

        g.setColor(Color.RED);
        g.fillRect((int) x, (int) y + size, size, 6);
        g.setColor(Color.GREEN);
        int hpWidth = (int) ((float) hp / maxHp * size);
        g.fillRect((int) x, (int) y + size, hpWidth, 6);
    }
}