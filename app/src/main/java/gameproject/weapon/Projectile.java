package gameproject.weapon;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Projectile {
    private float x, y;
    public float startX, startY;
    public float speedX, speedY;
    public int size = 10;
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

    public boolean isExplosive = false;
    public float explosionRadius = 0;
    public long expirationTime = 0; // 0 = không hết hạn theo thời gian

    // THÊM: Hỗ trợ đạn rượt (Homing)
    public boolean isHoming = false;
    public gameproject.Player targetPlayer = null;
    public float homingTurnSpeed = 0.05f;

    // THÊM: Hỗ trợ khói/ma tím
    public boolean isPurpleGhost = false;

    // THÊM: Hỗ trợ hình ảnh cho đạn
    public String spriteKey = null;

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
        if (isHoming && targetPlayer != null) {
            float targetDx = (targetPlayer.getX() + targetPlayer.SIZE / 2) - x;
            float targetDy = (targetPlayer.getY() + targetPlayer.SIZE / 2) - y;
            float dist = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);

            if (dist > 0) {
                targetDx /= dist;
                targetDy /= dist;

                // Nới lỏng quỹ đạo từ từ (Lerp hướng)
                float currentSpeed = (float) Math.sqrt(speedX * speedX + speedY * speedY);
                if (currentSpeed == 0)
                    currentSpeed = 1;

                float dirX = speedX / currentSpeed;
                float dirY = speedY / currentSpeed;

                dirX += (targetDx - dirX) * homingTurnSpeed;
                dirY += (targetDy - dirY) * homingTurnSpeed;

                // Chuẩn hóa lại
                float newDist = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                speedX = (dirX / newDist) * currentSpeed;
                speedY = (dirY / newDist) * currentSpeed;
            }
        }

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

    public boolean isFairyBullet = false;
    public boolean isPriestBullet = false;

    public void draw(Graphics g) {
        int drawX = (int) Math.round(x);
        int drawY = (int) Math.round(y);

        if (isRailgun) {
            g.setColor(Color.CYAN);
            g.fillRect(drawX, drawY, size * 3, size * 3);
        } else if (isFairyBullet) {
            // Viên đạn phát sáng xanh lá giống năng lượng ma thuật
            Graphics2D g2d = (Graphics2D) g.create();
            int glowSize = size + 12;
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.25f));
            g2d.setColor(new Color(100, 255, 100));
            g2d.fillOval(drawX - 6, drawY - 6, glowSize, glowSize);
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
            g2d.setColor(new Color(50, 220, 50));
            g2d.fillOval(drawX - 3, drawY - 3, size + 6, size + 6);
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
            g2d.setColor(new Color(180, 255, 180));
            g2d.fillOval(drawX, drawY, size, size);
            g2d.dispose();
        } else if (isPriestBullet) {
            // Viên đạn phát sáng vàng (Boss Priest)
            Graphics2D g2d = (Graphics2D) g.create();
            int glowSize = size + 12;
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.25f));
            g2d.setColor(new Color(255, 255, 100));
            g2d.fillOval(drawX - 6, drawY - 6, glowSize, glowSize);
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
            g2d.setColor(new Color(220, 220, 50));
            g2d.fillOval(drawX - 3, drawY - 3, size + 6, size + 6);
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
            g2d.setColor(new Color(255, 255, 200));
            g2d.fillOval(drawX, drawY, size, size);
            g2d.dispose();
        } else if (isPurpleGhost) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            java.awt.geom.Point2D center = new java.awt.geom.Point2D.Float(drawX + size / 2f, drawY + size / 2f);
            float radius = size / 2f + 8f;
            float[] dist = { 0.0f, 0.6f, 1.0f };
            Color[] colors = { new Color(255, 150, 255), new Color(150, 0, 255), new Color(50, 0, 100, 0) };
            java.awt.RadialGradientPaint paint = new java.awt.RadialGradientPaint(center, radius, dist, colors);
            g2d.setPaint(paint);
            g2d.fillOval(drawX - 8, drawY - 8, size + 16, size + 16);

            // Lõi đen ma thuật (Dark Core)
            g2d.setColor(new Color(20, 0, 30));
            g2d.fillOval(drawX + size / 4, drawY + size / 4, size / 2, size / 2);

            g2d.dispose();
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
            if (spriteKey != null) {
                java.awt.image.BufferedImage img = gameproject.ImageManager.get(spriteKey);
                if (img != null) {
                    g.drawImage(img, drawX, drawY, size, size, null);
                    return;
                }
            }
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