package gameproject;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class FrostAuraSkill implements PassiveSkill {
    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
    }

    @Override
    public void draw(Graphics g, Player player) {
        int frostLevel = player.getBreakthroughLevel(Upgrade.FROST_AURA);
        if (frostLevel <= 0)
            return;

        float frostRadius = 120 + (frostLevel * 30);
        g.setColor(new Color(0, 255, 255, 40));
        g.fillOval((int) (player.getX() + 20 - frostRadius), (int) (player.getY() + 20 - frostRadius),
                (int) (frostRadius * 2), (int) (frostRadius * 2));
        g.setColor(new Color(0, 255, 255, 100));
        g.drawOval((int) (player.getX() + 20 - frostRadius), (int) (player.getY() + 20 - frostRadius),
                (int) (frostRadius * 2), (int) (frostRadius * 2));
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
    }

    // Hàm tiện ích để EntityManager tính tốc độ quái vật
    public static float getSlowMultiplier(Player player, Enemy enemy) {
        int frostLevel = player.getBreakthroughLevel(Upgrade.FROST_AURA);
        if (frostLevel <= 0)
            return 1.0f;

        float frostRadius = 120 + (frostLevel * 30);
        float dist = (float) Math
                .sqrt(Math.pow(player.getX() - enemy.getX(), 2) + Math.pow(player.getY() - enemy.getY(), 2));
        if (dist <= frostRadius) {
            return Math.max(0.2f, 1.0f - (frostLevel * 0.15f));
        }
        return 1.0f;
    }
}