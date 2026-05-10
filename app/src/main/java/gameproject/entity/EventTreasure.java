package gameproject.entity;

import gameproject.GamePanel;
import gameproject.ImageManager;
import gameproject.SoundManager;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class EventTreasure {
    public float x, y;
    public int size = 60;
    public boolean opened = false;

    public EventTreasure(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, size, size);
    }

    public void draw(Graphics2D g) {
        BufferedImage img = ImageManager.get("treasure");
        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        if (img != null) {
            g.drawImage(img, dx, dy, size, size, null);
        } else {
            g.setColor(java.awt.Color.ORANGE);
            g.fillRect(dx, dy, size, size);
        }
    }

    public void interact(GamePanel panel) {
        if (opened) return;
        opened = true;

        long currentTime = GamePanel.getTickTime();
        double chance = Math.random();
        if (chance < 0.75) { // Tỉ lệ phần thưởng 75%, Mimic 25%
            // Reward: Rơi vật phẩm hoặc nâng cấp trực tiếp
            SoundManager.play("pickup");
            
            // Tỉ lệ nhận nâng cấp đặc biệt (10%) - Mở menu chọn nâng cấp chỉ số ngay lập tức
            if (Math.random() < 0.1) {
                panel.triggerNormalUpgrade();
                panel.vfxManager.showWaveBanner("STAT UPGRADE GRANTED!", java.awt.Color.YELLOW, currentTime);
            } else {
                // Rơi Vàng và Linh hồn bằng hàm chuẩn của game
                int goldAmount = 60 + (int)(Math.random() * 60);
                synchronized (panel.entityManager.resourceDrops) {
                    panel.entityManager.spawnResource(x, y, ResourceDrop.Type.GOLD, goldAmount, currentTime, 15000);
                    
                    // Rơi Linh hồn (25% tỉ lệ)
                    if (Math.random() < 0.25) {
                        panel.entityManager.spawnResource(x, y, ResourceDrop.Type.SOUL, 5, currentTime, 15000);
                    }
                }
                panel.vfxManager.showWaveBanner("TREASURE OPENED!", java.awt.Color.CYAN, currentTime);
            }
        } else {
            // MIMIC!
            SoundManager.play("explosion");
            synchronized (panel.entityManager.enemies) {
                panel.entityManager.enemies.add(new Mimic(x, y, panel.entityManager.waveCount));
            }
            panel.vfxManager.showWaveBanner("IT'S A MIMIC!", java.awt.Color.RED, currentTime);
            gameproject.meta.AchievementManager.getInstance().onSecretTriggered("mimic_trap");
        }
    }
}
