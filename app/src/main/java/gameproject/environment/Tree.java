package gameproject.environment;

import java.awt.Color;
import java.awt.Graphics2D;

public class Tree extends Obstacle {
    private int hp = 200; // Cây kiên cố hơn thùng gỗ

    public Tree(int x, int y, int width, int height) {
        super(x, y, width, height);
        // Chuyển sang AABB để khớp với hình dáng thân cây gỗ (dẹt và ngang)
        int hbW = 40;
        int hbH = 20;
        float hbx = x + (width - hbW) / 2.0f;
        float hby = y + height * 1.15f - hbH / 2.0f;
        this.hitbox = new AABBHitbox(hbx, hby, hbW, hbH);
    }

    @Override
    public boolean isSolid() {
        return true;
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
    public float getBottomY() {
        return y + height * 1.15f;
    }

    @Override
    public void render(Graphics2D g) {
        int dx = x;
        int dy = y;

        java.awt.image.BufferedImage img = gameproject.ImageManager.get("tree");
        if (img != null) {
            g.drawImage(img, dx - 64, dy - 64, 192, 192, null);
        } else {
            g.setColor(new Color(34, 139, 34));
            g.fillOval(dx + 5, dy + 5, width - 10, height - 10);
            g.setColor(new Color(0, 100, 0));
            g.drawOval(dx + 5, dy + 5, width - 10, height - 10);
        }

        if (gameproject.GamePanel.showHitboxes && hitbox != null) {
            g.setColor(Color.BLUE);
            hitbox.draw(g);
        }
    }
}