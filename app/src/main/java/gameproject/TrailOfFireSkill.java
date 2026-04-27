package gameproject;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

public class TrailOfFireSkill implements PassiveSkill {
    @Override
    public void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime) {
        int fireLevel = player.getBreakthroughLevel(Upgrade.TRAIL_OF_FIRE);
        if (fireLevel <= 0)
            return;

        // Sinh lửa khi lướt
        if (player.isDashing() && currentTime % 30 < 15) {
            vfxManager.addFireTrail(player.getX() + 10, player.getY() + 10, currentTime);
        }

        // Tính sát thương (Tick rate)
        for (FireZone fz : vfxManager.fireZones) {
            if (!fz.isExplosion) {
                Rectangle fzHitbox = new Rectangle((int) fz.x, (int) fz.y, 20, 20);
                for (Enemy e : enemies) {
                    if (e.getBounds().intersects(fzHitbox) && currentTime % 5 == 0) {
                        e.takeDamage(fireLevel * 2);
                    }
                }
            }
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        // Việc vẽ lửa đã được ủy quyền hoàn toàn cho VFXManager
    }

    @Override
    public void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime) {
    }
}