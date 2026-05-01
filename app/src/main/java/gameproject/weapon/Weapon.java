package gameproject.weapon;

//import gameproject.*; // Import để gọi SoundManager
import java.util.ArrayList;

public abstract class Weapon {
    public String name;
    public float damageMultiplier;
    public long cooldown;
    public long baseCooldown;     // Chỉ số cooldown gốc – dùng để tính tỉ lệ khi tiến hóa
    public long lastShootTime = 0;
    public boolean isAutomatic;
    public float range;
    public float baseRange;       // Chỉ số range gốc – dùng để tính tỉ lệ khi tiến hóa

    public Weapon(String name, float damageMultiplier, long cooldown, boolean isAutomatic, float range) {
        this.name = name;
        this.damageMultiplier = damageMultiplier;
        this.cooldown = cooldown;
        this.baseCooldown = cooldown;
        this.isAutomatic = isAutomatic;
        this.range = range;
        this.baseRange = range;
    }

    public long getActualCooldown(float fireRateBonus) {
        float totalBonus = gameproject.meta.PlayerData.statCooldownLevel * 0.02f + fireRateBonus;
        return (long) (cooldown * (1.0f - Math.min(0.9f, totalBonus)));
    }

    public boolean canShoot(long currentTime, float fireRateBonus) {
        return currentTime - lastShootTime >= getActualCooldown(fireRateBonus);
    }

    public abstract void shoot(float startX, float startY, float targetX, float targetY,
            float bulletSpeedMulti, int playerDamage, int bounces,
            ArrayList<Projectile> projectiles, long currentTime);
}