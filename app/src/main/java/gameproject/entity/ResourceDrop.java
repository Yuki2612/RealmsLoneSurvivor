package gameproject.entity;
import gameproject.GamePanel;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import gameproject.ImageManager;
import gameproject.meta.PlayerData;

public class ResourceDrop {
    public enum Type {
        GOLD, SOUL
    }

    public float x, y;
    public Type type;
    public int amount;
    public long expireTime;
    private float velX, velY;
    private boolean isBeingCollected = false;

    public ResourceDrop(float x, float y, Type type, int amount, long expireTime) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.amount = amount;
        this.expireTime = expireTime;

        double angle = Math.random() * Math.PI * 2;
        float force = (float) (Math.random() * 3 + 1.5);
        this.velX = (float) Math.cos(angle) * force;
        this.velY = (float) Math.sin(angle) * force;
    }

    public void update(float playerX, float playerY) {
        if (!isBeingCollected) {
            x += velX;
            y += velY;
            velX *= 0.94f;
            velY *= 0.94f;

            float dx = playerX - x;
            float dy = playerY - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 120) {
                isBeingCollected = true;
            }
        } else {
            float dx = playerX - x;
            float dy = playerY - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 5) {
                x += (dx / dist) * 15;
                y += (dy / dist) * 15;
            }
        }
    }

    public boolean isCollected(float playerX, float playerY) {
        float dx = playerX - x;
        float dy = playerY - y;
        return (dx * dx + dy * dy) < 400;
    }

    public void applyToPlayer() {
        if (type == Type.GOLD) {
            PlayerData.gold += amount;
        } else {
            PlayerData.soulStones += amount;
        }
    }

    public void draw(java.awt.Graphics g) {
        long currentTime = GamePanel.getTickTime();
        long remaining = expireTime - currentTime;

        // Nhấp nháy khi sắp biến mất (3 giây cuối)
        if (remaining < 3000) {
            if ((remaining / 150) % 2 == 0) return;
        }

        java.awt.image.BufferedImage img = ImageManager.get(type == Type.GOLD ? "gold" : "soul");
        int drawX = (int) Math.round(x);
        int drawY = (int) Math.round(y);

        if (img != null) {
            g.drawImage(img, drawX - 12, drawY - 12, 24, 24, null);
        } else {
            g.setColor(type == Type.GOLD ? java.awt.Color.YELLOW : java.awt.Color.CYAN);
            g.fillOval(drawX - 6, drawY - 6, 12, 12);
        }

        // Vẽ số lượng
        if (amount > 1) {
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g.setColor(java.awt.Color.BLACK);
            g.drawString("x" + amount, drawX + 11, drawY + 11);
            g.setColor(java.awt.Color.WHITE);
            g.drawString("x" + amount, drawX + 10, drawY + 10);
        }
    }
}
