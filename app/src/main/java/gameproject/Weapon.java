package gameproject;

import java.util.ArrayList;

public abstract class Weapon {
    protected int baseDamage;
    protected long cooldown;
    protected long lastShootTime = 0;

    public Weapon(int baseDamage, long cooldown) {
        this.baseDamage = baseDamage;
        this.cooldown = cooldown;
    }

    public boolean canShoot() {
        return System.currentTimeMillis() - lastShootTime >= cooldown;
    }

    // Hàm bắn trừu tượng: Vũ khí tự quyết định sinh ra bao nhiêu đạn và bay hướng
    // nào
    public abstract void shoot(float startX, float startY, float targetX, float targetY,
            float bulletSpeedMulti, int extraDamage, int bounces,
            ArrayList<Projectile> projectiles);
}