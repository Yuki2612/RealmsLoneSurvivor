package gameproject.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import gameproject.weapon.Projectile;

public class ShooterEnemy extends Enemy {
    private Random rand = new Random();
    private int tier;

    private int shootCooldown;
    private int currentCooldown = 0;
    private boolean canShoot = false;
    private float targetPX, targetPY;

    public ShooterEnemy(float startX, float startY, int tier, int surviveTimeSeconds) {
        super(startX, startY, 30, 0, 0, Color.WHITE);
        this.isBoss = false;
        this.tier = tier;

        switch (tier) {
            case 1 -> {
                this.maxHp = 15;
                this.speed = 0.7f;
                this.shootCooldown = 215; // Giảm ~10% từ 240
            }
            case 2 -> {
                this.maxHp = 25;
                this.speed = 0.9f;
                this.shootCooldown = 190; // Giảm ~10% từ 210
            }
            case 3 -> {
                this.maxHp = 40;
                this.speed = 1.1f;
                this.shootCooldown = 160; // Giảm ~10% từ 180
            }
            case 4 -> {
                this.maxHp = 60;
                this.speed = 1.3f;
                this.shootCooldown = 135; // Giảm ~10% từ 150
            }
            default -> {
                this.maxHp = 80;
                this.speed = 1.5f;
                this.shootCooldown = 110; // Giảm ~10% từ 120
                this.tier = 5;
            }
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 30.0f) * 0.15f));
        this.hp = this.maxHp;
        this.currentCooldown = rand.nextInt(shootCooldown);
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
        if (currentCooldown <= 0 && distance <= 400) {
            canShoot = true;
            currentCooldown = shootCooldown;
        }
    }

    @Override
    public ArrayList<Projectile> shoot() {
        if (canShoot) {
            canShoot = false;
            float bulletSpeed = 0.3f + (tier * 0.04f); // Giảm tốc độ bay của đạn
            Projectile p = new Projectile(x, y, targetPX, targetPY, bulletSpeed, 800f);
            p.isEnemyBullet = true;
            p.spriteKey = "projectile";
            p.size = 20;
            p.damage = 1;
            return new ArrayList<>(java.util.List.of(p));
        }
        return null;
    }

    @Override
    public void draw(Graphics g) {
        // Sử dụng ảnh shooter cho tất cả các tier, không vẽ chấm màu
        drawSprite(g, "enemy_shooter");
    }
}