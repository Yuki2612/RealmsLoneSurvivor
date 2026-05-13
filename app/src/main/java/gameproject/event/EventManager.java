package gameproject.event;

import java.awt.Graphics2D;
import gameproject.GamePanel;
import gameproject.state.PlayingState;
import gameproject.state.PlayingState.EventPhase;
import gameproject.state.PlayingState.EventType;

public class EventManager {
    private GameEvent currentEventInstance = null;
 
    public void reset() {
        currentEventInstance = null;
    }

    public void update(GamePanel game, long currentTime) {
        if (PlayingState.activeEvent == EventType.NONE) {
            currentEventInstance = null;
            return;
        }

        if (currentEventInstance == null) {
            currentEventInstance = createEventInstance(PlayingState.activeEvent);
        }

        if (currentEventInstance != null) {
            currentEventInstance.update(game, currentTime);
        }
    }

    public void render(Graphics2D g2d, GamePanel game, int sw, int sh, long currentTime) {
        if (currentEventInstance != null) {
            currentEventInstance.render(g2d, game, sw, sh, currentTime);
        }
    }

    public void onPhaseChanged(GamePanel game, EventPhase oldPhase, EventPhase newPhase, long currentTime) {
        if (currentEventInstance == null) return;

        if (newPhase == EventPhase.ACTIVE) {
            currentEventInstance.onStart(game, currentTime);
        } else if (newPhase == EventPhase.ENDING && oldPhase == EventPhase.ACTIVE) {
            currentEventInstance.onEnd(game, currentTime);
        }
    }

    public String getWarningMessage(long timeLeftSeconds) {
        if (currentEventInstance != null) {
            return currentEventInstance.getWarningMessage(timeLeftSeconds);
        }
        return "";
    }

    public String getStartMessage() {
        if (currentEventInstance != null) {
            return currentEventInstance.getStartMessage();
        }
        return "";
    }

    private GameEvent createEventInstance(EventType type) {
        switch (type) {
            case ACID_RAIN: return new AcidRainEvent();
            case DARKNESS: return new DarknessEvent();
            case MIMIC_MANIA: return new MimicManiaEvent();
            case BLOOD_MOON: return new BloodMoonEvent();
            case TOXIC_WATERS: return new ToxicWatersEvent();
            case ALTAR_ASCENSION: return new AltarAscensionEvent();
            default: return null;
        }
    }
}
