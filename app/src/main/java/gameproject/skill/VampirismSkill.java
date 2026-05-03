package gameproject.skill;

import java.awt.Graphics;
import java.util.ArrayList;
import gameproject.Player;
import gameproject.VFXManager;
import gameproject.entity.Enemy;

public class VampirismSkill implements PassiveSkill {
    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
    }

    @Override
    public void draw(Graphics g, Player player) {
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
        int level = player.getUpgradeLevel(Upgrade.VAMPIRISM);
        if (level > 0 && Math.random() < (level * 0.005f)) {
            player.addHeart();
        }
    }
}