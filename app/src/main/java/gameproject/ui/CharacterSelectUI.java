package gameproject.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import gameproject.FontManager;
import gameproject.ImageManager;
import gameproject.meta.CharacterClass;
import gameproject.meta.PlayerData;

public class CharacterSelectUI {
    public static void draw(Graphics g, int sw, int sh, CharacterClass[] classes, int selectedIndex, int mouseX, int mouseY) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark Background
        g2d.setColor(new Color(10, 10, 15));
        g2d.fillRect(0, 0, sw, sh);
        
        // --- Header ---
        g2d.setColor(new Color(100, 100, 200, 50));
        g2d.fillRect(0, 0, sw, 80);
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(32f));
        g2d.drawString("SELECT YOUR HERO", sw / 2 - 160, 50);

        CharacterClass c = classes[selectedIndex];
        boolean isUnlocked = PlayerData.unlockedClasses.contains(c);

        // --- Layout Constants ---
        int cardW = 280;
        int cardH = 420;
        int centerW = 380;
        int centerH = 500;
        
        int startX = 50; // Kéo bảng trái ra sát biên
        int panelY = sh / 2 - cardH / 2 + 40;

        // --- Center: Character Preview ---
        int centerX = sw / 2 - centerW / 2;
        int centerY = sh / 2 - centerH / 2 + 10;

        // Shadow Glow
        g2d.setColor(isUnlocked ? new Color(0, 255, 255, 20) : new Color(255, 50, 50, 15));
        g2d.fillOval(centerX - 20, centerY + centerH - 60, centerW + 40, 100);

        // Center Frame
        g2d.setColor(new Color(25, 25, 35));
        g2d.fillRoundRect(centerX, centerY, centerW, centerH, 30, 30);
        g2d.setColor(isUnlocked ? Color.CYAN : new Color(80, 80, 90));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(centerX, centerY, centerW, centerH, 30, 30);

        // Character Name (Inside Frame)
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(40f));
        String name = c.name.toUpperCase();
        int nameW = g2d.getFontMetrics().stringWidth(name);
        g2d.drawString(name, sw / 2 - nameW / 2, centerY + 55);
        
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(centerX + 40, centerY + 70, centerX + centerW - 40, centerY + 70);

        // Image Holder
        BufferedImage charImg = ImageManager.get("player" + (selectedIndex + 1));
        if (charImg != null) {
            g2d.drawImage(charImg, centerX + 40, centerY + 100, centerW - 80, centerH - 160, null);
        } else {
            g2d.setColor(new Color(15, 15, 25));
            g2d.fillRoundRect(centerX + 30, centerY + 90, centerW - 60, centerH - 150, 20, 20);
            g2d.setColor(new Color(100, 100, 120));
            g2d.setFont(FontManager.getFont(18f));
            g2d.drawString("PREVIEW", centerX + centerW/2 - 40, centerY + centerH/2);
            g2d.setFont(FontManager.getFont(14f));
            g2d.drawString("(player" + (selectedIndex + 1) + ".png)", centerX + centerW/2 - 60, centerY + centerH/2 + 25);
        }

        // Navigation Arrows (Nằm giữa khung và bảng)
        drawArrow(g2d, centerX - 60, sh / 2, true, mouseX, mouseY);
        drawArrow(g2d, centerX + centerW + 60, sh / 2, false, mouseX, mouseY);

        // --- Left Panel: Stats ---
        drawPanel(g2d, "ATTRIBUTES", startX, panelY, cardW, cardH);
        g2d.setFont(FontManager.getFont(22f));
        int textY = panelY + 80;
        drawStatLine(g2d, "HP", String.valueOf(c.baseHp), startX + 30, textY, cardW - 60);
        drawStatLine(g2d, "Speed", "x" + c.speedMulti, startX + 30, textY + 50, cardW - 60);
        drawStatLine(g2d, "Power", "x" + c.damageMulti, startX + 30, textY + 100, cardW - 60);
        
        g2d.setColor(new Color(150, 150, 150));
        g2d.setFont(FontManager.getFont(16f));
        g2d.drawString("Signature Trait:", startX + 30, textY + 170);
        g2d.setColor(Color.CYAN);
        g2d.setFont(FontManager.getFont(20f));
        String skill = c.startingUpgrade == null ? "None" : c.startingUpgrade.name().replace("_", " ");
        g2d.drawString(skill, startX + 30, textY + 205);

        // --- Right Panel: Selection ---
        int rightX = sw - cardW - 50; // Kéo bảng phải ra sát biên
        drawPanel(g2d, isUnlocked ? "RECRUIT" : "LOCKED", rightX, panelY, cardW, cardH);
        
        // Resources
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("Gold: " + PlayerData.gold, rightX + 40, panelY + 90);
        g2d.setColor(Color.MAGENTA);
        g2d.drawString("Souls: " + PlayerData.soulStones, rightX + 40, panelY + 130);

        int btnW = 200, btnH = 55;
        int btnX = rightX + cardW / 2 - btnW / 2;
        int btnY = panelY + cardH - 85;

        if (isUnlocked) {
            g2d.setColor(new Color(0, 100, 50));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 12, 12);
            g2d.setColor(Color.GREEN);
            g2d.drawRoundRect(btnX, btnY, btnW, btnH, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.getFont(24f));
            g2d.drawString("SELECT", btnX + 55, btnY + 36);
        } else {
            boolean canAfford = PlayerData.gold >= c.unlockCost;
            g2d.setColor(canAfford ? new Color(80, 80, 0) : new Color(60, 20, 20));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 12, 12);
            g2d.setColor(canAfford ? Color.YELLOW : Color.RED);
            g2d.drawRoundRect(btnX, btnY, btnW, btnH, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.getFont(18f));
            g2d.drawString("UNLOCK: " + c.unlockCost, btnX + 35, btnY + 35);
        }

        // Back
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawString("ESC to Back", sw / 2 - 50, sh - 30);
    }

    private static void drawPanel(Graphics2D g2d, String title, int x, int y, int w, int h) {
        g2d.setColor(new Color(30, 30, 40, 230));
        g2d.fillRoundRect(x, y, w, h, 20, 20);
        g2d.setColor(new Color(70, 70, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, w, h, 20, 20);
        
        g2d.setColor(Color.CYAN);
        g2d.setFont(FontManager.getFont(16f));
        g2d.drawString(title, x + 25, y + 35);
        g2d.drawLine(x + 20, y + 45, x + w - 20, y + 45);
    }

    private static void drawStatLine(Graphics2D g2d, String label, String value, int x, int y, int w) {
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString(label, x, y);
        g2d.setColor(Color.WHITE);
        int valW = g2d.getFontMetrics().stringWidth(value);
        g2d.drawString(value, x + w - valW, y);
    }

    private static void drawArrow(Graphics2D g2d, int x, int y, boolean left, int mx, int my) {
        boolean hover = Math.pow(mx - x, 2) + Math.pow(my - y, 2) <= 1200;
        g2d.setColor(hover ? Color.CYAN : Color.WHITE);
        int size = 25;
        int[] px, py;
        if (left) {
            px = new int[]{x - 10, x + 15, x + 15};
            py = new int[]{y, y - size, y + size};
        } else {
            px = new int[]{x + 10, x - 15, x - 15};
            py = new int[]{y, y - size, y + size};
        }
        g2d.fillPolygon(px, py, 3);
    }
}
