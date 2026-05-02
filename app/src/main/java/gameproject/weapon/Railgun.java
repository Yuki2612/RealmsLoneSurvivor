package gameproject.weapon;

import gameproject.SoundManager;
import java.util.ArrayList;

public class Railgun extends Weapon {
    public Railgun() {
        super("Railgun", 4f, 1100, false, 750f);
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            int playerDamage, int bounces,
            ArrayList<Projectile> projectiles, long currentTime) {

        lastShootTime = currentTime;
        SoundManager.play("laser");

        Projectile p = new Projectile(startX, startY, targetX, targetY, 3.0f, range);
        p.damage = (int) (playerDamage * damageMultiplier);
        p.isRailgun = true;
        p.bouncesLeft = 1;
        projectiles.add(p);
    }
}
