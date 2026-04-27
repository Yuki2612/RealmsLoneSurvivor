package gameproject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

public class TeleporterBoss extends Enemy {
    private int actionTimer = 0;
    private Random rand = new Random();

    public TeleporterBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 60, 300 + (surviveTimeSeconds * 3), 0.8f, Color.MAGENTA);
        this.isBoss = true;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH) {
        actionTimer--;
        if (actionTimer <= 0) {
            actionTimer = 240;
            float randomAngle = (float) (rand.nextDouble() * 2 * Math.PI);
            this.x = playerX + (float) Math.cos(randomAngle) * 250f;
            this.y = playerY + (float) Math.sin(randomAngle) * 250f;

            this.x = Math.max(0, Math.min(this.x, screenW - size));
            this.y = Math.max(0, Math.min(this.y, screenH - size));
            return;
        }

        float dx = playerX - x;
        float dy = playerY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float currentSpeed = speed * speedMultiplier;
        float moveX = 0, moveY = 0;
        if (distance > 0) {
            moveX = (dx / distance) * currentSpeed;
            moveY = (dy / distance) * currentSpeed;
        }
        applyPhysicsAndBounds(moveX, moveY, screenW, screenH);
    }
}