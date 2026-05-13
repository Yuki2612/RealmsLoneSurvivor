package gameproject.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import gameproject.FontManager;
import gameproject.ImageManager;
import gameproject.environment.MapConfig;
import gameproject.environment.MapType;

public class MapSelectUI {

    public static void draw(Graphics g, int sw, int sh, MapType[] types, int selectedIndex, int mx, int my) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background (Blurred or Darkened current menu background)
        g2.setColor(new Color(15, 15, 25));
        g2.fillRect(0, 0, sw, sh);

        // Title
        g2.setFont(FontManager.getFont(50f));
        g2.setColor(Color.WHITE);
        String title = "SELECT REALM";
        int titleW = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, sw / 2 - titleW / 2, 80);

        // Map Card
        MapType currentType = types[selectedIndex];
        MapConfig config = MapConfig.getConfig(currentType);

        int cardW = 500;
        int cardH = 350;
        int cardX = sw / 2 - cardW / 2;
        int cardY = sh / 2 - cardH / 2 - 50;

        // Draw Card Shadow/Glow
        g2.setColor(new Color(config.overlayColor.getRed(), config.overlayColor.getGreen(),
                config.overlayColor.getBlue(), 50));
        g2.fillRoundRect(cardX - 10, cardY - 10, cardW + 20, cardH + 20, 30, 30);

        // Main Card Body
        g2.setColor(new Color(30, 30, 45));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g2.setColor(new Color(60, 60, 80));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        // Map Image Placeholder
        g2.setColor(new Color(20, 20, 30));
        g2.fillRect(cardX + 20, cardY + 20, cardW - 40, 200);

        // Map Image Preview (Cover style to avoid stretching)
        BufferedImage thumb = ImageManager.get(config.thumbnailKey);
        if (thumb != null) {
            Shape oldClip = g2.getClip();
            int targetW = cardW - 40;
            int targetH = 200;
            g2.setClip(cardX + 20, cardY + 20, targetW, targetH);
            
            double targetAspect = (double) targetW / targetH;
            double imgAspect = (double) thumb.getWidth() / thumb.getHeight();
            
            int drawW, drawH, offX, offY;
            if (imgAspect > targetAspect) {
                drawH = targetH;
                drawW = (int) (targetH * imgAspect);
                offX = (targetW - drawW) / 2;
                offY = 0;
            } else {
                drawW = targetW;
                drawH = (int) (targetW / imgAspect);
                offX = 0;
                offY = (targetH - drawH) / 2;
            }
            
            g2.drawImage(thumb, cardX + 20 + offX, cardY + 20 + offY, drawW, drawH, null);
            
            g2.setClip(oldClip);
        } else {
            // Fallback to procedural preview or background
            BufferedImage bg = ImageManager.get(config.backgroundKey);
            if (bg != null) {
                Shape oldClip = g2.getClip();
                g2.setClip(cardX + 20, cardY + 20, cardW - 40, 200);
                
                // 1. Draw tiled background
                for (int x = cardX + 20; x < cardX + cardW - 20; x += bg.getWidth() * 2) {
                    for (int y = cardY + 20; y < cardY + 220; y += bg.getHeight() * 2) {
                        g2.drawImage(bg, x, y, bg.getWidth() * 2, bg.getHeight() * 2, null);
                    }
                }
                
                // 2. Draw procedural map elements
                drawPreviewElements(g2, currentType, cardX + 20, cardY + 20, cardW - 40, 200);

                g2.setClip(oldClip);
            }
        }

        // Map Info
        g2.setFont(FontManager.getFont(32f));
        g2.setColor(Color.YELLOW);
        g2.drawString(config.name, cardX + 30, cardY + 250);

        g2.setFont(FontManager.getFont(18f));
        g2.setColor(new Color(200, 200, 200));
        drawWrappedString(g2, config.description, cardX + 30, cardY + 280, cardW - 60);

        // Navigation Arrows
        drawArrow(g2, sw / 2 - 350, sh / 2 - 50, true, mx, my);
        drawArrow(g2, sw / 2 + 350, sh / 2 - 50, false, mx, my);

        // Start Button
        int btnW = 240, btnH = 60;
        int btnX = sw / 2 - btnW / 2;
        int btnY = sh - 150;
        boolean hoverBtn = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;

        g2.setColor(hoverBtn ? new Color(50, 180, 50) : new Color(34, 139, 34));
        g2.fillRoundRect(btnX, btnY, btnW, btnH, 15, 15);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(btnX, btnY, btnW, btnH, 15, 15);

        g2.setFont(FontManager.getFont(24f));
        String btnText = "DEPLOY";
        int btw = g2.getFontMetrics().stringWidth(btnText);
        g2.drawString(btnText, btnX + btnW / 2 - btw / 2, btnY + 40);

        // Controls hint
        g2.setFont(FontManager.getFont(14f));
        g2.setColor(Color.GRAY);
        g2.drawString("ESC TO GO BACK", 30, sh - 30);
    }

    private static void drawPreviewElements(Graphics2D g2, MapType type, int x, int y, int w, int h) {
        int mapId = 0;
        if (type == MapType.SWAMP) mapId = 3;

        if (type == MapType.SWAMP) {
            // Draw some water patches
            g2.setColor(new Color(40, 60, 40, 150));
            g2.fillOval(x + 50, y + 40, 120, 60);
            g2.fillOval(x + 250, y + 100, 100, 50);

            // Draw Altar (Small version)
            BufferedImage altar = ImageManager.get("altar1");
            if (altar != null) {
                g2.drawImage(altar, x + w / 2 - 40, y + h / 2 - 60, 80, 80, null);
            }

            // Draw some swamp trees
            BufferedImage tree = ImageManager.get("tree_" + mapId);
            if (tree != null) {
                g2.drawImage(tree, x + 20, y + 50, 40, 50, null);
                g2.drawImage(tree, x + 380, y + 30, 45, 55, null);
                g2.drawImage(tree, x + 350, y + 120, 40, 50, null);
            }
            
            // Rock
            BufferedImage rock = ImageManager.get("rock_" + mapId);
            if (rock != null) {
                g2.drawImage(rock, x + 100, y + 140, 30, 30, null);
            }
        } else {
            // Default Map
            BufferedImage tree = ImageManager.get("tree_0");
            if (tree == null) tree = ImageManager.get("tree");
            if (tree != null) {
                g2.drawImage(tree, x + 30, y + 40, 50, 60, null);
                g2.drawImage(tree, x + 350, y + 60, 50, 60, null);
            }
            
            BufferedImage rock = ImageManager.get("rock_0");
            if (rock == null) rock = ImageManager.get("rock");
            if (rock != null) {
                g2.drawImage(rock, x + 120, y + 130, 30, 30, null);
                g2.drawImage(rock, x + 280, y + 40, 35, 35, null);
            }

            BufferedImage crate = ImageManager.get("woodencrate_0");
            if (crate == null) crate = ImageManager.get("woodencrate");
            if (crate != null) {
                g2.drawImage(crate, x + 200, y + 100, 25, 25, null);
            }
        }
    }

    private static void drawArrow(Graphics2D g2, int x, int y, boolean left, int mx, int my) {
        boolean hover = Math.pow(mx - x, 2) + Math.pow(my - y, 2) <= 2500;
        g2.setColor(hover ? Color.YELLOW : Color.WHITE);
        g2.setStroke(new BasicStroke(5));
        int size = 30;
        if (left) {
            g2.drawLine(x + size / 2, y - size, x - size / 2, y);
            g2.drawLine(x - size / 2, y, x + size / 2, y + size);
        } else {
            g2.drawLine(x - size / 2, y - size, x + size / 2, y);
            g2.drawLine(x + size / 2, y, x - size / 2, y + size);
        }
    }

    private static void drawWrappedString(Graphics2D g2, String text, int x, int y, int maxWidth) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : words) {
            if (g2.getFontMetrics().stringWidth(line.toString() + word) < maxWidth) {
                line.append(word).append(" ");
            } else {
                g2.drawString(line.toString(), x, currentY);
                line = new StringBuilder(word + " ");
                currentY += g2.getFontMetrics().getHeight();
            }
        }
        g2.drawString(line.toString(), x, currentY);
    }
}
