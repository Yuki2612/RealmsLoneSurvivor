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

        // 1. CƠ CHẾ THOÁT HIỂM KHẨN CẤP (EMERGENCY UNSTUCK)
        // Nếu quái lỡ bị kẹt sâu trong tường, đẩy chúng ra hướng thoáng nhất
        if (panel.mapManager.isSolid(centerX, centerY)) {
            // Thử đẩy ra 4 hướng chính
            int pushDist = 16;
            if (!panel.mapManager.isSolid(centerX - pushDist, centerY))
                enemy.x -= 8;
            else if (!panel.mapManager.isSolid(centerX + pushDist, centerY))
                enemy.x += 8;
            else if (!panel.mapManager.isSolid(centerX, centerY - pushDist))
                enemy.y -= 8;
            else if (!panel.mapManager.isSolid(centerX, centerY + pushDist))
                enemy.y += 8;
            else {
                // Nếu kẹt cứng mọi hướng, đẩy về phía người chơi (thường là vùng an toàn)
                float toPX = panel.player.getX() - enemy.x;
                float toPY = panel.player.getY() - enemy.y;
                float dist = (float) Math.sqrt(toPX * toPX + toPY * toPY);
                if (dist > 0) {
                    enemy.x += (toPX / dist) * 10;
                    enemy.y += (toPY / dist) * 10;
                }
            }
        }

        // 2. LẤY HƯỚNG TỪ FLOW FIELD
        float dirX = panel.mapManager.getFlowDirX(centerX, centerY);
        float dirY = panel.mapManager.getFlowDirY(centerX, centerY);

        // Tính khoảng cách để phục vụ logic giãn cách bầy đàn
        float dxP = panel.player.getX() - enemy.x;
        float dyP = panel.player.getY() - enemy.y;
        float distSqP = dxP * dxP + dyP * dyP;

        // 3. LỰC ĐẨY BẦY ĐÀN (Tách nhau ra)
        float[] sep = calculateSeparation(enemy, panel.entityManager.enemies);

        // TỐI ƯU: Giảm lực đẩy khi áp sát người chơi để quái có thể "chạm" vào player dễ hơn
        float sepWeight = 0.6f;
        if (distSqP < 10000) sepWeight = 0.05f; // Rất nhỏ khi sát player
        else if (distSqP < 40000) sepWeight = 0.2f; 

        // Cập nhật hướng quay mặt (Luôn hướng về phía người chơi giống Boss)
        float enemyCenterX = enemy.x + enemy.size / 2f;
        float playerCenterX = panel.player.getX() + gameproject.Player.SIZE / 2f;
        if (playerCenterX > enemyCenterX + 5) enemy.movingRight = true;
        else if (playerCenterX < enemyCenterX - 5) enemy.movingRight = false;

        // 3. VẬT LÝ QUÁN TÍNH
        float targetVelX = (dirX * currentSpeed) + sep[0] * sepWeight;
        float targetVelY = (dirY * currentSpeed) + sep[1] * sepWeight;

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
            }
        }

        // 5. GIẢM DẦN LỰC KNOCKBACK
        enemy.kbX *= 0.85f;
        enemy.kbY *= 0.85f;
        if (Math.abs(enemy.kbX) < 0.1f)
            enemy.kbX = 0;
        if (Math.abs(enemy.kbY) < 0.1f)
            enemy.kbY = 0;

        // 6. GIỚI HẠN BIÊN THẾ GIỚI (World Clamping)
        enemy.x = Math.max(0, Math.min(enemy.x, GamePanel.WORLD_WIDTH - enemy.size));
        enemy.y = Math.max(0, Math.min(enemy.y, GamePanel.WORLD_HEIGHT - enemy.size));
    }

    /**
     * Thuật toán phát hiện và đẩy trượt vật lý.
     * Biến quái vật thành một hình tròn ở sát chân để tương tác mượt với môi
     * trường.
     */
    public static boolean resolveHybridCollision(Enemy enemy, MapManager map) {
        boolean collided = false;
        float radius = enemy.size * 0.35f; // Tăng nhẹ để chân quái cảm giác "đầm" hơn
        float cx = enemy.x + enemy.size / 2.0f;
        float cy = enemy.y + enemy.size * 0.85f; // Đặt tâm va chạm ở chân quái

        List<Obstacle> nearObs = map.getObstaclesInRadius(cx, cy, radius * 2 + TILE_SIZE);

        for (Obstacle obs : nearObs) {
            Hitbox hb = obs.getHitbox();
            if (hb == null)
                continue;

            if (hb instanceof CircleHitbox) {
                CircleHitbox cb = (CircleHitbox) hb;
                float dx = cx - cb.centerX;
                float dy = cy - cb.centerY;
                float distSq = dx * dx + dy * dy;
                float minDist = radius + cb.radius;

                if (distSq < minDist * minDist) {
                    collided = true;
                    float dist = (float) Math.sqrt(distSq);
                    // Nếu tâm trùng nhau, tạo một hướng đẩy ngẫu nhiên nhẹ để không bị kẹt cứng
                    if (dist < 0.1f) {
                        dx = (float) Math.random() - 0.5f;
                        dy = (float) Math.random() - 0.5f;
                        dist = (float) Math.sqrt(dx * dx + dy * dy);
                    }

                    float overlap = minDist - dist;
                    float nx = dx / dist;
                    float ny = dy / dist;

                    enemy.x += nx * overlap;
                    enemy.y += ny * overlap;

                    // Cập nhật lại tâm ngay lập tức cho các lần kiểm tra vật thể tiếp theo
                    cx = enemy.x + enemy.size / 2.0f;
                    cy = enemy.y + enemy.size * 0.85f;
                }
            } else if (hb instanceof AABBHitbox) {
                AABBHitbox ab = (AABBHitbox) hb;
                float testX = Math.max(ab.x, Math.min(cx, ab.x + ab.width));
                float testY = Math.max(ab.y, Math.min(cy, ab.y + ab.height));
                float dx = cx - testX;
                float dy = cy - testY;
                float distSq = dx * dx + dy * dy;

                if (distSq > 0 && distSq < radius * radius) {
                    collided = true;
                    float dist = (float) Math.sqrt(distSq);
                    float overlap = radius - dist;
                    enemy.x += (dx / dist) * overlap;
                    enemy.y += (dy / dist) * overlap;
                } else if (distSq == 0) {
                    collided = true;
                    float dl = cx - ab.x;
                    float dr = (ab.x + ab.width) - cx;
                    float dt = cy - ab.y;
                    float db = (ab.y + ab.height) - cy;
                    float minDist = Math.min(Math.min(dl, dr), Math.min(dt, db));
                    if (minDist == dl)
                        enemy.x -= (dl + radius);
                    else if (minDist == dr)
                        enemy.x += (dr + radius);
                    else if (minDist == dt)
                        enemy.y -= (dt + radius);
                    else
                        enemy.y += (db + radius);
                }
                cx = enemy.x + enemy.size / 2.0f;
                cy = enemy.y + enemy.size * 0.85f;
            }
        }
        return collided;
    }

    private static float[] calculateSeparation(Enemy me, ArrayList<Enemy> enemies) {
        float sepX = 0, sepY = 0;
        int count = 0;
        float maxCheckDist = me.size * 1.5f;
        float maxCheckDistSq = maxCheckDist * maxCheckDist;

        // TỐI ƯU O(N): Chỉ kiểm tra các thực thể trong vùng lân cận để tránh lag khi
        // quái đông
        for (Enemy other : enemies) {
            if (other == me || other.isDying)
                continue;

            // Box check nhanh trước khi tính bình phương
            float dx = me.x - other.x;
            if (Math.abs(dx) > maxCheckDist)
                continue;
            float dy = me.y - other.y;
            if (Math.abs(dy) > maxCheckDist)
                continue;

            float distSq = dx * dx + dy * dy;
            if (distSq > 0 && distSq < maxCheckDistSq) {
                float dist = (float) Math.sqrt(distSq);
                float safeDistance = (me.size + other.size) * 0.45f; // Cho phép quái chồng lấn nhẹ để trông đông đúc
                                                                     // hơn
                if (dist < safeDistance) {
                    float force = (safeDistance - dist) / safeDistance;
                    sepX += (dx / dist) * force * 1.5f;
                    sepY += (dy / dist) * force * 1.5f;
                    count++;
                }
            }
            if (count > 10)
                break; // Chỉ tính toán tối đa 10 quái gần nhất để giữ 60 FPS
        }

        // Giới hạn lực đẩy tối đa để tránh bị "bắn" ra quá mạnh
        float maxSep = 4.0f;
        float sepLen = (float) Math.sqrt(sepX * sepX + sepY * sepY);
        if (sepLen > maxSep) {
            sepX = (sepX / sepLen) * maxSep;
            sepY = (sepY / sepLen) * maxSep;
        }

        return new float[] { sepX, sepY };
    }
}