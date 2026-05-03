package gameproject.skill;

import java.awt.Color;
import java.awt.Graphics;
import gameproject.Player;
import gameproject.VFXManager;
import gameproject.entity.EntityManager;
import gameproject.entity.Enemy;

import java.util.ArrayList;

public class EnergyShieldSkill implements PassiveSkill {
    private long lastActivateTime = 0;

    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
        int level = player.getBreakthroughLevel(Upgrade.ENERGY_SHIELD);
        if (level > 0 && !player.hasShield) {
            float soulMulti = 1.0f
                    + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.ENERGY_SHIELD, 0) * 0.05f);
            long cooldown = (long) ((15000 - (level * 1000)) / soulMulti);
            if (currentTime - lastActivateTime > cooldown) {
                player.hasShield = true;
                lastActivateTime = currentTime;
                // Hiệu ứng hạt khi khiên hồi phục
                vfxManager.spawnDeathParticles(player.getX() + 12, player.getY() + 12, currentTime, Color.CYAN);
            }
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        int level = player.getBreakthroughLevel(Upgrade.ENERGY_SHIELD);
        if (level > 0 && (player.hasShield || player.isInvulnerable())) {
            g.setColor(new Color(0, 255, 255, 60));
            g.fillOval((int) player.getX() - 15, (int) player.getY() - 15, 55, 55);
            g.setColor(new Color(0, 255, 255, 150));
            g.drawOval((int) player.getX() - 15, (int) player.getY() - 15, 55, 55);
        }
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
    }
}
