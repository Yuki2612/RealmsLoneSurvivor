package gameproject.skill;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gameproject.*;
import gameproject.entity.Enemy;

public class OrbitingOrbsSkill implements PassiveSkill {
    private float orbitAngle = 0;

    // Map lưu trữ thời gian lần cuối quái vật chịu sát thương từ Orb
    private Map<Enemy, Long> lastDamageTimes = new HashMap<>();

    // Tốc độ gây sát thương (Cooldown): 500ms (0.5 giây)
    private static final long DAMAGE_INTERVAL_MS = 500;

    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
        int orbLevel = player.getBreakthroughLevel(Upgrade.ORBITING_ORBS);
        if (orbLevel <= 0)
            return;

        orbitAngle += 0.08f;
        int numOrbs = Math.min(orbLevel, 5); // Tối đa 5 orb

        float soulMulti = 1.0f
                + (gameproject.meta.PlayerData.skillSoulLevels.getOrDefault(Upgrade.ORBITING_ORBS, 0) * 0.05f);
        int dmg = (int) ((16 + ((orbLevel - 1) * 8)) * soulMulti);

        for (int i = 0; i < numOrbs; i++) {
            float angle = orbitAngle + (float) (i * 2 * Math.PI / numOrbs);
            int ox = (int) (player.getX() + 20 + Math.cos(angle) * 90);
            int oy = (int) (player.getY() + 20 + Math.sin(angle) * 90);
            Rectangle orbHitbox = new Rectangle(ox - 10, oy - 10, 20, 20);

            synchronized (enemies) {
                for (Enemy e : enemies) {
                    if (e.getBounds().intersects(orbHitbox)) {
                        // Kiểm tra thời gian hồi chiêu sát thương
                        long lastHitTime = lastDamageTimes.getOrDefault(e, 0L);
                        if (currentTime - lastHitTime >= DAMAGE_INTERVAL_MS) {
                            e.takeDamage(dmg, vfxManager, currentTime);
                            e.applyKnockback(ox, oy, 10f);
                            // Cập nhật lại thời điểm chịu sát thương
                            lastDamageTimes.put(e, currentTime);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        int orbLevel = player.getBreakthroughLevel(Upgrade.ORBITING_ORBS);
        if (orbLevel <= 0)
            return;

        g.setColor(Color.MAGENTA);

        // Đã ĐỒNG BỘ logic số lượng với hàm update
        int numOrbs = Math.min(orbLevel, 5);
        for (int i = 0; i < numOrbs; i++) {
            float angle = orbitAngle + (float) (i * 2 * Math.PI / numOrbs);
            int ox = (int) (player.getX() + 20 + Math.cos(angle) * 90);
            int oy = (int) (player.getY() + 20 + Math.sin(angle) * 90);
            g.fillOval(ox - 10, oy - 10, 20, 20);
        }
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
        // Dọn rác: Xóa quái đã chết khỏi Map để giải phóng RAM
        lastDamageTimes.remove(deadEnemy);
    }
}