package gameproject.skill;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import gameproject.Player;
import gameproject.VFXManager;
import gameproject.entity.Enemy;

public class PoisonCloudSkill implements PassiveSkill {

    // Tự quản lý cấu trúc dữ liệu vùng độc ngay trong Skill, không làm phiền
    // VFXManager
    private class Cloud {
        float x, y;
        long expireTime;
    }

    private ArrayList<Cloud> clouds = new ArrayList<>();

    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
        int level = player.getUpgradeLevel(Upgrade.POISON_CLOUD);
        if (level <= 0)
            return;

        synchronized (clouds) {
            Iterator<Cloud> it = clouds.iterator();
            while (it.hasNext()) {
                Cloud c = it.next();
                if (currentTime > c.expireTime) {
                    it.remove();
                    continue;
                }

                float soulMulti = 1.0f + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.POISON_CLOUD, 0) * 0.05f);
                // Lv1: 60px  Lv5: 140px
                int radius = (int) ((40 + (level * 20)) * soulMulti);
                Rectangle cloudBox = new Rectangle((int) c.x - radius, (int) c.y - radius, radius * 2, radius * 2);
                synchronized (enemies) {
                    for (Enemy e : enemies) {
                        if (e.getBounds().intersects(cloudBox)) {
                            e.applyPoison(3000);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        int level = player.getUpgradeLevel(Upgrade.POISON_CLOUD);
        if (level <= 0)
            return;

        g.setColor(new Color(0, 255, 0, 80)); // Màu xanh lá trong suốt
        synchronized (clouds) {
            for (Cloud c : clouds) {
                float soulMulti = 1.0f + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.POISON_CLOUD, 0) * 0.05f);
                // Lv1: 60px  Lv5: 140px
                int radius = (int) ((40 + (level * 20)) * soulMulti);
                g.fillOval((int) c.x - radius, (int) c.y - radius, radius * 2, radius * 2);
            }
        }
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
        int level = player.getUpgradeLevel(Upgrade.POISON_CLOUD);
        float soulMulti = 1.0f + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.POISON_CLOUD, 0) * 0.05f);
        // Tỷ lệ xuất hiện mây độc: Cấp 1 = 32%, Cấp 5 = 80%
        if (level > 0 && Math.random() < ((0.2f + (level * 0.12f)) * soulMulti)) {
            Cloud c = new Cloud();
            c.x = deadEnemy.getX() + deadEnemy.getBounds().width / 2;
            c.y = deadEnemy.getY() + deadEnemy.getBounds().height / 2;
            c.expireTime = currentTime + 4000; // Tồn tại 4 giây
            synchronized (clouds) {
                clouds.add(c);
            }
        }
    }
}