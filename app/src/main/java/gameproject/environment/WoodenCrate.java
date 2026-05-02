package gameproject.environment;

import java.awt.Color;
import java.awt.Graphics2D;

public class WoodenCrate extends Obstacle {
    private int hp = 50;

    public WoodenCrate(int x, int y, int width, int height) {
        super(x, y, width, height);
        // TỐI ƯU: Chuyển sang "Hitbox chân" giống Cây và Đá
        // Điều này giúp quái có thể áp sát người chơi từ phía đối diện thùng gỗ dễ dàng hơn
        int hbW = 32;
        int hbH = 16; 
        this.hitbox = new AABBHitbox(x + (width - hbW) / 2f, y + height - hbH - 4, hbW, hbH);
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public float getBottomY() {
        // Tối ưu Y-sorting: Điểm chân của thùng gỗ nên ở gần đáy sprite
        // Giảm một chút để người chơi dễ dàng đứng "trước" thùng hơn
        return y + height - 8;
    }

    @Override
    public void takeDamage(int dmg) {
        this.hp -= dmg;
    }

    @Override
    public boolean isDestroyed() {
        return hp <= 0;
    }

    @Override
    public void render(Graphics2D g) {
        int dx = x;
        int dy = y;

        java.awt.image.BufferedImage img = gameproject.ImageManager.get("woodencrate");
        if (img != null) {
            g.drawImage(img, dx, dy, width, height, null);
        } else {
            g.setColor(new Color(139, 69, 19)); // Màu nâu gỗ
            g.fillRect(dx + 2, dy + 2, width - 4, height - 4);
            g.setColor(new Color(101, 50, 14));
            g.drawRect(dx + 2, dy + 2, width - 4, height - 4);
            g.drawLine(dx + 10, dy + 10, dx + width - 10, dy + height - 10);
            g.drawLine(dx + width - 10, dy + 10, dx + 10, dy + height - 10);
        }

        if (gameproject.GamePanel.showHitboxes && hitbox != null) {
            g.setColor(Color.BLUE);
            hitbox.draw(g);
        }
    }
}