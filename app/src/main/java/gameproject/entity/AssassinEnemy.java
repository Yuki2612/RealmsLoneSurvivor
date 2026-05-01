package gameproject.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.util.ArrayList;
import java.util.Random;

public class AssassinEnemy extends Enemy {
    private Random rand = new Random();
    private int tier;

    private boolean isInvisible = false;
    private int invisTimer = 0;
    private int invisCooldown;

    // Tốc độ lưu trữ để phục hồi sau khi tàng hình
    private float baseSpeed;

    public AssassinEnemy(float startX, float startY, int tier, int surviveTimeSeconds) {
        super(startX, startY, 30, 0, 0, Color.BLACK);
        this.isBoss = false;
        this.tier = tier;

        switch (tier) {
            case 1 -> {
                this.maxHp = 10;
                this.baseSpeed = 1.5f;
                this.invisCooldown = 300;
            } // 5s cooldown
            case 2 -> {
                this.maxHp = 15;
                this.baseSpeed = 1.8f;
                this.invisCooldown = 270;
            }
            case 3 -> {
                this.maxHp = 25;
                this.baseSpeed = 2.1f;
                this.invisCooldown = 240;
            }
            case 4 -> {
                this.maxHp = 35;
                this.baseSpeed = 2.5f;
                this.invisCooldown = 210;
            }
            default -> {
                this.maxHp = 50;
                this.baseSpeed = 3.0f;
                this.invisCooldown = 180;
                this.tier = 5;
            }
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 60.0f) * 0.05f));
        this.hp = this.maxHp;
        this.speed = this.baseSpeed;
        this.invisTimer = rand.nextInt(60); // Bắt đầu đếm ngược tàng hình
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, gameproject.GamePanel panel) {
        // Logic tàng hình
        if (isInvisible) {
            invisTimer--;
            if (invisTimer <= 0) {
                isInvisible = false;
                speed = baseSpeed; // Phục hồi tốc độ
                invisTimer = invisCooldown; // Chờ lần tàng hình tiếp theo
            }
        } else {
            invisTimer--;
            if (invisTimer <= 0) {
                isInvisible = true;
                speed = baseSpeed * 1.4f; // Tăng 80% tốc độ khi tàng hình
                invisTimer = 45 + (tier * 2); // Thời gian tàng hình tăng theo tier (1s -> 1.5s)
            }
        }

        // Sử dụng bộ não AI tập trung để xử lý di chuyển và va chạm (Sliding Collision)
        EnemyController.moveEnemy(this, panel, speedMultiplier);
    }

    // Ghi đè takeDamage: Kháng 100% sát thương đạn khi đang tàng hình
    @Override
    public void takeDamage(int damage, boolean isCrit, gameproject.VFXManager vfxManager, long currentTime) {
        if (!isInvisible) {
            super.takeDamage(damage, isCrit, vfxManager, currentTime);
        }
    }

    @Override
    public void takeDamage(int damage, gameproject.VFXManager vfxManager, long currentTime) {
        takeDamage(damage, false, vfxManager, currentTime);
    }

    @Override
    public void draw(Graphics g) {
        if (isInvisible) {
            // V\u1ebd th\u1ee7 c\u00f4ng \u0111\u1ec3 tr\u00e1nh xung \u0111\u1ed9t
            // composite v\u1edbi drawSprite()
            // drawSprite() ghi \u0111\u00e8 alpha v\u1ec1 1.0f n\u1ebfu kh\u00f4ng isDying
            // \u2014 n\u00ean ta b\u1ecf qua n\u00f3
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            java.awt.image.BufferedImage img = gameproject.ImageManager.get("enemy" + tier);
            if (img != null) {
                g2d.drawImage(img, (int) x - 10, (int) y - 20, size + 20, size + 20, null);
            } else {
                g2d.setColor(color);
                g2d.fillRect((int) x, (int) y, size, size);
            }
            g2d.dispose();
            // Kh\u00f4ng v\u1ebd HP bar v\u00e0 ch\u1ea5m \u0111en khi t\u00e0ng h\u00ecnh
            // \u2014 \u1ea9n ho\u00e0n to\u00e0n
        } else {
            drawSprite(g, "enemy" + tier);
            // Ch\u1ea5m \u0111en nh\u1eadn di\u1ec7n \u2014 ch\u1ec9 hi\u1ec7n khi
            // kh\u00f4ng t\u00e0ng h\u00ecnh
            g.setColor(Color.BLACK);
            g.fillOval((int) x + size / 2 - 4, (int) y - 12, 8, 8);
        }
    }
}