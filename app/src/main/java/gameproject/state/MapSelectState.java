package gameproject.state;

import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.environment.MapConfig;
import gameproject.environment.MapType;

public class MapSelectState implements State {
    private int selectedIndex = 0;
    private MapType[] mapTypes;

    public MapSelectState() {
        // Lấy tất cả các loại map
        MapType[] types = MapType.values();
        // Sắp xếp dựa trên MapConfig.mapId
        java.util.Arrays.sort(types, (a, b) -> {
            int idA = MapConfig.getConfig(a).mapId;
            int idB = MapConfig.getConfig(b).mapId;
            return Integer.compare(idA, idB);
        });
        this.mapTypes = types;
    }

    @Override
    public void update(GamePanel game) {
        if (game.input.escPressed) {
            game.input.clearClickAndKey();
            game.changeState(new CharacterSelectState());
            return;
        }

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;
            int sw = game.screenWidth;
            int sh = game.screenHeight;

            // Navigation Arrows (Matched with MapSelectUI)
            int leftArrowX = sw / 2 - 350;
            int rightArrowX = sw / 2 + 350;
            int arrowY = sh / 2 - 50;

            if (Math.pow(mx - leftArrowX, 2) + Math.pow(my - arrowY, 2) <= 2500) {
                selectedIndex = (selectedIndex - 1 + mapTypes.length) % mapTypes.length;
                gameproject.SoundManager.play("shoot");
            } else if (Math.pow(mx - rightArrowX, 2) + Math.pow(my - arrowY, 2) <= 2500) {
                selectedIndex = (selectedIndex + 1) % mapTypes.length;
                gameproject.SoundManager.play("shoot");
            }

            // Start Button
            int btnW = 240, btnH = 60;
            int btnX = sw / 2 - btnW / 2;
            int btnY = sh - 150;

            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                MapConfig selectedConfig = MapConfig.getConfig(mapTypes[selectedIndex]);
                game.startNewGame(selectedConfig);
            }
            
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        gameproject.ui.MapSelectUI.draw(g, game.screenWidth, game.screenHeight, mapTypes, selectedIndex, game.input.mouseX, game.input.mouseY);
    }
}
