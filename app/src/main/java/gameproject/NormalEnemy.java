package gameproject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

public class NormalEnemy extends Enemy {
    private float wanderX, wanderY;
    private int wanderTimer = 0;
    private Random rand = new Random();

    public NormalEnemy(float startX, float startY, int tier, int surviveTimeSeconds) {
        super(startX, startY, 30, 0, 0, Color.WHITE);
        this.isBoss = false;

        switch (tier) {
            case 1 -> {
                this.maxHp = 20;
                this.color = Color.YELLOW;
                this.speed = 1.2f;
            }
            case 2 -> {
                this.maxHp = 30;
                this.color = Color.ORANGE;
                this.speed = 1.5f;
            }
            case 3 -> {
                this.maxHp = 50;
                this.color = Color.BLUE;
                this.speed = 1.8f;
            }
            case 4 -> {
                this.maxHp = 70;
                this.color = Color.GREEN;
                this.speed = 2.1f;
            }
            default -> {
                this.maxHp = 100;
                this.color = new Color(128, 0, 128);
                this.speed = 2.4f;
            }
        }
        this.maxHp = (int) (this.maxHp * (1.0f + (surviveTimeSeconds / 60.0f) * 0.1f));
        this.hp = this.maxHp;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH) {
        float dx = playerX - x;
        float dy = playerY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float currentSpeed = speed * speedMultiplier;
        float moveX = 0, moveY = 0;

        if (distance < 300 && distance > 0) {
            moveX = (dx / distance) * currentSpeed;
            moveY = (dy / distance) * currentSpeed;
        } else {
            if (wanderTimer <= 0) {
                wanderX = rand.nextFloat() * 2 - 1;
                wanderY = rand.nextFloat() * 2 - 1;
                wanderTimer = 60 + rand.nextInt(60);
            }
            moveX = wanderX * (currentSpeed * 0.5f);
            moveY = wanderY * (currentSpeed * 0.5f);
            wanderTimer--;
        }

        // Tách đàn
        for (Enemy other : allEnemies) {
            if (other == this || other.isBoss)
                continue;
            float odx = this.x - other.x;
            float ody = this.y - other.y;
            float oDist = (float) Math.sqrt(odx * odx + ody * ody);
            if (oDist > 0 && oDist < 30) {
                moveX += (odx / oDist) * 0.8f;
                moveY += (ody / oDist) * 0.8f;
            }
        }
        applyPhysicsAndBounds(moveX, moveY, screenW, screenH);
    }
}