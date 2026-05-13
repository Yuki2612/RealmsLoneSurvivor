package gameproject.environment;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import gameproject.ImageManager;

public class Altar extends Obstacle {
    private String spriteKey;
    private long startTime;
    private int runeCount = 0;

    public Altar(int x, int y, int width, int height, String spriteKey) {
        super(x, y, width, height);
        this.spriteKey = spriteKey;
        this.startTime = System.currentTimeMillis();

        // Sử dụng kích thước hitbox 380x260
        int hbW = 380;
        int hbH = 260;
        // Căn giữa theo tâm x (x + 32)
        float hbx = x + 32 - hbW / 2.0f;
        // Căn theo chân bệ đá (y + 84)
        float hby = y + 84 - hbH;
        this.hitbox = new AABBHitbox(hbx, hby, hbW, hbH);
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public void takeDamage(int dmg) {
        // Bất tử giống Wall
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    public int getRuneCount() {
        return runeCount;
    }

    public void addRune() {
        if (runeCount < 4)
            runeCount++;
    }

    @Override
    public float getBottomY() {
        return y + height;
    }

    @Override
    public void render(Graphics2D g) {
        BufferedImage[] frames = ImageManager.getAnimation(spriteKey);
        if (frames != null && frames.length > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            int delay = 150; // 150ms mỗi khung hình
            int index = (int) ((elapsed / delay) % frames.length);
            BufferedImage frame = frames[index];

            // Altar 384x384 (Giảm 25% từ 512)
            // Offset: -160px (để căn giữa 384 vào 64px tile)
            g.drawImage(frame, x - 160, y - 300, 384, 384, null);

            // Vẽ các Rune đã thu thập
            BufferedImage runeImg = ImageManager.get("rune");
            if (runeImg != null) {
                // Vị trí 4 góc bệ đá (phối cảnh 2.5D: Y lớn hơn là ở phía trước)
                int[][] runeOffsets = {
                        { -100, -80 }, { 100, -80 }, // 2 cái phía sau
                        { -130, 10 }, { 130, 10 } // 2 cái phía trước
                };
                for (int i = 0; i < runeCount; i++) {
                    // Vẽ Rune (căn giữa theo offset) - Tăng kích cỡ lên 40x40
                    g.drawImage(runeImg, x + 32 + runeOffsets[i][0] - 20, y + 32 + runeOffsets[i][1] - 20, 40, 40,
                            null);
                }
            }
        } else {
            // Dự phòng nếu không load được ảnh
            g.setColor(new java.awt.Color(120, 0, 180));
            g.fillRect(x - 32, y - 32, 128, 128);
            g.setColor(java.awt.Color.WHITE);
            g.drawString("ALTAR NOT LOADED", x - 30, y);
        }

        // Hiển thị hitbox khi bật trong Setting
        if (gameproject.GamePanel.showHitboxes && hitbox != null) {
            g.setColor(java.awt.Color.RED);
            hitbox.draw(g);
        }
    }
}
