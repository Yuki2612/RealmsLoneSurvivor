package gameproject.entity;

import gameproject.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

public class TeleporterBoss extends Enemy {
    private int actionTimer = 0;
    private Random rand = new Random();

    public TeleporterBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 60, 450 + (surviveTimeSeconds * 3), 0.9f, Color.MAGENTA);
        this.isBoss = true;
    }

    private int teleportsLeft = 0;
    private int teleportGap = 0;
    private java.util.List<gameproject.weapon.Projectile> mines = new java.util.ArrayList<>();

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {
        actionTimer--;
        if (actionTimer <= 0) {
            actionTimer = 300; // Cooldown dài hơn chút
            teleportsLeft = 4; // Dịch chuyển 4 lần
            teleportGap = 0;
        }

        if (teleportsLeft > 0) {
            teleportGap--;
            if (teleportGap <= 0) {
                // Trước khi dịch chuyển, để lại 1 quả bom
                gameproject.weapon.Projectile bomb = new gameproject.weapon.Projectile(x, y, x, y, 0, 100f);
                bomb.isEnemyBullet = true;
                bomb.isExplosive = true;
                bomb.explosionRadius = 100;
                bomb.damage = 1;
                bomb.expirationTime = gameproject.GamePanel.getTickTime() + 1000; // Nổ sau 1s
                mines.add(bomb);

                // Dịch chuyển
                float randomAngle = (float) (rand.nextDouble() * 2 * Math.PI);
                this.x = playerX + (float) Math.cos(randomAngle) * 150f;
                this.y = playerY + (float) Math.sin(randomAngle) * 150f;
                this.x = Math.max(0, Math.min(this.x, screenW - size));
                this.y = Math.max(0, Math.min(this.y, screenH - size));

                teleportsLeft--;
                teleportGap = 20; // 20 frame giữa mỗi lần dịch chuyển
            }
            return;
        }

        // Sử dụng bộ não AI tập trung
        EnemyController.moveEnemy(this, panel, speedMultiplier);
    }

    @Override
    public java.util.List<gameproject.weapon.Projectile> shoot() {
        if (mines.isEmpty())
            return null;
        java.util.List<gameproject.weapon.Projectile> result = new java.util.ArrayList<>(mines);
        mines.clear();
        return result;
    }

    @Override
    public void draw(Graphics g) {
        drawSprite(g, "boss2");
    }
}