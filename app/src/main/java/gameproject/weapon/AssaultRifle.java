package gameproject.weapon;

import gameproject.*;
import java.util.ArrayList;

public class AssaultRifle extends Weapon {
    public AssaultRifle() {
        super("Assault Rifle", 1.1f, 300, false, 450f);
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            int playerDamage, int bounces, ArrayList<Projectile> projectiles, long currentTime) {
        Projectile p = new Projectile(startX, startY, targetX, targetY, 1.5f, range);
        p.damage = Math.max(1, (int) (playerDamage * this.damageMultiplier));
        p.bouncesLeft = bounces;
        projectiles.add(p);
        this.lastShootTime = currentTime;
        SoundManager.play("shoot");
    }
}