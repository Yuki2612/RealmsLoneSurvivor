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
            long cooldown = 15000 - (level * 1000); // Level 5: 5s cooldown
            if (currentTime - lastActivateTime > cooldown) {
                player.addInvulnerability(3000); // 3 giây bất tử
                lastActivateTime = currentTime;
                gameproject.SoundManager.play("shield");
            }
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        int level = player.getBreakthroughLevel(Upgrade.ENERGY_SHIELD);
        if (level > 0 && player.isInvulnerable()) {
            g.setColor(new Color(0, 255, 255, 80));
            g.fillOval((int) player.getX() - 20, (int) player.getY() - 20, 65, 65);
            g.setColor(Color.CYAN);
            g.drawOval((int) player.getX() - 20, (int) player.getY() - 20, 65, 65);
        }
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
    }
}
