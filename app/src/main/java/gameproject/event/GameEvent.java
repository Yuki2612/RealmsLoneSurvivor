package gameproject.event;

import java.awt.Graphics2D;
import gameproject.GamePanel;

public interface GameEvent {
    void onStart(GamePanel game, long currentTime);
    void update(GamePanel game, long currentTime);
    void render(Graphics2D g2d, GamePanel game, int screenW, int screenH, long currentTime);
    void onEnd(GamePanel game, long currentTime);
    
    String getWarningMessage(long timeLeftSeconds);
    String getStartMessage();
}
