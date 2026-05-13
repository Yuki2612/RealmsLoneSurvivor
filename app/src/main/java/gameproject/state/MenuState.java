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
            int sh = game.screenHeight;

            int actionY = (int)(sh * 0.32f); 
            if (actionY < 260) actionY = 260;
            
            int metaY = actionY + 90;
            int mSpc = 55;
            int exitY = sh - 70;
            int uSpc = 45;

            // --- Cluster 1: ACTION ---
            if (checkHit(game, mx, my, 98, actionY, "START GAME", 38f)) {
                game.changeState(new CharacterSelectState());
            } 
            // --- Cluster 2: META ---
            else if (checkHit(game, mx, my, 125, metaY, "CHARACTER STATS", 18f)) {
                game.changeState(new StatsState());
            } else if (checkHit(game, mx, my, 125, metaY + mSpc, "SKILLS", 18f)) {
                game.changeState(new SkillsState());
            } else if (checkHit(game, mx, my, 125, metaY + mSpc * 2, "ACHIEVEMENTS", 18f)) {
                game.changeState(new AchievementState());
            }
            // --- Cluster 3: UTILITY ---
            else if (checkHit(game, mx, my, 125, exitY - uSpc * 2, "SETTINGS", 18f)) {
                game.changeState(new SettingsState());
            } else if (checkHit(game, mx, my, 125, exitY - uSpc, "SURVIVAL GUIDE", 18f)) {
                game.changeState(new GuideState());
            } else if (checkHit(game, mx, my, 125, exitY, "EXIT TO DESKTOP", 18f)) {
                gameproject.meta.PlayerData.save();
                System.exit(0);
            }

            game.input.clearClickAndKey();
        }
    }

    private boolean checkHit(GamePanel game, int mx, int my, int x, int y, String text, float fontSize) {
        java.awt.Font font = gameproject.FontManager.getFont(fontSize);
        java.awt.FontMetrics fm = game.getFontMetrics(font);
        int tw = fm.stringWidth(text);
        int th = fm.getHeight();
        // Matching the hitbox logic in MenuUI
        return mx >= x && mx <= x + tw && my >= y - th + 10 && my <= y + 10;
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        MenuUI.draw(g, game.screenWidth, game.screenHeight, game.input.mouseX, game.input.mouseY);
    }
}
