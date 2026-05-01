package gameproject.environment;

import java.awt.Color;
import java.awt.Graphics2D;

public class Rock extends Obstacle {

    public Rock(int x, int y, int width, int height) {
        super(x, y, width, height);
        // Dịch hitbox xuống chân đá cho thực tế hơn trong Top-down
        this.hitbox = new AABBHitbox(x + 5, y + height * 0.4f, width - 10, height * 0.6f);
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public void takeDamage(int dmg) {
    } // Bất tử

    @Override
    public boolean isDestroyed() {
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        int dx = x;
        int dy = y;

        java.awt.image.BufferedImage img = gameproject.ImageManager.get("rock");
        if (img != null) {
            g.drawImage(img, dx - 8, dy - 8, width + 16, height + 16, null);
        } else {
            g.setColor(new Color(128, 128, 128));
            g.fillRoundRect(dx + 5, dy + 5, width - 10, height - 10, 15, 15);
            g.setColor(Color.DARK_GRAY);
            g.drawRoundRect(dx + 5, dy + 5, width - 10, height - 10, 15, 15);
        }

        if (gameproject.GamePanel.showHitboxes && hitbox != null) {
            g.setColor(Color.BLUE);
            hitbox.draw(g);
        }
    }
}