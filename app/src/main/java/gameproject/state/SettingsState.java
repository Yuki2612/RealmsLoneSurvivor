package gameproject.state;

import java.awt.Color;
import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.FontManager;
import gameproject.ui.SettingsUI;

public class SettingsState implements State {
    private boolean pendingReset = false; // Chờ xác nhận reset
    private boolean isAdminMode = false;
    private boolean showAdminInput = false;

    @Override
    public void update(GamePanel game) {
        if (showAdminInput) {
            if (game.input.typedKeySequence.endsWith("010206")) {
                isAdminMode = true;
                showAdminInput = false;
            }
        } else if (game.input.typedKeySequence.endsWith("010206")) {
            isAdminMode = true;
        }

        if (game.input.escPressed) {
            if (pendingReset) {
                pendingReset = false; // Hủy xác nhận
            } else {
                game.input.clearClickAndKey();
                game.changeState(new MenuState());
                return;
            }
            game.input.clearClickAndKey();
            return;
        }

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            int sw = game.screenWidth;
            int sh = game.screenHeight;
            int mainY = sh / 2 - 325;

            if (!pendingReset) {
                // --- Section 1: GENERAL (Damage Numbers) ---
                int btnX = sw / 2 - 200;
                int btnY = mainY + 150;
                if (mx >= btnX && mx <= btnX + 400 && my >= btnY && my <= btnY + 50) {
                    game.vfxManager.showDamageText = !game.vfxManager.showDamageText;
                }

                // --- Section 2: ADMIN ---
                int sec2Y = mainY + 240;
                if (isAdminMode) {
                    int gridX = sw / 2 - 210;
                    int gridY = sec2Y + 30;
                    int smallW = 200, smallH = 45;

                    // Gold
                    if (mx >= gridX && mx <= gridX + smallW && my >= gridY && my <= gridY + smallH) {
                        gameproject.meta.PlayerData.gold += 1000;
                    }
                    // Souls
                    if (mx >= gridX + 220 && mx <= gridX + 220 + smallW && my >= gridY && my <= gridY + smallH) {
                        gameproject.meta.PlayerData.soulStones += 100;
                    }
                    // Wave
                    if (mx >= gridX && mx <= gridX + smallW && my >= gridY + 60 && my <= gridY + 60 + smallH) {
                        gameproject.meta.PlayerData.debugStartWave = (gameproject.meta.PlayerData.debugStartWave % 50) + 1;
                    }
                    // Level
                    if (mx >= gridX + 220 && mx <= gridX + 220 + smallW && my >= gridY + 60 && my <= gridY + 60 + smallH) {
                        gameproject.meta.PlayerData.debugStartLevel = (gameproject.meta.PlayerData.debugStartLevel % 100) + 1;
                    }
                } else if (!showAdminInput) {
                    int aBtnX = sw / 2 - 150;
                    int aBtnY = sec2Y + 40;
                    if (mx >= aBtnX && mx <= aBtnX + 300 && my >= aBtnY && my <= aBtnY + 50) {
                        showAdminInput = true;
                        game.input.typedKeySequence = "";
                    }
                }

                // --- Section 3: DATA (Reset) ---
                int rBtnX = sw / 2 - 150;
                int rBtnY = mainY + 480;
                if (mx >= rBtnX && mx <= rBtnX + 300 && my >= rBtnY && my <= rBtnY + 50) {
                    pendingReset = true;
                }

            } else {
                int by = sh / 2 - 125;
                int btnW = 140, btnH = 45;
                // --- YES ---
                int yesX = sw / 2 - 160;
                int yesY = by + 170;
                if (mx >= yesX && mx <= yesX + btnW && my >= yesY && my <= yesY + btnH) {
                    performReset();
                    pendingReset = false;
                }
                // --- NO ---
                int noX = sw / 2 + 20;
                if (mx >= noX && mx <= noX + btnW && my >= yesY && my <= yesY + btnH) {
                    pendingReset = false;
                }
            }

            game.input.clearClickAndKey();
        }
    }

    private void performReset() {
        gameproject.meta.PlayerData.gold = 0;
        gameproject.meta.PlayerData.soulStones = 0;
        gameproject.meta.PlayerData.statHealthLevel = 0;
        gameproject.meta.PlayerData.statDamageLevel = 0;
        gameproject.meta.PlayerData.statSpeedLevel = 0;
        gameproject.meta.PlayerData.statDashLevel = 0;
        gameproject.meta.PlayerData.statCritLevel = 0;
        gameproject.meta.PlayerData.statCooldownLevel = 0;
        gameproject.meta.PlayerData.skillSoulLevels.clear();
        gameproject.meta.PlayerData.debugStartWave = 1;
        gameproject.meta.PlayerData.debugStartLevel = 1;
        gameproject.meta.PlayerData.save();
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        String inputStr = showAdminInput ? game.input.typedKeySequence : "";
        SettingsUI.draw(g, game.screenWidth, game.screenHeight, game.vfxManager.showDamageText, pendingReset, isAdminMode, showAdminInput, inputStr);
    }
}
