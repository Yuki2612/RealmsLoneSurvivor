package gameproject.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import gameproject.GamePanel;

public class EggEntity extends Enemy {
    private int tier;
    private int mapId;
    private int surviveTimeSeconds;
    private int hatchTimer;
    private boolean hatched = false;
    private boolean summoned = false;

    public EggEntity(float startX, float startY, int tier, int surviveTimeSeconds, int mapId) {
        // Trứng to (size 45) và đứng yên (speed 0)
        super(startX, startY, 45, 0, 0, Color.WHITE);
        this.tier = tier;
        this.mapId = mapId;
        this.surviveTimeSeconds = surviveTimeSeconds;
        this.hatchTimer = 600; // ~10 giây ở 60fps
        this.deathFadeDuration = 0; // Biến mất ngay lập tức khi nở/bị phá hủy

        // Máu trứng
        switch (tier) {
            case 1 -> this.maxHp = 50;
            case 2 -> this.maxHp = 100;
            case 3 -> this.maxHp = 200;
            case 4 -> this.maxHp = 350;
            default -> this.maxHp = 500;
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 60.0f) * 0.1f));
        this.hp = this.maxHp;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {

        if (hatchTimer > 0) {
            hatchTimer--;
        } else if (!hatched) {
            hatch(panel);
        }

        // Nếu hết máu cũng nở
        if (hp <= 0 && !hatched) {
            hatch(panel);
        }
    }

    private void hatch(GamePanel panel) {
        if (hatched)
            return;
        hatched = true;
        // Tự kết liễu để kích hoạt quy trình xóa quái chuẩn của EntityManager
        this.takeDamage(this.maxHp + 999, null, gameproject.GamePanel.getTickTime());
    }

    @Override
    public List<Enemy> summon() {
        // Chỉ triệu hồi 1 lần khi nở
        if (hatched && !summoned) {
            summoned = true;
            List<Enemy> spawns = new ArrayList<>();
            // Nở ra 1-2 quái normal
            int count = 1 + (tier / 3);
            for (int i = 0; i < count; i++) {
                float offsetX = (float) (Math.random() * 40 - 20);
                float offsetY = (float) (Math.random() * 40 - 20);
                spawns.add(new NormalEnemy(x + offsetX, y + offsetY, tier, surviveTimeSeconds, mapId));
            }
            return spawns;
        }
        return null;
    }

    @Override
    public void draw(Graphics g) {
        // Trứng rung rinh khi sắp nở
        float shakeX = 0;
        if (hatchTimer < 60) {
            shakeX = (float) (Math.sin(System.currentTimeMillis() / 20.0) * 3);
        }

        java.awt.image.BufferedImage img = gameproject.ImageManager.get("egg");

        if (img != null) {
            int drawX = (int) Math.round(x + shakeX) - 5;
            int drawY = (int) Math.round(y) - 5;
            int drawW = size + 10;
            int drawH = size + 10;
            g.drawImage(img, drawX, drawY, drawW, drawH, null);
        } else {
            g.setColor(new Color(200, 200, 150));
            g.fillOval((int) (x + shakeX), (int) y, size, size);
        }

        // Vẽ HP bar cho trứng
        if (hp > 0) {
            g.setColor(Color.RED);
            g.fillRect((int) x, (int) y + size, size, 4);
            g.setColor(Color.GREEN);
            int hpWidth = (int) ((float) hp / maxHp * size);
            g.fillRect((int) x, (int) y + size, hpWidth, 4);
        }
    }

    @Override
    public String getName() {
        return "MONSTER EGG";
    }
}
