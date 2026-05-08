package gameproject.weapon;

import gameproject.SoundManager;
import java.util.ArrayList;

public class LightningGun extends Weapon {
    public LightningGun() {
        super("Lightning Gun", 1.2f, 250, true, 450f);
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            int playerDamage, int bounces,
            ArrayList<Projectile> projectiles, long currentTime) {

        lastShootTime = currentTime;
        SoundManager.play("shoot");

        Projectile p = new Projectile(startX, startY, targetX, targetY, 1.0f, range);
        p.damage = (int) (playerDamage * damageMultiplier);
        p.bouncesLeft = bounces + 1; // Nảy thêm 1 mục tiêu
        p.isShocking = true; // Sát thương hệ sét
        projectiles.add(p);
    }
}
