package gameproject.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import gameproject.weapon.Projectile;

public class WizardEnemy extends Enemy {
    private Random rand = new Random();
    private int tier;

    private int shootCooldown;
    private int currentCooldown = 0;
    private boolean canShoot = false;
    private float targetPX, targetPY;

    public WizardEnemy(float startX, float startY, int tier, int surviveTimeSeconds) {
        super(startX, startY, 40, 0, 0, Color.ORANGE);
        this.isBoss = false;
        this.tier = tier;

        switch (tier) {
            case 1 -> {
                this.maxHp = 25;
                this.speed = 0.5f;
                this.shootCooldown = 270; // Giảm ~10% từ 300
            }
            case 2 -> {
                this.maxHp = 40;
                this.speed = 0.7f;
                this.shootCooldown = 235; // Giảm ~10% từ 260
            }
            case 3 -> {
                this.maxHp = 70;
                this.speed = 0.9f;
                this.shootCooldown = 200; // Giảm ~10% từ 220
            }
            case 4 -> {
                this.maxHp = 100;
                this.speed = 1.1f;
                this.shootCooldown = 160; // Giảm ~10% từ 180
            }
            default -> {
                this.maxHp = 150;
                this.speed = 1.3f;
                this.shootCooldown = 125; // Giảm ~10% từ 140
                this.tier = 5;
            }
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 30.0f) * 0.15f));
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
            Projectile p = new Projectile(x, y, targetPX, targetPY, 0.35f, 600f); // Giảm tốc độ từ 0.5f
            p.isEnemyBullet = true;
            p.isExplosive = true;
            p.explosionRadius = 40 + (tier * 10);
            p.damage = 1;
            return java.util.List.of(p);
        }
        return null;
    }

    @Override
    public void draw(Graphics g) {
        // Sử dụng ảnh wizard cho tất cả các tier, không vẽ chấm màu
        drawSprite(g, "enemy_wizard");
    }
}
