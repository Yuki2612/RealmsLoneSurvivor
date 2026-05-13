package gameproject.skill;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

import gameproject.*;
import gameproject.entity.Enemy;

public class TrailOfFireSkill implements PassiveSkill {
    private float lastX, lastY;

    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
        int fireLevel = player.getBreakthroughLevel(Upgrade.TRAIL_OF_FIRE);
        if (fireLevel <= 0)
            return;

        // 1. Tần suất tạo lửa (Giãn cách theo khoảng cách di chuyển)
        float dx = player.getX() - lastX;
        float dy = player.getY() - lastY;
        float distSq = dx * dx + dy * dy;

        // Cấp càng cao, giãn cách càng ngắn (tần suất cao hơn)
        // Lv1: 45px, Lv5: 25px
        float minSpawnDist = Math.max(25, 50 - (fireLevel * 5));

        if (player.isMoving() && distSq > minSpawnDist * minSpawnDist) {
            String tileType = GamePanel.instance.mapManager.getTileTypeAtWorld(player.getX() + 12, player.getY() + 12);
            if (!tileType.equals("water")) {
                vfxManager.addFireTrail(player.getX() + 5, player.getY() + 5, currentTime);
            }
            lastX = player.getX();
            lastY = player.getY();
        }

        // 2. Gây sát thương và hiệu ứng Burn
        float soulMulti = 1.0f
                + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.TRAIL_OF_FIRE, 0) * 0.05f);
        // Sát thương tăng theo Level: (Base: 40% - 100% playerDamage)
        int directDamage = (int) (gameproject.GamePanel.instance.upgradeManager.playerDamage * (0.3f + fireLevel * 0.1f)
                * soulMulti);

        for (VFXManager.FireZone fz : vfxManager.fireZones) {
            if (!fz.isExplosion) {
                // Sát thương FireTrail (vùng sát thương rộng hơn hình vẽ 20x20)
                Rectangle fzHitbox = new Rectangle((int) fz.x - 10, (int) fz.y - 10, 40, 40);
                synchronized (enemies) {
                    for (Enemy e : enemies) {
                        if (e.getBounds().intersects(fzHitbox)) {
                            e.applyBurn((int) (2000 * soulMulti), vfxManager);

                            // Sát thương trực tiếp (Tick 500ms giống Poison)
                            if (currentTime - e.lastFireZoneDamageTick >= 500) {
                                e.takeDamage(directDamage, vfxManager, currentTime);
                                e.lastFireZoneDamageTick = currentTime;
                            }
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