package gameproject;

import java.awt.Color;
import java.util.ArrayList;

public class ChargerBoss extends Enemy {
    private int actionTimer = 0;
    private boolean isCharging = false;
    private int chargeFrames = 0;
    private float baseSpeed = 1.5f;

    public ChargerBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 60, 300 + (surviveTimeSeconds * 3), 1.5f, Color.RED);
        this.isBoss = true;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH) {
        actionTimer--;
        if (actionTimer <= 0) {
            actionTimer = 180;
            isCharging = true;
            chargeFrames = 30;
        }
        if (isCharging) {
            speed = 5.0f;
            chargeFrames--;
            if (chargeFrames <= 0) {
                isCharging = false;
                speed = baseSpeed;
            }
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