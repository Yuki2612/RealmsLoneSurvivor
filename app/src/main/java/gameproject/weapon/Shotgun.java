package gameproject.weapon;

import gameproject.*;
import java.util.ArrayList;

public class Shotgun extends Weapon {
    public Shotgun() {
        super("Shotgun", 0.95f, 650, false, 250f);
    }

    @Override
    public int getProjectilesPerShot() {
        return 3;
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            int playerDamage, int bounces, ArrayList<Projectile> projectiles, long currentTime) {
        float dx = targetX - startX;
        float dy = targetY - startY;
        double baseAngle = Math.atan2(dy, dx);
        double[] angles = { baseAngle - 0.26, baseAngle, baseAngle + 0.26 };

        int finalDamage = Math.max(1, (int) (playerDamage * this.damageMultiplier));

        for (double angle : angles) {
            float tX = startX + (float) Math.cos(angle) * 100;
            float tY = startY + (float) Math.sin(angle) * 100;
            Projectile p = new Projectile(startX, startY, tX, tY, 1.0f, range);
            p.damage = finalDamage;
            p.bouncesLeft = bounces;
            projectiles.add(p);
        }
        this.lastShootTime = currentTime;
        SoundManager.play("shoot");
    }
}