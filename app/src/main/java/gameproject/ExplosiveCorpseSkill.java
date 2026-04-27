package gameproject;

import java.awt.Graphics;
import java.util.ArrayList;

public class ExplosiveCorpseSkill implements PassiveSkill {
    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
    }

    @Override
    public void draw(Graphics g, Player player) {
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
        int corpseLevel = player.getBreakthroughLevel(Upgrade.EXPLOSIVE_CORPSE);
        if (corpseLevel <= 0)
            return;

        vfxManager.triggerScreenShake(5);
        SoundManager.play("explosion");

        float explosionRadius = 50 + (corpseLevel * 15);
        int explosionDamage = (int) (deadEnemy.getMaxHp() * 0.1) + (corpseLevel * 3);

        for (Enemy other : enemies) {
            if (other == deadEnemy)
                continue;
            float dist = (float) Math
                    .sqrt(Math.pow(deadEnemy.getX() - other.getX(), 2) + Math.pow(deadEnemy.getY() - other.getY(), 2));
            if (dist <= explosionRadius) {
                other.takeDamage(explosionDamage);
            }
        }
        vfxManager.addExplosion(deadEnemy.getX(), deadEnemy.getY(), explosionRadius, currentTime);
    }
}