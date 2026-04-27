package gameproject;

import java.util.ArrayList;

public class Pistol extends Weapon {
    public Pistol() {
        super(10, 400); // 10 sát thương gốc, 400ms delay
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            float bulletSpeedMulti, int extraDamage, int bounces,
            ArrayList<Projectile> projectiles) {

        Projectile p = new Projectile(startX, startY, targetX, targetY, bulletSpeedMulti);
        p.damage = this.baseDamage + extraDamage;
        p.bouncesLeft = bounces;
        projectiles.add(p);

        this.lastShootTime = System.currentTimeMillis();
        SoundManager.play("shoot");
    }
}