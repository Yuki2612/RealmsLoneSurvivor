package gameproject.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import gameproject.GamePanel;

public class SpawnerEnemy extends Enemy {
    private Random rand = new Random();
    private int tier;
    private int spawnCooldown;
    private int currentCooldown;
    private List<Enemy> pendingEggs = new ArrayList<>();
    private int mapId;
    private int surviveTimeSeconds;

    public SpawnerEnemy(float startX, float startY, int tier, int surviveTimeSeconds, int mapId) {
        super(startX, startY, 40, 0, 0, Color.WHITE);
        this.isBoss = false;
        this.tier = tier;
        this.mapId = mapId;
        this.surviveTimeSeconds = surviveTimeSeconds;

        // Spawner trâu bò hơn NormalEnemy
        switch (tier) {
            case 1 -> {
                this.maxHp = 60;
                this.speed = 0.9f;
                this.spawnCooldown = 1500; // Khoảng 25 giây
            }
            case 2 -> {
                this.maxHp = 100;
                this.speed = 1.0f;
                this.spawnCooldown = 1300;
            }
            case 3 -> {
                this.maxHp = 150;
                this.speed = 1.1f;
                this.spawnCooldown = 1200;
            }
            case 4 -> {
                this.maxHp = 210;
                this.speed = 1.2f;
                this.spawnCooldown = 1100;
            }
            default -> {
                this.maxHp = 280;
                this.speed = 1.3f;
                this.spawnCooldown = 1000;
                this.tier = 5;
            }
        }
        // Scale theo thời gian
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 30.0f) * 0.15f));
        this.hp = this.maxHp;
        this.currentCooldown = rand.nextInt(spawnCooldown / 2) + spawnCooldown / 2;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {

        float dx = playerX - x;
        float dy = playerY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Di chuyển về phía player nhưng dừng lại ở khoảng cách 200px để đẻ trứng
        if (dist > 200) {
            EnemyController.moveEnemy(this, panel, speedMultiplier);
        }

        if (currentCooldown > 0) {
            currentCooldown--;
        } else {
            // Đẻ trứng
            spawnEgg();
            currentCooldown = spawnCooldown;
        }
    }

    private void spawnEgg() {
        // Tạo trứng tại vị trí hiện tại của Spawner
        EggEntity egg = new EggEntity(x, y, tier, surviveTimeSeconds, mapId);
        pendingEggs.add(egg);
    }

    @Override
    public List<Enemy> summon() {
        if (pendingEggs.isEmpty())
            return null;
        List<Enemy> result = new ArrayList<>(pendingEggs);
        pendingEggs.clear();
        return result;
    }

    @Override
    public void draw(Graphics g) {
        drawSprite(g, "enemy_spawner");
    }
}
