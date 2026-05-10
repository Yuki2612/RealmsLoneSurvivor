package gameproject.state;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import gameproject.GamePanel;
import gameproject.FontManager;
import gameproject.meta.PlayerData;
import gameproject.skill.Upgrade;

public class SkillsState implements State {
    private List<Upgrade> breakthroughSkills;
    private int currentPage = 0;
    private final int SKILLS_PER_PAGE = 6;

    public SkillsState() {
        breakthroughSkills = new ArrayList<>();
        for (Upgrade u : Upgrade.values()) {
            if (u.isBreakthrough) {
                breakthroughSkills.add(u);
            }
        }
    }

    @Override
    public void update(GamePanel game) {
        if (game.input.escPressed) {
            PlayerData.save();
            game.changeState(new MenuState());
            game.input.clearClickAndKey();
            return;
        }

        int maxPages = (int) Math.ceil((double) breakthroughSkills.size() / SKILLS_PER_PAGE);

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            // Navigation Arrows
            if (my >= game.screenHeight / 2 - 50 && my <= game.screenHeight / 2 + 50) {
                // Left Arrow
                if (mx >= 20 && mx <= 80) {
                    currentPage = (currentPage - 1 + maxPages) % maxPages;
                }
                // Right Arrow
                else if (mx >= game.screenWidth - 80 && mx <= game.screenWidth - 20) {
                    currentPage = (currentPage + 1) % maxPages;
                }
            }

            int columns = 3;
            int cardW = 280;
            int cardH = 220;
            int gap = 25;
            int totalGridW = columns * cardW + (columns - 1) * gap;
            int startX = (game.screenWidth - totalGridW) / 2;
            int startY = 130;

            int startIndex = currentPage * SKILLS_PER_PAGE;
            int endIndex = Math.min(startIndex + SKILLS_PER_PAGE, breakthroughSkills.size());

            for (int i = startIndex; i < endIndex; i++) {
                int displayIdx = i - startIndex;
                int row = displayIdx / columns;
                int col = displayIdx % columns;
                int x = startX + col * (cardW + gap);
                int y = startY + row * (cardH + gap);

                // Nút Upgrade
                int btnW = 160;
                int btnH = 40;
                int btnX = x + cardW / 2 - btnW / 2;
                int btnY = y + cardH - 60;

                if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                    Upgrade u = breakthroughSkills.get(i);
                    boolean isUnlocked = PlayerData.unlockedSkills.contains(u);

                    if (!isUnlocked) {
                        int unlockCost = 20;
                        if (PlayerData.soulStones >= unlockCost) {
                            PlayerData.soulStones -= unlockCost;
                            PlayerData.unlockedSkills.add(u);
                            gameproject.SoundManager.play("levelup");
                            PlayerData.save();
                        }
                    } else {
                        int level = PlayerData.skillSoulLevels.getOrDefault(u, 0);
                        int maxSoulLevel = 10;
                        if (level < maxSoulLevel) {
                            int cost = (int) (15 * Math.pow(1.2, level));
                            if (PlayerData.soulStones >= cost) {
                                PlayerData.soulStones -= cost;
                                PlayerData.skillSoulLevels.put(u, level + 1);
                                gameproject.SoundManager.play("levelup");
                                PlayerData.save();
                            }
                        }
                    }
                }
            }

            // Nút Back
            if (mx >= 50 && mx <= 170 && my >= 50 && my <= 95) {
                PlayerData.save();
                game.changeState(new MenuState());
            }

            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        int startIndex = currentPage * SKILLS_PER_PAGE;
        int endIndex = Math.min(startIndex + SKILLS_PER_PAGE, breakthroughSkills.size());
        List<Upgrade> pageSkills = breakthroughSkills.subList(startIndex, endIndex);
        int totalPages = (int) Math.ceil((double) breakthroughSkills.size() / SKILLS_PER_PAGE);

        gameproject.ui.SkillsUI.draw(g, game.screenWidth, game.screenHeight, pageSkills, game.input.mouseX,
                game.input.mouseY, currentPage, totalPages);
    }
}
