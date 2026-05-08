package gameproject.state;

import java.awt.Color;
import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.FontManager;
import gameproject.ui.SettingsUI;

public class SettingsState implements State {
    private boolean pendingReset = false;
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
                pendingReset = false;
            } else {
                game.input.clearClickAndKey();
                game.changeState(new MenuState());
                return;
            }
            game.input.clearClickAndKey();
            return;
        }

        int sw = game.screenWidth;
        int sh = game.screenHeight;
        int mainW = 1150;
        int mainH = 700;
        int mainX = sw / 2 - mainW / 2;
        int mainY = sh / 2 - mainH / 2;

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            if (!pendingReset) {
                int colY = mainY + 120;
                int rightX = mainX + 600;
                int btnW = 420, btnH = 50;

                // --- Section: DISPLAY (Cột Phải) ---
                // Damage Numbers
                int t1Y = colY + 50;
                if (mx >= rightX + 40 && mx <= rightX + 40 + btnW && my >= t1Y && my <= t1Y + btnH) {
                    game.vfxManager.showDamageText = !game.vfxManager.showDamageText;
                }
                // Hitboxes
                int t2Y = colY + 120;
                if (mx >= rightX + 40 && mx <= rightX + 40 + btnW && my >= t2Y && my <= t2Y + btnH) {
                    GamePanel.showHitboxes = !GamePanel.showHitboxes;
                }

                // --- Section: ADMIN ---
                int adminY = colY + 230; // mainY + 350
                if (isAdminMode) {
                    int cardW = 250, cardH = 80;
                    int cardX = mainX + 65;
                    int cardY = adminY + 30; // mainY + 380
                    int spacing = 20;

                    if (mx >= cardX && mx <= cardX + cardW && my >= cardY && my <= cardY + cardH) {
                        gameproject.meta.PlayerData.gold += 1000;
                    }
                    if (mx >= cardX + (cardW+spacing) && mx <= cardX + (cardW+spacing) + cardW && my >= cardY && my <= cardY + cardH) {
                        gameproject.meta.PlayerData.soulStones += 100;
                    }
                    if (mx >= cardX + (cardW+spacing)*2 && mx <= cardX + (cardW+spacing)*2 + cardW && my >= cardY && my <= cardY + cardH) {
                        gameproject.meta.PlayerData.debugStartWave = (gameproject.meta.PlayerData.debugStartWave % gameproject.entity.EntityManager.FINAL_WAVE) + 1;
                    }
                    if (mx >= cardX + (cardW+spacing)*3 && mx <= cardX + (cardW+spacing)*3 + cardW && my >= cardY && my <= cardY + cardH) {
                        gameproject.meta.PlayerData.debugStartLevel = (gameproject.meta.PlayerData.debugStartLevel % 100) + 1;
                    }
                } else if (!showAdminInput) {
                    int aBtnX = sw / 2 - 150;
                    int aBtnY = adminY + 40;
                    if (mx >= aBtnX && mx <= aBtnX + 300 && my >= aBtnY && my <= aBtnY + 50) {
                        showAdminInput = true;
                        game.input.typedKeySequence = "";
                    }
                }

                // --- Section: DATA ---
                int dataY = adminY + 160; // mainY + 510
                int rBtnX = sw / 2 - 200;
                int rBtnY = dataY + 40; // mainY + 550
                if (mx >= rBtnX && mx <= rBtnX + 400 && my >= rBtnY && my <= rBtnY + 60) {
                    pendingReset = true;
                }
            } else {
                int by = sh / 2 - 150;
                int btnW = 160, btnH = 50;
                int yesX = sw / 2 - 180;
                int yesY = by + 210;
                if (mx >= yesX && mx <= yesX + btnW && my >= yesY && my <= yesY + btnH) {
                    performReset();
                    pendingReset = false;
                }
                int noX = sw / 2 + 20;
                if (mx >= noX && mx <= noX + btnW && my >= yesY && my <= yesY + btnH) {
                    pendingReset = false;
                }
            }
            game.input.clearClickAndKey();
        }

        // --- Volume Sliders (Dragging) ---
        if (game.input.isMouseHolding && !pendingReset) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;
            
            int colY = mainY + 120;
            int leftX = mainX + 50;
            int sliderW = 420;

            // SFX Slider
            int s1Y = colY + 60; // mainY + 180
            if (mx >= leftX + 40 - 20 && mx <= leftX + 40 + sliderW + 20 && my >= s1Y - 30 && my <= s1Y + 40) {
                float val = (float) (mx - (leftX + 40)) / sliderW;
                gameproject.SoundManager.setSfxVolume(val);
            }
            // Music Slider
            int s2Y = colY + 140; // mainY + 260
            if (mx >= leftX + 40 - 20 && mx <= leftX + 40 + sliderW + 20 && my >= s2Y - 30 && my <= s2Y + 40) {
                float val = (float) (mx - (leftX + 40)) / sliderW;
                gameproject.SoundManager.setMusicVolume(val);
            }
        }
    }

    private void performReset() {
        gameproject.meta.PlayerData.gold = 2000;
        gameproject.meta.PlayerData.soulStones = 20;
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
        SettingsUI.draw(g, game.screenWidth, game.screenHeight, game.vfxManager.showDamageText, GamePanel.showHitboxes, pendingReset, isAdminMode, showAdminInput, inputStr);
    }
}
