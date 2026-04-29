package gameproject.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import gameproject.weapon.Projectile;

public class ShooterEnemy extends Enemy {
    private float wanderX, wanderY;
    private int wanderTimer = 0;
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
                this.speed = 1.0f;
                this.shootCooldown = 240;
            } // 3s
            case 2 -> {
                this.maxHp = 25;
                this.speed = 1.2f;
                this.shootCooldown = 210;
            } // 2.5s
            case 3 -> {
                this.maxHp = 40;
                this.speed = 1.5f;
                this.shootCooldown = 180;
            } // 2s
            case 4 -> {
                this.maxHp = 60;
                this.speed = 1.8f;
                this.shootCooldown = 150;
            } // 1.5s
            default -> {
                this.maxHp = 80;
                this.speed = 2.0f;
                this.shootCooldown = 120;
                this.tier = 5;
            } // 1s
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 60.0f) * 0.05f));
        this.hp = this.maxHp;
        this.currentCooldown = rand.nextInt(shootCooldown); // Đừng bắn cùng lúc
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH) {
        targetPX = playerX;
        targetPY = playerY;

        float dx = playerX - x;
        float dy = playerY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float currentSpeed = speed * speedMultiplier;
        float moveX = 0, moveY = 0;

        if (distance < 800 && distance > 0) { // Tầm nhìn xa hơn quái thường
            moveX = (dx / distance) * currentSpeed;
            moveY = (dy / distance) * currentSpeed;
        } else {
            // Đi về phía player chậm rãi khi ngoài tầm nhìn
            moveX = (dx / distance) * (currentSpeed * 0.4f);
            moveY = (dy / distance) * (currentSpeed * 0.4f);
        }

        for (Enemy other : allEnemies) {
            if (other == this || other.isBoss)
                continue;
            float odx = this.x - other.x;
            float ody = this.y - other.y;
            float oDist = (float) Math.sqrt(odx * odx + ody * ody);
            if (oDist > 0 && oDist < 30) {
                moveX += (odx / oDist) * 0.8f;
                moveY += (ody / oDist) * 0.8f;
            }
        }
        applyPhysicsAndBounds(moveX, moveY, screenW, screenH);

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
            float bulletSpeed = 0.4f + (tier * 0.05f); // Đạn nhanh dần theo tier
            Projectile p = new Projectile(x, y, targetPX, targetPY, bulletSpeed, 800f);
            p.isEnemyBullet = true;
            p.damage = 1;
            return new ArrayList<>(java.util.List.of(p));
        }
        return null;
    }

    @Override
    public void draw(Graphics g) {
        // Tạm mượn ảnh enemy, bạn có thể thêm "shooter1" -> "shooter5" sau
        drawSprite(g, "enemy" + tier);

        // Vẽ thêm dấu hiệu là Xạ thủ (ví dụ: một chấm nhỏ trên đầu)
        g.setColor(Color.YELLOW);
        g.fillOval((int) x + size / 2 - 4, (int) y - 12, 8, 8);
    }
}