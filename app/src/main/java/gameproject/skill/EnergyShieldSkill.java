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
        if (level > 0) {
            float soulMulti = 1.0f
                    + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.ENERGY_SHIELD, 0) * 0.05f);

            // Base cooldown 30s. Mỗi level giảm 2s.
            long cooldown = (long) ((30000 - (level * 2000)) / soulMulti);

            if (player.hasShield) {
                // Kiểm tra thời gian tồn tại của khiên (3 giây = 3000ms)
                if (currentTime - lastActivateTime > 3000) {
                    player.hasShield = false;
                }
            } else {
                // Nếu không có khiên, kiểm tra thời gian hồi chiêu
                if (currentTime - lastActivateTime > cooldown) {
                    player.hasShield = true;
                    lastActivateTime = currentTime;
                    // Hiệu ứng hạt khi khiên bật lên
                    vfxManager.spawnDeathParticles(player.getX() + 12, player.getY() + 12, currentTime, Color.CYAN);
                    gameproject.SoundManager.play("powerup"); // Thêm một chút âm thanh báo hiệu khiên bật
                }
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
