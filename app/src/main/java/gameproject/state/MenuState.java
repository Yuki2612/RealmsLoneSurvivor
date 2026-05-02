package gameproject.state;

import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.ui.MenuUI;

public class MenuState implements State {
    @Override
    public void update(GamePanel game) {
        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;
            
            int btnW = 320;
            int btnH = 50;
            int btnX = (game.screenWidth - btnW) / 2;
            int startY = game.screenHeight / 2 - 80;
            int spacing = 62;

            if (mx >= btnX && mx <= btnX + btnW) {
                if (my >= startY && my <= startY + btnH) {
                    game.changeState(new CharacterSelectState());
                } else if (my >= startY + spacing && my <= startY + spacing + btnH) {
                    game.changeState(new StatsState());
                } else if (my >= startY + spacing * 2 && my <= startY + spacing * 2 + btnH) {
                    game.changeState(new SkillsState());
                } else if (my >= startY + spacing * 3 && my <= startY + spacing * 3 + btnH) {
                    game.changeState(new SettingsState());
                } else if (my >= startY + spacing * 4 && my <= startY + spacing * 4 + btnH) {
                    game.changeState(new GuideState());
                } else if (my >= startY + spacing * 5 && my <= startY + spacing * 5 + btnH) {
                    gameproject.meta.PlayerData.save();
                    System.exit(0);
                }
            }
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        MenuUI.draw(g, game.screenWidth, game.screenHeight, game.input.mouseX, game.input.mouseY);
    }
}
