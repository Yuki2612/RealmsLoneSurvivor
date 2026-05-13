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
                this.baseSpeed = 0.9f;
                this.invisCooldown = 300;
            }
            case 2 -> {
                this.maxHp = 15;
                this.baseSpeed = 1.2f;
                this.invisCooldown = 270;
            }
            case 3 -> {
                this.maxHp = 25;
                this.baseSpeed = 1.5f;
                this.invisCooldown = 240;
            }
            case 4 -> {
                this.maxHp = 35;
                this.baseSpeed = 1.8f;
                this.invisCooldown = 210;
            }
            default -> {
                this.maxHp = 50;
                this.baseSpeed = 2.2f;
                this.invisCooldown = 180;
                this.tier = 5;
            }
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 30.0f) * 0.15f));
        this.hp = this.maxHp;
        this.speed = this.baseSpeed;
        this.invisTimer = rand.nextInt(60);
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, gameproject.GamePanel panel) {
        if (isInvisible) {
            invisTimer--;
            if (invisTimer <= 0) {
                isInvisible = false;
                speed = baseSpeed;
                invisTimer = invisCooldown;
            }
        } else {
            invisTimer--;
            if (invisTimer <= 0) {
                isInvisible = true;
                speed = baseSpeed * 1.6f;
                invisTimer = 45 + (tier * 2);
            }
        }

        EnemyController.moveEnemy(this, panel, speedMultiplier);
    }

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
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            
            // Hỗ trợ Animation khi tàng hình
            long now = gameproject.GamePanel.getTickTime();
            java.awt.image.BufferedImage[] anim = gameproject.ImageManager.getAnimation("enemy_assassin");
            java.awt.image.BufferedImage img = null;
            
            if (anim != null && anim.length > 0) {
                int index = (int) ((now / 150) % anim.length);
                img = anim[index];
            } else {
                img = gameproject.ImageManager.get("enemy_assassin");
            }

            if (img != null) {
                g2d.drawImage(img, (int) x - 10, (int) y - 20, size + 20, size + 20, null);
            } else {
                g2d.setColor(color);
                g2d.fillRect((int) x, (int) y, size, size);
            }
            g2d.dispose();
        } else {
            // Sử dụng ảnh assassin cho tất cả các tier, không vẽ chấm màu
            drawSprite(g, "enemy_assassin");
        }
    }
}