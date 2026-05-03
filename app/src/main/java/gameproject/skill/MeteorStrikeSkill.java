package gameproject.skill;

import java.awt.Graphics;
import gameproject.Player;
import gameproject.VFXManager;
import gameproject.entity.EntityManager;
import gameproject.entity.Enemy;

import java.util.ArrayList;

public class MeteorStrikeSkill implements PassiveSkill {
    private long lastStrikeTime = 0;

    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
        int level = player.getBreakthroughLevel(Upgrade.METEOR_STRIKE);
        if (level > 0) {
            long cooldown = Math.max(500, 5000 - (level * 300));
            if (currentTime - lastStrikeTime > cooldown) {
                lastStrikeTime = currentTime;

                float targetX = player.getX() + (float) (Math.random() * 400 - 200);
                float targetY = player.getY() + (float) (Math.random() * 400 - 200);

                synchronized (enemies) {
                    if (!enemies.isEmpty()) {
                        Enemy e = enemies.get((int) (Math.random() * enemies.size()));
                        targetX = e.getX();
                        targetY = e.getY();
                    }
                }

                float soulMulti = 1.0f + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.METEOR_STRIKE, 0) * 0.05f);
                int radius = (int)((50 + level * 10) * soulMulti);
                int damage = (int)((10 + level * 5) * soulMulti);

                vfxManager.addExplosion(targetX, targetY, radius, currentTime);
                gameproject.SoundManager.play("explosion");

                synchronized (enemies) {
                    for (Enemy e : enemies) {
                        float dist = (float) Math.sqrt(Math.pow(e.getX() - targetX, 2) + Math.pow(e.getY() - targetY, 2));
                        if (dist <= radius) {
                            e.takeDamage(damage, vfxManager, currentTime);
                            e.applyBurn(3000, vfxManager); // Meteor applies burn
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
    }
}
