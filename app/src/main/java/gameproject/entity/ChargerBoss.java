package gameproject.entity;

//import gameproject.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class ChargerBoss extends Enemy {
    private int actionTimer = 0;
    private boolean isCharging = false;
    private int chargeFrames = 0;
    private float baseSpeed = 1.5f;

    public ChargerBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 90, 500 + (surviveTimeSeconds * 4), 1.2f, Color.RED);
        this.isBoss = true;
    }

    private java.util.List<gameproject.weapon.Projectile> nextShots = new java.util.ArrayList<>();

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, gameproject.GamePanel panel) {
        actionTimer--;
        if (actionTimer <= 0) {
            actionTimer = 180;
            isCharging = true;
            chargeFrames = 30;
        }
        if (isCharging) {
            speed = 5.0f;
            chargeFrames--;

            // SÁT THƯƠNG VẬT CẢN KHI ĐANG LƯỚT
            // Kiểm tra một vùng nhỏ phía trước hướng di chuyển
            float checkDist = size / 2f + 10;
            float checkX = x + size / 2f + (velX > 0 ? checkDist : (velX < 0 ? -checkDist : 0));
            float checkY = y + size / 2f + (velY > 0 ? checkDist : (velY < 0 ? -checkDist : 0));
            
            // Phá hủy vật thể có thể phá hủy (Trees, Crates)
            panel.mapManager.damageObstacleAt((int)checkX, (int)checkY, 10); // Gây sát thương lớn để phá nhanh

            // Bắn đạn xung quanh khi đang lướt (mỗi 5 frame bắn 1 lần burst)
            if (chargeFrames % 5 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    float tx = x + (float) Math.cos(angle) * 100;
                    float ty = y + (float) Math.sin(angle) * 100;
                    gameproject.weapon.Projectile p = new gameproject.weapon.Projectile(x, y, tx, ty, 0.75f, 600f);
                    p.isEnemyBullet = true;
                    p.damage = 1;
                    nextShots.add(p);
                }
            }

            if (chargeFrames <= 0) {
                isCharging = false;
                speed = baseSpeed;
            }
        }

        // Điều khiển di chuyển bằng AI tập trung
        EnemyController.moveEnemy(this, panel, speedMultiplier);
    }

    @Override
    public java.util.List<gameproject.weapon.Projectile> shoot() {
        if (nextShots.isEmpty())
            return null;
        java.util.List<gameproject.weapon.Projectile> result = new java.util.ArrayList<>(nextShots);
        nextShots.clear();
        return result;
    }

    @Override
    public void draw(Graphics g) {
        drawSprite(g, "boss1");
    }
}