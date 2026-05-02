package gameproject.state;

import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.ui.GuideUI;

public class GuideState implements State {
    private int currentPage = 0;
    private final int MAX_PAGES = 6;

    @Override
    public void update(GamePanel game) {
        if (game.input.escPressed) {
            game.input.clearClickAndKey();
            game.changeState(new MenuState());
            return;
        }

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            // X Button (Top Right)
            int closeX = game.screenWidth - 80;
            int closeY = 30;
            if (mx >= closeX && mx <= closeX + 50 && my >= closeY && my <= closeY + 50) {
                game.changeState(new MenuState());
            }

            // Left Arrow
            if (my >= game.screenHeight / 2 - 50 && my <= game.screenHeight / 2 + 50) {
                if (mx >= 50 && mx <= 150) {
                    currentPage = (currentPage - 1 + MAX_PAGES) % MAX_PAGES;
                } 
                // Right Arrow
                else if (mx >= game.screenWidth - 150 && mx <= game.screenWidth - 50) {
                    currentPage = (currentPage + 1) % MAX_PAGES;
                }
            }

            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        GuideUI.draw(g, game.screenWidth, game.screenHeight, currentPage);
    }
}
