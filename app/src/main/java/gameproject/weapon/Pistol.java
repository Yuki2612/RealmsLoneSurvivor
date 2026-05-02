package gameproject.weapon;

import gameproject.*;
import java.util.ArrayList;

public class Pistol extends Weapon {
    public Pistol() {
        super("Pistol", 1.0f, 400, false, 300f);
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            int playerDamage, int bounces, ArrayList<Projectile> projectiles, long currentTime) {
        Projectile p = new Projectile(startX, startY, targetX, targetY, 1.0f, range);
        p.damage = Math.max(1, (int) (playerDamage * this.damageMultiplier));
        p.bouncesLeft = bounces;
        projectiles.add(p);
        this.lastShootTime = currentTime;
        SoundManager.play("shoot");
    }
}