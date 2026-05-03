package gameproject.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import gameproject.weapon.Projectile;

public class CannoneerEnemy extends Enemy {
    private Random rand = new Random();
    private int tier;

    private int shootCooldown;
    private int currentCooldown = 0;
    private boolean canShoot = false;
    private float targetPX, targetPY;

    public CannoneerEnemy(float startX, float startY, int tier, int surviveTimeSeconds) {
        super(startX, startY, 40, 0, 0, Color.ORANGE); // Size to hơn chút
        this.isBoss = false;
        this.tier = tier;

        switch (tier) {
            case 1 -> {
                this.maxHp = 30;
                this.speed = 0.8f;
                this.shootCooldown = 300;
            } // 5s
            case 2 -> {
                this.maxHp = 50;
                this.speed = 0.9f;
                this.shootCooldown = 270;
            }
            case 3 -> {
                this.maxHp = 80;
                this.speed = 1.0f;
                this.shootCooldown = 240;
            }
            case 4 -> {
                this.maxHp = 120;
                this.speed = 1.1f;
                this.shootCooldown = 210;
            }
            default -> {
                this.maxHp = 160;
                this.speed = 1.2f;
                this.shootCooldown = 180;
                this.tier = 5;
            }
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 60.0f) * 0.08f));
        this.hp = this.maxHp;
        this.currentCooldown = rand.nextInt(120) + 60;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, gameproject.GamePanel panel) {
        targetPX = playerX;
        targetPY = playerY;

        float dx = playerX - x;
        float dy = playerY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float currentSpeed = speed * speedMultiplier;
        float moveX = 0, moveY = 0;

        // Sử dụng bộ não AI tập trung để xử lý di chuyển và va chạm (Sliding Collision)
        EnemyController.moveEnemy(this, panel, speedMultiplier);

        if (currentCooldown > 0)
            currentCooldown--;
        if (currentCooldown <= 0 && distance <= 500) {
            canShoot = true;
            currentCooldown = shootCooldown;
        }
    }

    @Override
    public java.util.List<Projectile> shoot() {
        if (canShoot) {
            canShoot = false;
            // Đạn bay chậm
            Projectile p = new Projectile(x, y, targetPX, targetPY, 0.5f, 600f);
            p.isEnemyBullet = true;
            p.isExplosive = true; // Cờ đạn nổ
            p.explosionRadius = 40 + (tier * 10); // Bán kính nổ tăng theo tier
            p.damage = 1;
            return java.util.List.of(p);
        }
        return null;
    }

    @Override
    public void draw(Graphics g) {
        drawSprite(g, "enemy" + tier);
        // Đánh dấu Pháo thủ bằng chấm đỏ
        g.setColor(Color.RED);
        g.fillOval((int) x + size / 2 - 4, (int) y - 12, 8, 8);
    }
}