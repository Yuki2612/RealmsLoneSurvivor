package gameproject.entity;

import gameproject.GamePanel;
import gameproject.environment.MapManager;
import gameproject.environment.Obstacle;
import gameproject.environment.Hitbox;
import gameproject.environment.CircleHitbox;
import gameproject.environment.AABBHitbox;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * EnemyController: Hệ thống AI và vật lý trung tâm cho quái vật.
 * Đã được nâng cấp lên Hybrid Collision (SAT) giúp quái vật trượt qua vật thể
 * đa hình.
 */
public class EnemyController {

    private static final int TILE_SIZE = 64;

    public static void moveEnemy(Enemy enemy, GamePanel panel, float speedMultiplier) {
        if (enemy.isDying)
            return;

        float currentSpeed = enemy.speed * speedMultiplier;

        // 1. LẤY HƯỚNG TỪ FLOW FIELD
        Rectangle bounds = enemy.getPhysicsHitbox();
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        float dirX = panel.mapManager.getFlowDirX(centerX, centerY);
        float dirY = panel.mapManager.getFlowDirY(centerX, centerY);

        // Cận chiến: Bỏ qua FlowField nếu sát Player để lao vào
        float dxP = panel.player.getX() - enemy.x;
        float dyP = panel.player.getY() - enemy.y;
        float distSqP = dxP * dxP + dyP * dyP;

        if (distSqP < (TILE_SIZE * 0.8f) * (TILE_SIZE * 0.8f)) {
            float d = (float) Math.sqrt(distSqP);
            if (d > 0) {
                dirX = dxP / d;
                dirY = dyP / d;
            }
        }

        // 2. LỰC ĐẨY BẦY ĐÀN (Tách nhau ra)
        float[] sep = calculateSeparation(enemy, panel.entityManager.enemies);

        // 3. VẬT LÝ QUÁN TÍNH
        float targetVelX = (dirX * currentSpeed) + sep[0] * 0.6f;
        float targetVelY = (dirY * currentSpeed) + sep[1] * 0.6f;

        float acceleration = 0.3f;
        enemy.velX += (targetVelX - enemy.velX) * acceleration;
        enemy.velY += (targetVelY - enemy.velY) * acceleration;

        // CHIA NHỎ BƯỚC DI CHUYỂN (Sub-stepping: 2 bước) để tránh xuyên tường mỏng
        float totalMoveX = enemy.velX + enemy.kbX;
        float totalMoveY = enemy.velY + enemy.kbY;

        for (int step = 0; step < 2; step++) {
            float oldX = enemy.x;
            float oldY = enemy.y;

            enemy.x += totalMoveX / 2.0f;
            enemy.y += totalMoveY / 2.0f;

            // 4. PHÂN GIẢI VA CHẠM HYBRID (Trượt mượt mà)
            boolean collided = resolveHybridCollision(enemy, panel.mapManager);

            // Triệt tiêu knockback nếu chạm tường (Tạo cảm giác va chạm chắc chắn)
            if (collided && (Math.abs(enemy.kbX) > 0.1f || Math.abs(enemy.kbY) > 0.1f)) {
                enemy.kbX = 0;
                enemy.kbY = 0;
                // Khi chạm tường, dừng quán tính theo hướng va chạm (Tùy chọn, ở đây ta triệt tiêu toàn bộ KB)
            }
        }

        // 5. GIẢM DẦN LỰC KNOCKBACK (Dành cho trường hợp không chạm tường)
        enemy.kbX *= 0.85f;
        enemy.kbY *= 0.85f;
        if (Math.abs(enemy.kbX) < 0.1f)
            enemy.kbX = 0;
        if (Math.abs(enemy.kbY) < 0.1f)
            enemy.kbY = 0;
    }

    /**
     * Thuật toán phát hiện và đẩy trượt vật lý.
     * Biến quái vật thành một hình tròn ở sát chân để tương tác mượt với môi
     * trường.
     */
    private static boolean resolveHybridCollision(Enemy enemy, MapManager map) {
        boolean collided = false;
        // Thiết lập chân quái vật là một hình tròn vật lý
        float radius = enemy.size * 0.3f; // Bán kính hẹp giúp lách khe
        float cx = enemy.x + enemy.size / 2.0f; // Tâm X
        float cy = enemy.y + enemy.size - radius; // Tâm Y nằm dưới chân

        // Quét lấy danh sách vật cản xung quanh (Broad-phase)
        List<Obstacle> nearObs = map.getObstaclesInRadius(cx, cy, radius * 2 + TILE_SIZE);

        for (Obstacle obs : nearObs) {
            Hitbox hb = obs.getHitbox();
            if (hb == null)
                continue;

            if (hb instanceof CircleHitbox) {
                // XỬ LÝ: Circle vs Circle (Quái vật đụng Cây/Bụi)
                CircleHitbox cb = (CircleHitbox) hb;
                float dx = cx - cb.centerX;
                float dy = cy - cb.centerY;
                float distSq = dx * dx + dy * dy;
                float minDist = radius + cb.radius;

                if (distSq < minDist * minDist) {
                    collided = true;
                    float dist = (float) Math.sqrt(distSq);
                    if (dist == 0) {
                        dx = 1;
                        dist = 1;
                    } // Tránh chia 0
                    float overlap = minDist - dist;

                    // Toán học Vector: Ép trượt vòng qua đường cong
                    enemy.x += (dx / dist) * overlap;
                    enemy.y += (dy / dist) * overlap;

                    // Cập nhật lại tâm ngay lập tức cho vật cản tiếp theo
                    cx = enemy.x + enemy.size / 2.0f;
                    cy = enemy.y + enemy.size - radius;
                }
            } else if (hb instanceof AABBHitbox) {
                // XỬ LÝ: Circle vs Rectangle (Quái vật đụng Tường/Thùng gỗ)
                AABBHitbox ab = (AABBHitbox) hb;

                // Tìm điểm gần nhất trên khung chữ nhật so với tâm hình tròn
                float testX = Math.max(ab.x, Math.min(cx, ab.x + ab.width));
                float testY = Math.max(ab.y, Math.min(cy, ab.y + ab.height));

                float dx = cx - testX;
                float dy = cy - testY;
                float distSq = dx * dx + dy * dy;

                // Trường hợp 1: Tâm ở ngoài nhưng chạm/lồng vào nhau
                if (distSq > 0 && distSq < radius * radius) {
                    collided = true;
                    float dist = (float) Math.sqrt(distSq);
                    float overlap = radius - dist;
                    enemy.x += (dx / dist) * overlap;
                    enemy.y += (dy / dist) * overlap;
                }
                // Trường hợp 2: Tâm ĐÃ LỌT VÀO TRONG (Xảy ra khi knockback quá mạnh)
                // Phải đẩy ra cạnh gần nhất để không bị xuyên tường
                else if (distSq == 0) {
                    collided = true;
                    float dl = cx - ab.x;
                    float dr = (ab.x + ab.width) - cx;
                    float dt = cy - ab.y;
                    float db = (ab.y + ab.height) - cy;

                    float minDist = Math.min(Math.min(dl, dr), Math.min(dt, db));
                    if (minDist == dl) enemy.x -= (dl + radius);
                    else if (minDist == dr) enemy.x += (dr + radius);
                    else if (minDist == dt) enemy.y -= (dt + radius);
                    else enemy.y += (db + radius);
                }
                
                // Cập nhật lại tâm sau khi xử lý va chạm này
                cx = enemy.x + enemy.size / 2.0f;
                cy = enemy.y + enemy.size - radius;
            }
        }
        return collided;
    }

    private static float[] calculateSeparation(Enemy me, ArrayList<Enemy> enemies) {
        float sepX = 0, sepY = 0;
        for (Enemy other : enemies) {
            if (other == me || other.isDying)
                continue;
            float dx = me.x - other.x;
            float dy = me.y - other.y;
            float distSq = dx * dx + dy * dy;
            float safeDistance = (me.size + other.size) / 2.0f;
            float safeDistanceSq = safeDistance * safeDistance;

            if (distSq > 0 && distSq < safeDistanceSq) {
                float dist = (float) Math.sqrt(distSq);
                float force = (safeDistance - dist) / safeDistance;
                sepX += (dx / dist) * force * 2.0f;
                sepY += (dy / dist) * force * 2.0f;
            }
        }
        return new float[] { sepX, sepY };
    }
}