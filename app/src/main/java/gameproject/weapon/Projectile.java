package gameproject.weapon;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Projectile {
    private float x, y;
    public float startX, startY;
    public float speedX, speedY;
    private int size = 10;
    private boolean active = true;

    public int bouncesLeft = 0;
    public int damage = 10;
    public float maxRange;

    public boolean isEnemyBullet = false;
    public boolean isShocking = false;
    public boolean isPoisonous = false;
    public boolean isPlayerExplosive = false;
    public boolean isHellfire = false;
    public boolean isRailgun = false;
    public boolean isCrit = false; // Crit hit — hiện text vàng tại địch khi trúng

    // THÊM: Hỗ trợ đạn nổ cho Pháo thủ
    public boolean isExplosive = false;
    public float explosionRadius = 0;
    public long expirationTime = 0; // 0 = không hết hạn theo thời gian

    public gameproject.entity.Enemy ignoredEnemy = null;

    public Projectile(float startX, float startY, float targetX, float targetY, float speedMultiplier, float maxRange) {
        this.x = startX + 15;
        this.y = startY + 15;
        this.startX = this.x;
        this.startY = this.y;
        this.maxRange = maxRange;

        float dx = targetX - this.x;
        float dy = targetY - this.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance == 0)
            distance = 1;

        float baseSpeed = 12f;
        float finalSpeed = baseSpeed * speedMultiplier;
        this.speedX = (dx / distance) * finalSpeed;
        this.speedY = (dy / distance) * finalSpeed;
    }

    public void update(int worldWidth, int worldHeight) {
        x += speedX;
        y += speedY;

        float distTraveled = (float) Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
        if (distTraveled > maxRange || x < 0 || x > worldWidth || y < 0 || y > worldHeight) {
            active = false;
        }
        if (expirationTime > 0 && gameproject.GamePanel.getTickTime() > expirationTime) {
            active = false;
        }
    }

    public void draw(Graphics g) {
        int drawX = (int) Math.round(x);
        int drawY = (int) Math.round(y);

        if (isRailgun) {
            g.setColor(Color.CYAN);
            g.fillRect(drawX, drawY, size * 3, size * 3);
        } else if (isHellfire) {
            g.setColor(Color.MAGENTA);
            g.fillOval(drawX, drawY, size + 8, size + 8);
            g.setColor(Color.WHITE);
            g.drawOval(drawX, drawY, size + 8, size + 8);
        } else if (isPoisonous) {
            g.setColor(Color.GREEN);
            g.fillOval(drawX, drawY, size, size);
            g.setColor(new Color(0, 100, 0));
            g.drawOval(drawX, drawY, size, size);
        } else if (isExplosive) {
            g.setColor(Color.RED);
            g.fillOval(drawX, drawY, size + 4, size + 4);
            g.setColor(Color.YELLOW);
            g.drawOval(drawX, drawY, size + 4, size + 4);
        } else if (isShocking) {
            g.setColor(Color.YELLOW);
            g.fillOval(drawX, drawY, size + 2, size + 2);
            g.setColor(Color.CYAN);
            g.drawOval(drawX, drawY, size + 2, size + 2);
        } else {
            g.setColor(isEnemyBullet ? Color.ORANGE : Color.WHITE);
            g.fillOval(drawX, drawY, size, size);
            g.setColor(Color.BLACK);
            g.drawOval(drawX, drawY, size, size);
        }
    }

    public float getX() {
        return x;
    } // Thêm getter cho X

    public float getY() {
        return y;
    } // Thêm getter cho Y

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, size, size);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}