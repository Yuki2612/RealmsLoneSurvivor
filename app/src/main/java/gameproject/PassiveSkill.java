package gameproject;

import java.awt.Graphics;
import java.util.ArrayList;

public interface PassiveSkill {
    // Gọi mỗi khung hình (dành cho Lửa, Quả cầu xoay)
    void update(Player player, ArrayList<Enemy> enemies, VFXManager vfxManager, long currentTime);

    // Gọi khi vẽ đồ họa (dành cho Vùng lạnh, Quả cầu xoay)
    void draw(Graphics g, Player player);

    // Gọi khi có một con quái bay màu (dành cho Bom Máu)
    void onEnemyDeath(Enemy deadEnemy, Player player, ArrayList<Enemy> enemies, VFXManager vfxManager,
            long currentTime);
}