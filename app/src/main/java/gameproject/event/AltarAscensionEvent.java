package gameproject.event;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import gameproject.GamePanel;
import gameproject.entity.Enemy;
import gameproject.environment.Altar;
import gameproject.environment.Obstacle;
import gameproject.state.PlayingState;

public class AltarAscensionEvent implements GameEvent {
    private long ascensionProgress = 0; // ms
    private long lastUpdateTick = 0;
    private Altar targetAltar = null;
    private final float auraRadius = 450f; // Tầm playground (Tăng từ 300)
    private final long requiredTime = 30000; // 30s
    private boolean isFinished = false;

    @Override
    public void onStart(GamePanel game, long currentTime) {
        ascensionProgress = 0;
        lastUpdateTick = currentTime;
        isFinished = false;

        // Tìm Altar trên bản đồ
        synchronized (game.mapManager.getAllObstacles()) {
            for (Obstacle obs : game.mapManager.getAllObstacles()) {
                if (obs instanceof Altar) {
                    targetAltar = (Altar) obs;
                    break;
                }
            }
        }
    }

    @Override
    public void update(GamePanel game, long currentTime) {
        if (PlayingState.eventPhase != PlayingState.EventPhase.ACTIVE)
            return;
        if (targetAltar == null)
            return;

        long dt = currentTime - lastUpdateTick;
        lastUpdateTick = currentTime;

        // Kiểm tra người chơi trong aura (Sử dụng tâm Bàn thờ)
        float worldAltarX = targetAltar.x + 32;
        float worldAltarY = targetAltar.y + 32;
        float px = game.player.getX() + 12.5f;
        float py = game.player.getY() + 12.5f;

        float dx = px - worldAltarX;
        float dy = py - worldAltarY;
        float distSq = dx * dx + dy * dy;

        if (distSq < auraRadius * auraRadius) {
            ascensionProgress += dt;
        }

        // Kết thúc sớm nếu tiến độ đầy
        if (ascensionProgress >= requiredTime) {
            PlayingState.eventEndTime = currentTime;
        }
    }

    @Override
    public void render(Graphics2D g2d, GamePanel game, int screenW, int screenH, long currentTime) {
        if (PlayingState.activeEvent == PlayingState.EventType.NONE || targetAltar == null)
            return;

        // Tọa độ màn hình của Bàn thờ
        float worldAltarX = targetAltar.x + 32;
        float worldAltarY = targetAltar.y + 32;
        float screenAltarX = worldAltarX - game.cameraX;
        float screenAltarY = worldAltarY - game.cameraY;

        // 1. Vẽ vòng tròn aura (Hiệu ứng Playground) - Màu vàng sáng rõ
        g2d.setStroke(new BasicStroke(4f));
        g2d.setColor(new Color(255, 255, 100, 180));
        g2d.drawOval((int) (screenAltarX - auraRadius), (int) (screenAltarY - auraRadius), (int) auraRadius * 2,
                (int) auraRadius * 2);

        // Hiệu ứng phát sáng nhẹ bên trong để nhận diện vùng an toàn
        g2d.setColor(new Color(255, 255, 200, 30));
        g2d.fillOval((int) (screenAltarX - auraRadius), (int) (screenAltarY - auraRadius), (int) auraRadius * 2,
                (int) auraRadius * 2);

        // 2. Vẽ thanh tiến độ (Progress Bar)
        if (PlayingState.eventPhase == PlayingState.EventPhase.ACTIVE) {
            int barW = 350;
            int barH = 15;
            int barX = (screenW - barW) / 2;
            int barY = screenH - 110; // Dời xuống gần thanh EXP

            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(barX - 4, barY - 4, barW + 8, barH + 8, 10, 10);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRoundRect(barX, barY, barW, barH, 5, 5);

            float ratio = Math.min(1.0f, (float) ascensionProgress / requiredTime);
            g2d.setColor(ratio >= 1.0f ? Color.GREEN : new Color(255, 220, 0));
            g2d.fillRoundRect(barX, barY, (int) (barW * ratio), barH, 5, 5);

            g2d.setFont(gameproject.FontManager.getFont(20f));
            String text = "ASCENSION PROGRESS: " + (int) (ratio * 100) + "%";
            // Shadow
            g2d.setColor(Color.BLACK);
            g2d.drawString(text, barX + (barW - g2d.getFontMetrics().stringWidth(text)) / 2 + 2, barY - 15 + 2);
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, barX + (barW - g2d.getFontMetrics().stringWidth(text)) / 2, barY - 15);
        }
    }

    @Override
    public void onEnd(GamePanel game, long currentTime) {
        if (isFinished)
            return;
        isFinished = true;

        if (ascensionProgress >= requiredTime) {
            // THÀNH CÔNG: Tăng 1 Max HP và gây 1000 sát thương toàn màn hình
            game.player.addMaxHeart();

            synchronized (game.entityManager.enemies) {
                for (Enemy e : game.entityManager.enemies) {
                    if (!e.isDying) {
                        e.takeDamage(1000, game.vfxManager, currentTime);
                    }
                }
            }
            game.vfxManager.showWaveBanner("ASCENSION SUCCESSFUL! THE SWAMP PURGES!", Color.GREEN, currentTime);
            gameproject.SoundManager.play("powerup");
            gameproject.meta.AchievementManager.getInstance().onStatChanged("event", 1, "altar_success");
        } else {
            // THẤT BẠI: Mất 1 máu và triệu hồi đợt quái lớn
            if (game.player.takeHit()) {
                game.triggerGameOver();
            }

            int surviveTime = (int) ((currentTime - game.startTime) / 1000);
            for (int i = 0; i < 20; i++) {
                Enemy e = game.entityManager.spawnSafeEnemy(game.player, game, surviveTime);
                synchronized (game.entityManager.enemies) {
                    game.entityManager.enemies.add(e);
                }
            }
            game.vfxManager.showWaveBanner("ASCENSION FAILED! THE SWAMP IS ANGRY!", Color.RED, currentTime);
            gameproject.SoundManager.play("playerhurt1");
            gameproject.meta.AchievementManager.getInstance().onStatChanged("event", 1, "altar_fail");
        }
    }

    @Override
    public String getWarningMessage(long timeLeftSeconds) {
        return "⚠ THE ALTAR IS CALLING... PREPARE FOR ASCENSION! (" + timeLeftSeconds + "s)";
    }

    @Override
    public String getStartMessage() {
        return "STAND WITHIN THE ALTAR'S AURA TO ASCEND!";
    }
}
