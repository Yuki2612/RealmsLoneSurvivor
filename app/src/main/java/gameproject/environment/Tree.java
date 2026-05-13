package gameproject.environment;

import java.awt.Color;
import java.awt.Graphics2D;

public class Tree extends Obstacle {
    private int hp = 200; // Cây kiên cố hơn thùng gỗ

    private String spriteKey = "tree";

    public Tree(int x, int y, int width, int height) {
        this(x, y, width, height, "tree");
    }

    public Tree(int x, int y, int width, int height, String spriteKey) {
        super(x, y, width, height);
        this.spriteKey = spriteKey;
        
        // Chuyển sang AABB để khớp với hình dáng thân cây gỗ (dẹt và ngang)
        int hbW = spriteKey.equals("tree3") ? 32 : 40; // Gốc cây nhỏ hơn
        int hbH = 20;
        float hbx = x + (width - hbW) / 2.0f;
        float hby = y + height * (spriteKey.equals("tree3") ? 1.0f : 1.15f) - hbH / 2.0f;
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
        return y + height * (spriteKey.equals("tree3") ? 1.0f : 1.15f);
    }

    @Override
    public void render(Graphics2D g) {
        int dx = x;
        int dy = y;

        java.awt.image.BufferedImage img = gameproject.ImageManager.get(spriteKey);
        if (img != null) {
            if (spriteKey.equals("tree3")) {
                // Gốc cây vẽ nhỏ hơn và ít lệch hơn
                g.drawImage(img, dx - 16, dy - 16, 96, 96, null);
            } else {
                g.drawImage(img, dx - 64, dy - 64, 192, 192, null);
            }
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