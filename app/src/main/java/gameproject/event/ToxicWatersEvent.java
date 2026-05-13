package gameproject.event;

import java.awt.Color;
import java.awt.Graphics2D;
import gameproject.GamePanel;
import gameproject.state.PlayingState;

public class ToxicWatersEvent implements GameEvent {
    private long lastDamageTick = 0;

    @Override
    public void onStart(GamePanel game, long currentTime) {
    }

    @Override
    public void update(GamePanel game, long currentTime) {
        if (PlayingState.eventPhase != PlayingState.EventPhase.ACTIVE) return;

        // Damage player every 1000ms if on water
        if (currentTime - lastDamageTick > 1000) {
            float px = game.player.getX() + gameproject.Player.SIZE / 2f;
            float py = game.player.getY() + gameproject.Player.SIZE / 2f;
            if (game.mapManager.getTileTypeAtWorld(px, py).equals("water")) {
                if (game.player.takeHit()) {
                    gameproject.meta.AchievementManager.getInstance().onStatChanged("fail", 1, "death_toxic_waters");
                    game.triggerGameOver();
                } else {
                    game.vfxManager.triggerPlayerDamageFlash(currentTime);
                }
            }
            lastDamageTick = currentTime;
        }

        // Spawn toxic smoke particles around player if on water or near water
        for (int i = 0; i < 8; i++) {
            float rx = game.player.getX() + (float)(Math.random() * 1000 - 500);
            float ry = game.player.getY() + (float)(Math.random() * 800 - 400);
            if (game.mapManager.getTileTypeAtWorld(rx, ry).equals("water")) {
                game.vfxManager.spawnToxicSmoke(rx, ry, currentTime);
            }
        }
    }

    @Override
    public void render(Graphics2D g2d, GamePanel game, int screenW, int screenH, long currentTime) {
        if (PlayingState.activeEvent == PlayingState.EventType.NONE) return;
        
        // Toxic Fog overlay
        float alpha = 0.2f;
        if (PlayingState.eventPhase == PlayingState.EventPhase.WARNING) {
            alpha = 0.1f;
        } else if (PlayingState.eventPhase == PlayingState.EventPhase.ACTIVE) {
            alpha = 0.3f;
        } else if (PlayingState.eventPhase == PlayingState.EventPhase.ENDING) {
            alpha = 0.15f;
        }
        
        g2d.setColor(new Color(50, 100, 30, (int)(alpha * 255)));
        g2d.fillRect(0, 0, screenW, screenH);
    }

    @Override
    public void onEnd(GamePanel game, long currentTime) {
    }

    @Override
    public String getWarningMessage(long timeLeftSeconds) {
        return "⚠ THE WATERS ARE BECOMING TOXIC AND FOG IS THICKENING! (" + timeLeftSeconds + "s)";
    }

    @Override
    public String getStartMessage() {
        return "TOXIC FOG AND WATERS ACTIVE! STAY ON DRY LAND!";
    }
}
