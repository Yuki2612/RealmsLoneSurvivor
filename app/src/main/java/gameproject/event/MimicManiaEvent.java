package gameproject.event;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import gameproject.GamePanel;
import gameproject.entity.EventTreasure;
import gameproject.entity.Mimic;
import gameproject.environment.Building;
import gameproject.state.PlayingState;

public class MimicManiaEvent implements GameEvent {
    @Override
    public void onStart(GamePanel game, long currentTime) {
        // Spawn chests when ACTIVE phase starts (triggered by EventManager later)
        synchronized (game.buildings) {
            for (Building b : game.buildings) {
                Rectangle r = b.getBounds();
                game.entityManager.eventChests.add(new EventTreasure(
                        r.x + r.width / 2 - 20, r.y + r.height / 2 - 20));
            }
        }
    }

    @Override
    public void update(GamePanel game, long currentTime) {
    }

    @Override
    public void render(Graphics2D g2d, GamePanel game, int screenW, int screenH, long currentTime) {
    }

    @Override
    public void onEnd(GamePanel game, long currentTime) {
        // Convert remaining chests to mimics
        List<EventTreasure> chestsToConvert;
        synchronized (game.entityManager.eventChests) {
            chestsToConvert = new ArrayList<>(game.entityManager.eventChests);
            game.entityManager.eventChests.clear();
        }
        synchronized (game.entityManager.enemies) {
            for (EventTreasure et : chestsToConvert) {
                game.entityManager.enemies.add(new Mimic(et.x, et.y, game.entityManager.waveCount));
            }
        }
        game.vfxManager.showWaveBanner("THE REMAINING CHESTS AWAKEN!", Color.RED, currentTime);
        gameproject.meta.AchievementManager.getInstance().onEventMet("mimic_mania");
    }

    @Override
    public String getWarningMessage(long timeLeftSeconds) {
        return "⚠ FIND TREASURES IN BUILDINGS! (" + timeLeftSeconds + "s)";
    }

    @Override
    public String getStartMessage() {
        return "FIND THEM BEFORE THEY AWAKEN!";
    }
}
