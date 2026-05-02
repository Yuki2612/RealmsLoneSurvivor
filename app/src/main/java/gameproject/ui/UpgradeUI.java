package gameproject.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import gameproject.FontManager;
import gameproject.ImageManager;
import gameproject.Player;
import gameproject.skill.Upgrade;
import gameproject.GamePanel;

public class UpgradeUI {
    private static float animationTimer = 0;
    private static long lastTime = 0;

    public static void draw(Graphics g, int screenWidth, int screenHeight, int playerLevel, Upgrade[] currentUpgradeOptions, Player player, int mouseX, int mouseY) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        long currentTime = GamePanel.getTickTime();
        if (lastTime == 0) lastTime = currentTime;
        animationTimer += (currentTime - lastTime) / 1000f;
        lastTime = currentTime;

        // --- 1. Background: Fantasy Mystic Realm ---
        g2d.setColor(new Color(15, 10, 20, 230)); // Deep dark purple/black
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        
        // Draw Magic Runes / Circles in background
        drawMagicBackground(g2d, screenWidth, screenHeight, animationTimer);

        boolean isBreakthrough = playerLevel % 3 == 0;
        
        // --- 2. Fantasy Title ---
        drawFantasyTitle(g2d, screenWidth, screenHeight, isBreakthrough);

        // --- 3. Card Layout (Fantasy Scroll/Tablet style) ---
        int boxWidth = 320;
        int boxHeight = 400;
        int spacing = 50;
        int startX = (screenWidth - (3 * boxWidth + 2 * spacing)) / 2;
        int by = (screenHeight - boxHeight) / 2 + 50;

        for (int i = 0; i < 3; i++) {
            int bx = startX + i * (boxWidth + spacing);
            Upgrade u = currentUpgradeOptions[i];
            
            boolean hovered = mouseX >= bx && mouseX <= bx + boxWidth && mouseY >= by && mouseY <= by + boxHeight;
            drawFantasyCard(g2d, bx, by, boxWidth, boxHeight, u, player, hovered, animationTimer);
        }
    }

    private static void drawMagicBackground(Graphics2D g2d, int sw, int sh, float time) {
        // Large rotating magic circle
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(new Color(150, 100, 255, 30));
        
        int circleSize = 600;
        AffineTransform old = g2d.getTransform();
        g2d.translate(sw / 2, sh / 2);
        g2d.rotate(time * 0.15);
        
        g2d.drawOval(-circleSize/2, -circleSize/2, circleSize, circleSize);
        g2d.drawRect(-circleSize/2 + 50, -circleSize/2 + 50, circleSize - 100, circleSize - 100);
        
        g2d.rotate(-time * 0.3); // Counter-rotate inner part
        g2d.drawPolygon(new int[]{0, 200, -200}, new int[]{-230, 115, 115}, 3);
        g2d.drawPolygon(new int[]{0, 200, -200}, new int[]{230, -115, -115}, 3);
        
        g2d.setTransform(old);

        // Floating magic particles
        for (int i = 0; i < 50; i++) {
            long seed = i * 12345L;
            float px = (float) ((seed % 1000) / 1000.0 * sw);
            float py = (float) (((seed + time * 40) % 1000) / 1000.0 * sh);
            int size = (int) (seed % 4) + 2;
            g2d.setColor(new Color(200, 150, 255, 40));
            g2d.fillOval((int)px, (int)py, size, size);
        }
    }

    private static void drawFantasyTitle(Graphics2D g2d, int sw, int sh, boolean isBreakthrough) {
        String title = isBreakthrough ? "ASCENSION" : "ENHANCEMENT";
        Color mainColor = isBreakthrough ? new Color(255, 80, 200) : new Color(255, 215, 100);
        
        g2d.setFont(FontManager.getFont(55f));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (sw - fm.stringWidth(title)) / 2;
        int ty = sh / 2 - 240;

        // Elegant Shadow
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(title, tx + 4, ty + 4);

        // Gradient for Gold/Silver look
        GradientPaint grad = new GradientPaint(tx, ty - 40, mainColor.brighter(), tx, ty, mainColor.darker());
        g2d.setPaint(grad);
        g2d.drawString(title, tx, ty);

        // Decorative Flourish
        g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 100));
        int lw = fm.stringWidth(title) + 160;
        int lx = (sw - lw) / 2;
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(lx, ty + 15, lx + lw, ty + 15);
        g2d.fillOval(lx - 5, ty + 10, 10, 10);
        g2d.fillOval(lx + lw - 5, ty + 10, 10, 10);
    }

    private static void drawFantasyCard(Graphics2D g2d, int x, int y, int w, int h, Upgrade u, Player player, boolean hovered, float time) {
        Color themeColor = u.isBreakthrough ? new Color(255, 100, 255) : new Color(255, 200, 80);
        if (hovered) themeColor = themeColor.brighter();

        int dx = x, dy = y, dw = w, dh = h;
        if (hovered) {
            dx -= 8; dy -= 8; dw += 16; dh += 16;
        }

        // 1. Card Body (Stone / Dark Metal texture look)
        GradientPaint bodyGrad = new GradientPaint(dx, dy, new Color(35, 30, 45, 250), dx, dy + dh, new Color(20, 15, 25, 250));
        g2d.setPaint(bodyGrad);
        g2d.fillRoundRect(dx, dy, dw, dh, 10, 10);

        // 2. Ornate Border (Fantasy style)
        g2d.setColor(hovered ? themeColor : new Color(150, 120, 80));
        g2d.setStroke(new BasicStroke(hovered ? 3f : 2f));
        g2d.drawRoundRect(dx, dy, dw, dh, 10, 10);
        
        // Inner thin border
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawRoundRect(dx + 5, dy + 5, dw - 10, dh - 10, 8, 8);

        // 3. Content
        if (u.isBreakthrough) {
            drawBreakthroughContent(g2d, dx, dy, dw, dh, u, themeColor, hovered);
        } else {
            drawNormalContent(g2d, dx, dy, dw, dh, u, themeColor, hovered);
        }

        // 4. Level Progress (Elegant gems/dots style)
        drawElegantLevelInfo(g2d, dx, dy + dh - 85, dw, u, player, themeColor);

        if (hovered) {
            g2d.setFont(FontManager.getFont(20f));
            g2d.setColor(themeColor);
            String selectTxt = "✦ CLAIM POWER ✦";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(selectTxt, dx + (dw - fm.stringWidth(selectTxt)) / 2, dy + dh - 20);
        }
    }

    private static void drawNormalContent(Graphics2D g2d, int x, int y, int w, int h, Upgrade u, Color theme, boolean hovered) {
        int imgSize = 100;
        int ix = x + (w - imgSize) / 2;
        int iy = y + 45;

        BufferedImage icon = ImageManager.get("skill_" + u.name().toLowerCase());
        if (icon != null) {
            g2d.drawImage(icon, ix, iy, imgSize, imgSize, null);
        } else {
            // Fallback: draw a simple shape or rune
            g2d.setColor(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 80));
            g2d.drawRect(ix, iy, imgSize, imgSize);
            g2d.setFont(FontManager.getFont(30f));
            g2d.drawString("?", ix + imgSize/2 - 10, iy + imgSize/2 + 10);
        }

        String fullDesc = u.description;
        String title = fullDesc.contains("(") ? fullDesc.substring(0, fullDesc.indexOf("(")).trim() : fullDesc;
        String details = fullDesc.contains("(") ? fullDesc.substring(fullDesc.indexOf("(")) : "";

        // Title (Centered)
        g2d.setFont(FontManager.getFont(24f));
        g2d.setColor(Color.WHITE);
        FontMetrics fmT = g2d.getFontMetrics();
        g2d.drawString(title, x + (w - fmT.stringWidth(title)) / 2, y + 185);

        // Details (Centered)
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(200, 220, 255));
        FontMetrics fmD = g2d.getFontMetrics();
        g2d.drawString(details, x + (w - fmD.stringWidth(details)) / 2, y + 225);
    }

    private static void drawBreakthroughContent(Graphics2D g2d, int x, int y, int w, int h, Upgrade u, Color theme, boolean hovered) {
        int imgSize = 110; // Shrunk from 140
        int ix = x + (w - imgSize) / 2;
        int iy = y + 40; // Moved up from 60

        BufferedImage icon = ImageManager.get("skill_" + u.name().toLowerCase());
        if (icon != null) {
            g2d.drawImage(icon, ix, iy, imgSize, imgSize, null);
        } else {
            // Rune placeholder
            g2d.setFont(FontManager.getFont(45f));
            g2d.drawString("ᛟ", ix + imgSize/2 - 20, iy + imgSize/2 + 20);
        }

        String fullDesc = u.description;
        String title = fullDesc.contains("(") ? fullDesc.substring(0, fullDesc.indexOf("(")).trim() : fullDesc;
        String details = fullDesc.contains("(") ? fullDesc.substring(fullDesc.indexOf("(")) : "";

        // Title (Centered)
        g2d.setFont(FontManager.getFont(25f));
        g2d.setColor(Color.WHITE);
        FontMetrics fmT = g2d.getFontMetrics();
        g2d.drawString(title, x + (w - fmT.stringWidth(title)) / 2, y + 190);

        // Details (Centered)
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(255, 150, 255));
        FontMetrics fmD = g2d.getFontMetrics();
        g2d.drawString(details, x + (w - fmD.stringWidth(details)) / 2, y + 230);
    }

    private static void drawElegantLevelInfo(Graphics2D g2d, int x, int y, int w, Upgrade u, Player player, Color theme) {
        int curLv = player.getUpgradeLevel(u);
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(255, 255, 255, 180));
        String txt = "Mastery: " + curLv + " / " + u.maxLevel;
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(txt, x + (w - fm.stringWidth(txt)) / 2, y);

        // Mastery dots instead of a bar for fantasy feel
        int dotSpacing = 15;
        int totalW = (u.maxLevel - 1) * dotSpacing;
        int startX = x + (w - totalW) / 2;
        int dotY = y + 20;

        for (int i = 0; i < u.maxLevel; i++) {
            if (i < curLv) {
                g2d.setColor(theme);
                g2d.fillOval(startX + i * dotSpacing - 4, dotY - 4, 8, 8);
                // Tiny glow
                g2d.setColor(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 100));
                g2d.drawOval(startX + i * dotSpacing - 6, dotY - 6, 12, 12);
            } else {
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.drawOval(startX + i * dotSpacing - 3, dotY - 3, 6, 6);
            }
        }
    }
}
