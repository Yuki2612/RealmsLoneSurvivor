package gameproject.skill;

import java.awt.Color;
import java.awt.Graphics;
import gameproject.Player;
import gameproject.VFXManager;
import gameproject.entity.EntityManager;
import gameproject.entity.Enemy;

import java.util.ArrayList;

public class PulseWaveSkill implements PassiveSkill {
    private long lastPulseTime = 0;
    private long pulseRenderUntil = 0;
    private float lastPulseX, lastPulseY;
    private int pulseRadius;

    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
        int level = player.getBreakthroughLevel(Upgrade.PULSE_WAVE);
        if (level > 0) {
            long cooldown = Math.max(1000, 4000 - (level * 300));
            if (currentTime - lastPulseTime > cooldown) {
                lastPulseTime = currentTime;

                pulseRadius = 100 + level * 20;
                int damage = 20 + level * 10;
                float knockback = 40f + level * 5f;

                lastPulseX = player.getX();
                lastPulseY = player.getY();
                pulseRenderUntil = currentTime + 300;

                for (Enemy e : enemies) {
                    float dist = (float) Math
                            .sqrt(Math.pow(e.getX() - player.getX(), 2) + Math.pow(e.getY() - player.getY(), 2));
                    if (dist <= pulseRadius) {
                        e.takeDamageBase(damage, vfxManager, currentTime, Color.CYAN);
                        e.applyKnockback(player.getX(), player.getY(), knockback);
                    }
                }
            }
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        if (System.currentTimeMillis() < pulseRenderUntil) {
            g.setColor(new Color(0, 255, 255, 60));
            g.fillOval((int) lastPulseX - pulseRadius, (int) lastPulseY - pulseRadius, pulseRadius * 2,
                    pulseRadius * 2);
            g.setColor(Color.WHITE);
            g.drawOval((int) lastPulseX - pulseRadius, (int) lastPulseY - pulseRadius, pulseRadius * 2,
                    pulseRadius * 2);
        }
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
    }
}
