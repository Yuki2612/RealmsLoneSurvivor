package gameproject;

import java.awt.Color;
import java.util.ArrayList;

public class TankBoss extends Enemy {
    public TankBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 80, (int) ((300 + (surviveTimeSeconds * 3)) * 1.5f), 0.5f, Color.WHITE);
        this.isBoss = true;
    }

    @Override
    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
        // Ghi đè rỗng: Hoàn toàn miễn nhiễm đẩy lùi
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH) {
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