package gameproject.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GradientPaint;
import java.util.List;
import gameproject.FontManager;
import gameproject.meta.PlayerData;
import gameproject.skill.Upgrade;

public class SkillsUI {
    public static void draw(Graphics g, int sw, int sh, List<Upgrade> skills, int mouseX, int mouseY, int currentPage, int totalPages) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Nền tối với Gradient nhẹ
        g2d.setColor(new Color(15, 15, 20));
        g2d.fillRect(0, 0, sw, sh);
        
        // Vẽ lưới nền mờ ảo
        g2d.setColor(new Color(30, 30, 45));
        for (int i = 0; i < sw; i += 50) g2d.drawLine(i, 0, i, sh);
        for (int i = 0; i < sh; i += 50) g2d.drawLine(0, i, sw, i);

        // --- Header ---
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(45f));
        g2d.drawString("SOUL MASTERY", sw / 2 - 180, 80);
        
        // Resources Balance
        int resX = sw - 300;
        int resY = 50;
        int resW = 250;
        int resH = 80;
        
        g2d.setColor(new Color(20, 20, 30, 240));
        g2d.fillRoundRect(resX, resY, resW, resH, 20, 20);
        g2d.setColor(new Color(70, 70, 100));
        g2d.drawRoundRect(resX, resY, resW, resH, 20, 20);

        java.awt.image.BufferedImage goldImg = gameproject.ImageManager.get("gold");
        java.awt.image.BufferedImage soulImg = gameproject.ImageManager.get("soul");

        g2d.setFont(FontManager.getFont(20f));
        if (goldImg != null) {
            g2d.drawImage(goldImg, resX + 20, resY + 12, 24, 24, null);
            g2d.setColor(Color.YELLOW);
            g2d.drawString("" + PlayerData.gold, resX + 55, resY + 32);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("G: " + PlayerData.gold, resX + 20, resY + 32);
        }

        if (soulImg != null) {
            g2d.drawImage(soulImg, resX + 20, resY + 45, 24, 24, null);
            g2d.setColor(Color.CYAN);
            g2d.drawString("" + PlayerData.soulStones, resX + 55, resY + 65);
        } else {
            g2d.setColor(Color.CYAN);
            g2d.drawString("S: " + PlayerData.soulStones, resX + 20, resY + 65);
        }

        // --- Page Indicator ---
        g2d.setColor(Color.GRAY);
        g2d.setFont(FontManager.getFont(18f));
        g2d.drawString("Page " + (currentPage + 1) + " / " + totalPages, sw / 2 - 50, sh - 30);

        // --- Navigation Arrows ---
        if (totalPages > 1) {
            g2d.setColor(Color.WHITE);
            // Left Arrow
            g2d.fillPolygon(new int[]{20, 50, 50}, new int[]{sh/2, sh/2 - 30, sh/2 + 30}, 3);
            // Right Arrow
            g2d.fillPolygon(new int[]{sw - 20, sw - 50, sw - 50}, new int[]{sh/2, sh/2 - 30, sh/2 + 30}, 3);
        }

        // --- Grid Layout ---
        int columns = 3;
        int cardW = 280;
        int cardH = 220;
        int gap = 25;
        int totalGridW = columns * cardW + (columns - 1) * gap;
        int startX = sw / 2 - totalGridW / 2;
        int startY = 130;

        for (int i = 0; i < skills.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            int x = startX + col * (cardW + gap);
            int y = startY + row * (cardH + gap);
            
            Upgrade u = skills.get(i);
            int level = PlayerData.skillSoulLevels.getOrDefault(u, 0);
            int maxLevel = 10;
            boolean isHovered = mouseX >= x && mouseX <= x + cardW && mouseY >= y && mouseY <= y + cardH;

            drawSkillCard(g2d, u, x, y, cardW, cardH, level, maxLevel, isHovered);
        }

        // --- Footer / Back ---
        int backX = 50, backY = 50;
        g2d.setColor(new Color(40, 40, 50));
        g2d.fillRoundRect(backX, backY, 120, 45, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(backX, backY, 120, 45, 15, 15);
        g2d.setFont(FontManager.getFont(20f));
        g2d.drawString("BACK", backX + 32, backY + 30);
    }

    private static void drawSkillCard(Graphics2D g2d, Upgrade u, int x, int y, int w, int h, int lv, int maxLv, boolean hover) {
        // Shadow/Glow effect on hover
        if (hover) {
            g2d.setColor(new Color(0, 255, 255, 30));
            g2d.fillRoundRect(x - 5, y - 5, w + 10, h + 10, 25, 25);
        }

        boolean isUnlocked = gameproject.meta.PlayerData.unlockedSkills.contains(u);

        // Card Base
        if (isUnlocked) {
            g2d.setColor(new Color(35, 35, 45));
        } else {
            g2d.setColor(new Color(60, 20, 20)); // Red background for locked
        }
        g2d.fillRoundRect(x, y, w, h, 20, 20);
        
        g2d.setColor(hover ? Color.CYAN : (isUnlocked ? new Color(70, 70, 90) : new Color(150, 50, 50)));
        g2d.setStroke(new BasicStroke(hover ? 3 : 2));
        g2d.drawRoundRect(x, y, w, h, 20, 20);

        // Icon Area
        int iconSize = 55;
        int ix = x + 20;
        int iy = y + 20;
        drawSkillIcon(g2d, u, ix, iy, iconSize);

        // Title & Level Info (Positioned to the right of the icon)
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(18f));
        String name = u.name().replace("_", " ");
        g2d.drawString(name, ix + iconSize + 15, iy + 25);

        if (isUnlocked) {
            g2d.setFont(FontManager.getFont(14f));
            g2d.setColor(new Color(180, 180, 200));
            g2d.drawString("Level: " + lv + "/" + maxLv, ix + iconSize + 15, iy + 45);

            // Level Progress Bar (Moved down to avoid overlap)
            int barX = x + 20;
            int barY = y + 95;
            int barW = w - 40;
            int barH = 10;
            g2d.setColor(Color.BLACK);
            g2d.fillRoundRect(barX, barY, barW, barH, 5, 5);
            g2d.setColor(Color.CYAN);
            int fillW = (int)((float)lv / maxLv * barW);
            g2d.fillRoundRect(barX, barY, fillW, barH, 5, 5);
        } else {
            g2d.setFont(FontManager.getFont(16f));
            g2d.setColor(new Color(255, 100, 100));
            g2d.drawString("LOCKED", ix + iconSize + 15, iy + 45);
        }

        // Upgrade Button
        int btnW = 160;
        int btnH = 40;
        int btnX = x + w / 2 - btnW / 2;
        int btnY = y + h - 60;
        
        if (!isUnlocked) {
            int unlockCost = 20;
            boolean canAfford = PlayerData.soulStones >= unlockCost;
            g2d.setColor(canAfford ? new Color(100, 50, 0) : new Color(50, 20, 20));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2d.setColor(canAfford ? Color.ORANGE : Color.GRAY);
            g2d.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.getFont(18f));
            g2d.drawString("UNLOCK: " + unlockCost, btnX + 18, btnY + 28);
        } else if (lv < maxLv) {
            int cost = (int) (15 * Math.pow(1.2, lv));
            boolean canAfford = PlayerData.soulStones >= cost;
            
            g2d.setColor(canAfford ? new Color(0, 100, 50) : new Color(50, 50, 50));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2d.setColor(canAfford ? Color.GREEN : Color.GRAY);
            g2d.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.getFont(18f));
            g2d.drawString("UPGRADE: " + cost, btnX + 15, btnY + 28);
        } else {
            g2d.setColor(new Color(40, 40, 40));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2d.setColor(Color.YELLOW);
            g2d.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2d.drawString("MASTERED", btnX + 35, btnY + 28);
        }
    }

    private static void drawSkillIcon(Graphics2D g2d, Upgrade u, int x, int y, int s) {
        // Attempt to load from ImageManager
        String iconKey = "skill_" + u.name().toLowerCase();
        java.awt.image.BufferedImage icon = gameproject.ImageManager.get(iconKey);
        
        if (icon != null) {
            // Draw the actual icon directly (it already has its own frame)
            g2d.drawImage(icon, x, y, s, s, null);
            return;
        }

        // Fallback procedural icon (Square)
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRect(x, y, s, s);
        
        Color skillColor = Color.WHITE;
        switch(u) {
            case CHAIN_LIGHTNING: skillColor = Color.YELLOW; break;
            case TRAIL_OF_FIRE: skillColor = Color.ORANGE; break;
            case ORBITING_ORBS: skillColor = Color.BLUE; break;
            case FROST_AURA: skillColor = Color.CYAN; break;
            case ENERGY_SHIELD: skillColor = Color.CYAN; break;
            case METEOR_STRIKE: skillColor = Color.RED; break;
            case PULSE_WAVE: skillColor = Color.MAGENTA; break;
            case POISON_CLOUD: skillColor = Color.GREEN; break;
            case EXPLOSIVE_BULLETS: skillColor = new Color(200, 50, 0); break;
        }
        
        g2d.setColor(skillColor);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(x + 5, y + 5, s - 10, s - 10);
        
        g2d.fillRect(x + s/2 - 5, y + s/2 - 5, 10, 10);
    }
}
