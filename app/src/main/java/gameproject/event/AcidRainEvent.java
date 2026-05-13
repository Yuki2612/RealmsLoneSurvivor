package gameproject.event;

import java.awt.Color;
import java.awt.Graphics2D;
import gameproject.GamePanel;
import gameproject.environment.Building;
import gameproject.state.PlayingState;

public class AcidRainEvent implements GameEvent {
    private long lastDamageTick = 0;

    @Override
    public void onStart(GamePanel game, long currentTime) {
        gameproject.meta.AchievementManager.getInstance().onEventMet("acid_rain");
    }

    @Override
    public void update(GamePanel game, long currentTime) {
        if (PlayingState.eventPhase != PlayingState.EventPhase.ACTIVE) return;

        // Damage player every 500ms if not in building
        if (currentTime - lastDamageTick > 500) {
            boolean safe = false;
            synchronized (game.buildings) {
                for (Building b : game.buildings) {
                    if (b.isPlayerInside()) {
                        safe = true;
                        break;
                    }
                }
            }
            if (!safe) {
                int oldHearts = game.player.getHearts();
                if (game.player.takeHit()) {
                    game.triggerGameOver();
                } else if (game.player.getHearts() < oldHearts) {
                    game.vfxManager.triggerPlayerDamageFlash(currentTime);
                }
            }
            lastDamageTick = currentTime;
        }
    }

    @Override
    public void render(Graphics2D g2d, GamePanel game, int screenW, int screenH, long currentTime) {
        if (PlayingState.eventPhase != PlayingState.EventPhase.ACTIVE) return;
        
        // Draw acid rain visual (Moved from VFXManager or keep there? Let's implement here for clarity)
        g2d.setColor(new Color(150, 255, 0, 40));
        g2d.fillRect(0, 0, screenW, screenH);
        
        g2d.setColor(new Color(100, 255, 50, 100));
        for (int i = 0; i < 30; i++) {
            int rx = (int) ((currentTime / 20 + i * 137) % screenW);
            int ry = (int) ((currentTime / 2 + i * 89) % screenH);
            g2d.drawLine(rx, ry, rx - 2, ry + 10);
        }
    }

    @Override
    public void onEnd(GamePanel game, long currentTime) {
    }

    @Override
    public String getWarningMessage(long timeLeftSeconds) {
        return "⚠ ACID RAIN IN " + timeLeftSeconds + "s! FIND SHELTER!";
    }

    @Override
    public String getStartMessage() {
        return "ACID RAIN ACTIVE!";
    }
}
