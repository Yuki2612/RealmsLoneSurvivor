package gameproject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Projectile {
    private float x, y;
    private float startX, startY; // Tọa độ gốc để tính tầm bắn
    private float speedX, speedY;
    private int size = 10;
    private boolean active = true;

    // Các chỉ số có thể nâng cấp
    public int bouncesLeft = 0;
    public int damage = 10;

    // Giới hạn tầm bắn tương đương với tầm nhìn quái (300px)
    private final float MAX_RANGE = 300f;

    public Projectile(float startX, float startY, float targetX, float targetY, float speedMultiplier) {
        // Căn chỉnh để đạn bay ra từ tâm nhân vật
        this.x = startX + 15;
        this.y = startY + 15;
        this.startX = this.x; // Ghi lại điểm xuất phát
        this.startY = this.y;

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

    public void update(int screenWidth, int screenHeight) {
        x += speedX;
        y += speedY;

        // Tính quãng đường đạn hiện tại đã bay được so với điểm bắt đầu
        float distTraveled = (float) Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));

        // Tự hủy nếu bay quá tầm bắn tối đa hoặc bay ra khỏi màn hình
        if (distTraveled > MAX_RANGE || x < 0 || x > screenWidth || y < 0 || y > screenHeight) {
            active = false;
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillOval((int) x, (int) y, size, size);
    }

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