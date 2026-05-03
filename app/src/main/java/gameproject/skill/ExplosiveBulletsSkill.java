package gameproject.skill;

import java.awt.Graphics;
import java.util.ArrayList;
import java.awt.Color;
import gameproject.Player;
import gameproject.VFXManager;
import gameproject.entity.Enemy;
import gameproject.weapon.Projectile;

public class ExplosiveBulletsSkill implements PassiveSkill {

    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
    }

    @Override
    public void draw(Graphics g, Player player) {
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
    }

    private long lastExplosionTime = 0;
    private static final long COOLDOWN = 150; // 150ms delay để không quá OP trên SMG

    @Override
    public void onProjectileHit(Projectile p, Enemy e, Player player, VFXManager vfxManager, long currentTime) {
        if (currentTime - lastExplosionTime < COOLDOWN) return;

        int level = player.getBreakthroughLevel(Upgrade.EXPLOSIVE_BULLETS);
        if (level > 0) {
            float soulMulti = 1.0f
                    + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.EXPLOSIVE_BULLETS, 0) * 0.05f);
            
            // Sát thương bằng khoảng 10% damage của người chơi + level bonus
            int baseDamage = (int) (gameproject.GamePanel.instance.upgradeManager.playerDamage * (0.10f + (level * 0.05f)) * soulMulti);
            float radius = 40 + (level * 15);
            
            // Tạo vụ nổ
            vfxManager.addExplosion(p.getX(), p.getY(), radius, currentTime);
            gameproject.SoundManager.play("explosion");
            lastExplosionTime = currentTime;

            // Gây sát thương lan
            ArrayList<Enemy> enemies = gameproject.GamePanel.instance.entityManager.getEnemies();
            synchronized (enemies) {
                for (Enemy enemy : enemies) {
                    if (enemy == null || enemy.isDead()) continue;
                    
                    float dx = (enemy.getX() + enemy.getSize() / 2f) - p.getX();
                    float dy = (enemy.getY() + enemy.getSize() / 2f) - p.getY();
                    float distSq = dx * dx + dy * dy;
                    
                    if (distSq < radius * radius) {
                        enemy.takeDamage(baseDamage, vfxManager, currentTime);
                    }
                }
            }
        }
    }
}
