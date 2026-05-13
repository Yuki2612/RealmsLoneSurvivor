package gameproject.entity;

import gameproject.GamePanel;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

public class NormalEnemy extends Enemy {
    private float wanderX, wanderY;
    private int wanderTimer = 0;
    private Random rand = new Random();
    private int tier;
    private String spriteKey;

    public NormalEnemy(float startX, float startY, int tier, int surviveTimeSeconds, int mapId) {
        super(startX, startY, 30, 0, 0, Color.WHITE);
        this.isBoss = false;
        this.tier = tier;

        // Công thức chọn ảnh: tier + (mapId * 5)
        // Ví dụ: MapId=0 (Outskirts) -> 1-5, MapId=1 (Swamp) -> 6-10
        int spriteIndex = tier + (mapId * 5);
        this.spriteKey = "enemy" + spriteIndex;

        switch (tier) {
            case 1 -> {
                this.maxHp = 20;
                this.speed = 0.8f;
            }
            case 2 -> {
                this.maxHp = 30;
                this.speed = 1.0f;
            }
            case 3 -> {
                this.maxHp = 50;
                this.speed = 1.2f;
            }
            case 4 -> {
                this.maxHp = 70;
                this.speed = 1.4f;
            }
            default -> {
                this.maxHp = 100;
                this.speed = 1.6f;
                this.tier = 5;
            }
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 30.0f) * 0.15f));
        this.hp = this.maxHp;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {

        // Gọi bộ não AI tập trung để xử lý di chuyển và va chạm (Sliding Collision)
        EnemyController.moveEnemy(this, panel, speedMultiplier);

    }

    @Override
    public void draw(Graphics g) {
        drawSprite(g, spriteKey);
    }
}