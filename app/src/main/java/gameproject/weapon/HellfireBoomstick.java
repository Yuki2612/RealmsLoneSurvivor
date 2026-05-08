package gameproject.weapon;

import gameproject.SoundManager;
import java.util.ArrayList;

public class HellfireBoomstick extends Weapon {
    public HellfireBoomstick() {
        super("Hellfire", 0.8f, 600, false, 300f);
    }

    @Override
    public int getProjectilesPerShot() {
        return 5;
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            int playerDamage, int bounces,
            ArrayList<Projectile> projectiles, long currentTime) {

        lastShootTime = currentTime;
        SoundManager.play("shoot");

        int bullets = 5;
        float angleSpread = 30.0f;

        float dx = targetX - startX;
        float dy = targetY - startY;
        float baseAngle = (float) Math.toDegrees(Math.atan2(dy, dx));

        for (int i = 0; i < bullets; i++) {
            float angle = baseAngle - (angleSpread / 2) + (i * (angleSpread / (bullets - 1)));
            float radians = (float) Math.toRadians(angle);
            float tx = startX + (float) Math.cos(radians) * 100;
            float ty = startY + (float) Math.sin(radians) * 100;

            Projectile p = new Projectile(startX, startY, tx, ty, 1.0f, range);
            p.damage = (int) (playerDamage * damageMultiplier);
            p.isPlayerExplosive = true;
            p.isHellfire = true;
            projectiles.add(p);
        }
    }
}
