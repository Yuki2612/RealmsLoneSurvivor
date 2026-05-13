package gameproject.environment;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import gameproject.ImageManager;

public class Decoration extends Obstacle {
    private String spriteKey;

    public Decoration(int x, int y, int width, int height, String spriteKey) {
        super(x, y, width, height);
        this.spriteKey = spriteKey;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public void takeDamage(int dmg) {
        // Không nhận sát thương
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        BufferedImage img = ImageManager.get(spriteKey);
        if (img != null) {
            // Cỏ và trang trí thường được vẽ lệch xuống một chút để tạo độ sâu
            g.drawImage(img, x, y, width, height, null);
        }
    }
}
