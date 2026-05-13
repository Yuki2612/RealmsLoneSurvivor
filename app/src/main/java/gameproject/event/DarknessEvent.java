package gameproject.event;

import java.awt.Graphics2D;
import gameproject.GamePanel;
import gameproject.state.PlayingState;

public class DarknessEvent implements GameEvent {
    @Override
    public void onStart(GamePanel game, long currentTime) {
        gameproject.meta.AchievementManager.getInstance().onEventMet("darkness");
    }

    @Override
    public void update(GamePanel game, long currentTime) {
    }

    @Override
    public void render(Graphics2D g2d, GamePanel game, int screenW, int screenH, long currentTime) {
        // Darkness rendering is handled by VFXManager because it needs complex vignettes
        // But we can trigger it here or just let VFXManager check the activeEvent
    }

    @Override
    public void onEnd(GamePanel game, long currentTime) {
    }

    @Override
    public String getWarningMessage(long timeLeftSeconds) {
        return "⚠ DARKNESS APPROACHING IN " + timeLeftSeconds + "s!";
    }

    @Override
    public String getStartMessage() {
        return "NIGHTFALL!";
    }
}
