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
        float evoBonus = gameproject.meta.PlayerData.evoVampirism * 0.002f; // 0.2% per level
        float totalChance = (level * 0.005f) + evoBonus;
        
        if (totalChance > 0 && Math.random() < totalChance) {
            player.addHeart();
            vfxManager.addDamageText(player.getX(), player.getY() - 30, 0, currentTime, java.awt.Color.GREEN);
        }
    }
}