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
        
        // Soul Stones Display
        int soulX = sw - 280;
        int soulY = 50;
        g2d.setColor(new Color(30, 30, 40));
        g2d.fillRoundRect(soulX, soulY, 230, 50, 20, 20);
        g2d.setColor(Color.MAGENTA);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(soulX, soulY, 230, 50, 20, 20);
        g2d.setFont(FontManager.getFont(24f));
        g2d.drawString("✨ " + PlayerData.soulStones, soulX + 30, soulY + 35);

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
        int cardH = 200;
        int gap = 30;
        int totalGridW = columns * cardW + (columns - 1) * gap;
        int startX = sw / 2 - totalGridW / 2;
        int startY = 140;

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

        // Card Base
        g2d.setColor(new Color(35, 35, 45));
        g2d.fillRoundRect(x, y, w, h, 20, 20);
        g2d.setColor(hover ? Color.CYAN : new Color(70, 70, 90));
        g2d.setStroke(new BasicStroke(hover ? 3 : 2));
        g2d.drawRoundRect(x, y, w, h, 20, 20);

        // Icon Area
        int iconSize = 50;
        int ix = x + 20;
        int iy = y + 20;
        drawSkillIcon(g2d, u, ix, iy, iconSize);

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(20f));
        String name = u.name().replace("_", " ");
        g2d.drawString(name, ix + iconSize + 15, iy + 35);

        // Level Progress Bar
        int barX = x + 20;
        int barY = y + 90;
        int barW = w - 40;
        int barH = 12;
        g2d.setColor(Color.BLACK);
        g2d.fillRoundRect(barX, barY, barW, barH, 5, 5);
        g2d.setColor(Color.CYAN);
        int fillW = (int)((float)lv / maxLv * barW);
        g2d.fillRoundRect(barX, barY, fillW, barH, 5, 5);
        
        g2d.setFont(FontManager.getFont(14f));
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString("Efficiency Level: " + lv + "/" + maxLv, barX, barY - 10);

        // Upgrade Button
        int btnW = 160;
        int btnH = 40;
        int btnX = x + w / 2 - btnW / 2;
        int btnY = y + h - 60;
        
        if (lv < maxLv) {
            int cost = 50 * (lv + 1);
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
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillOval(x, y, s, s);
        
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
            case EXPLOSIVE_CORPSE: skillColor = new Color(150, 0, 0); break;
        }
        
        g2d.setColor(skillColor);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(x + 5, y + 5, s - 10, s - 10);
        
        // Simple procedural icon
        g2d.fillOval(x + s/2 - 5, y + s/2 - 5, 10, 10);
    }
}
