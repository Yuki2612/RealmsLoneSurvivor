package gameproject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

public abstract class Enemy {
    protected float x, y;
    protected float speed;
    protected int size;
    protected int hp, maxHp;
    protected Color color;
    protected float kbX = 0, kbY = 0;
    public boolean isBoss = false;

    public Enemy(float x, float y, int size, int maxHp, float speed, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.speed = speed;
        this.color = color;
    }

    // Hàm update trừu tượng: Bắt buộc các loại quái con phải tự viết logic di
    // chuyển riêng
    public abstract void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies,
            int screenW, int screenH);

    // Xử lý di chuyển, đẩy lùi và chặn viền màn hình dùng chung
    protected void applyPhysicsAndBounds(float moveX, float moveY, int screenW, int screenH) {
        x += moveX;
        y += moveY;
        x += kbX;
        y += kbY;
        kbX *= 0.85f;
        kbY *= 0.85f;
        x = Math.max(0, Math.min(x, screenW - size));
        y = Math.max(0, Math.min(y, screenH - size));
    }

    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
        float dx = this.x - sourceX;
        float dy = this.y - sourceY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            this.kbX = (dx / dist) * (isBoss ? pushForce * 0.2f : pushForce);
            this.kbY = (dy / dist) * (isBoss ? pushForce * 0.2f : pushForce);
        }
    }

    public void takeDamage(int damage) {
        this.hp -= damage;
    }

    public boolean isDead() {
        return this.hp <= 0;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, size, size);
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect((int) x, (int) y, size, size);
        g.setColor(Color.RED);
        g.fillRect((int) x, (int) y - 8, size, 4);
        g.setColor(Color.GREEN);
        int hpWidth = (int) ((float) hp / maxHp * size);
        g.fillRect((int) x, (int) y - 8, hpWidth, 4);
    }
}