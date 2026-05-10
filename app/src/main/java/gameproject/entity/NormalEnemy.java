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

    public NormalEnemy(float startX, float startY, int tier, int surviveTimeSeconds) {
        super(startX, startY, 30, 0, 0, Color.WHITE);
        this.isBoss = false;
        this.tier = tier;

        switch (tier) {
            case 1 -> {
                this.maxHp = 20;
                this.speed = 0.9f;
            }
            case 2 -> {
                this.maxHp = 30;
                this.speed = 1.1f;
            }
            case 3 -> {
                this.maxHp = 50;
                this.speed = 1.4f;
            }
            case 4 -> {
                this.maxHp = 70;
                this.speed = 1.7f;
            }
            default -> {
                this.maxHp = 100;
                this.speed = 2.0f;
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
        drawSprite(g, "enemy" + tier);
    }
}