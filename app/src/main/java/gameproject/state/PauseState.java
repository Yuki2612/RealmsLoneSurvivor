package gameproject.state;

import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.ui.PauseGameOverUI;

public class PauseState implements State {
    @Override
    public void update(GamePanel game) {
        if (game.input.escPressed) {
            game.input.clearClickAndKey();
            game.changeState(new PlayingState());
            return;
        }

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;
            
            int cx = game.screenWidth / 2;
            int cy = game.screenHeight / 2;
            int btnW = 320;
            int btnH = 65;
            int btnX = cx - btnW / 2;
            int startY = cy - 60;
            int spacing = 25;

            // Check horizontal range for all buttons
            if (mx >= btnX && mx <= btnX + btnW) {
                // 1. RESUME
                if (my >= startY && my <= startY + btnH) {
                    game.changeState(new PlayingState());
                }
                // 2. MAIN MENU
                else if (my >= startY + (btnH + spacing) && my <= startY + (btnH + spacing) + btnH) {
                    gameproject.meta.PlayerData.save();
                    game.changeState(new MenuState());
                }
                // 3. EXIT GAME
                else if (my >= startY + (btnH + spacing) * 2 && my <= startY + (btnH + spacing) * 2 + btnH) {
                    gameproject.meta.PlayerData.save();
                    System.exit(0);
                }
            }
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        PauseGameOverUI.drawPaused(g, game.screenWidth, game.screenHeight);
    }
}
