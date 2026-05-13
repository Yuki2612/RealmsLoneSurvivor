package gameproject.event;

import java.awt.Graphics2D;
import gameproject.GamePanel;

public class BloodMoonEvent implements GameEvent {
    @Override
    public void onStart(GamePanel game, long currentTime) {
        gameproject.meta.AchievementManager.getInstance().onEventMet("blood_moon");
    }

    @Override
    public void update(GamePanel game, long currentTime) {
    }

    @Override
    public void render(Graphics2D g2d, GamePanel game, int screenW, int screenH, long currentTime) {
        // Handled by VFXManager for now
    }

    @Override
    public void onEnd(GamePanel game, long currentTime) {
    }

    @Override
    public String getWarningMessage(long timeLeftSeconds) {
        return "⚠ BLOOD MOON IN " + timeLeftSeconds + "s! PREPARE FOR CARNAGE!";
    }

    @Override
    public String getStartMessage() {
        return "THE BLOOD MOON RISES!";
    }
}
