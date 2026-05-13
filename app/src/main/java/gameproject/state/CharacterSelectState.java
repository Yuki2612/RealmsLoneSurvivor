package gameproject.state;

import java.awt.Color;
import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.FontManager;
import gameproject.meta.CharacterClass;
import gameproject.meta.PlayerData;

public class CharacterSelectState implements State {
    
    private int selectedIndex = 0;
    private CharacterClass[] classes = CharacterClass.values();

    public CharacterSelectState() {
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] == PlayerData.selectedClass) {
                selectedIndex = i;
                break;
            }
        }
    }

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
            int sw = game.screenWidth;
            int sh = game.screenHeight;

            // Navigation Arrows
            int leftArrowX = sw / 2 - 250;
            int rightArrowX = sw / 2 + 250;
            int arrowY = sh / 2;
            
            if (Math.pow(mx - leftArrowX, 2) + Math.pow(my - arrowY, 2) <= 1600) {
                selectedIndex = (selectedIndex - 1 + classes.length) % classes.length;
            } else if (Math.pow(mx - rightArrowX, 2) + Math.pow(my - arrowY, 2) <= 1600) {
                selectedIndex = (selectedIndex + 1) % classes.length;
            }

            // Battle / Unlock Button
            int cardW = 280;
            int rightX = sw - cardW - 50;
            int btnW = 200, btnH = 55;
            int btnX = rightX + cardW / 2 - btnW / 2;
            int btnY = sh / 2 + 165;

            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                CharacterClass c = classes[selectedIndex];
                if (PlayerData.unlockedClasses.contains(c)) {
                    PlayerData.selectedClass = c;
                    PlayerData.save();
                    game.changeState(new MapSelectState());
                } else {
                    if (PlayerData.gold >= c.unlockCost) {
                        PlayerData.gold -= c.unlockCost;
                        PlayerData.unlockedClasses.add(c);
                        PlayerData.save();
                        gameproject.SoundManager.play("shoot"); 
                    }
                }
            }
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        gameproject.ui.CharacterSelectUI.draw(g, game.screenWidth, game.screenHeight, classes, selectedIndex, game.input.mouseX, game.input.mouseY);
    }
}
