package gameproject.ui;

import java.awt.*;
import gameproject.FontManager;
import gameproject.weapon.Weapon;
import gameproject.ImageManager;

public class WeaponSelectUI {
    public static void draw(Graphics g, int sw, int sh, Weapon[] options, int mx, int my) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark Overlay with slight tint
        g2d.setColor(new Color(10, 10, 15, 235));
        g2d.fillRect(0, 0, sw, sh);

        // Ambient Glow in the background
        g2d.setPaint(new RadialGradientPaint(sw / 2, sh / 2, sw / 2, new float[]{0f, 1f}, new Color[]{new Color(50, 40, 20, 40), new Color(0, 0, 0, 0)}));
        g2d.fillRect(0, 0, sw, sh);

        // Header Title
        g2d.setFont(FontManager.getFont(50f));
        String title = "NEW ARMAMENT";
        int tw = g2d.getFontMetrics().stringWidth(title);
        
        // Title Shadow/Glow
        g2d.setColor(new Color(255, 200, 50, 100));
        g2d.drawString(title, sw / 2 - tw / 2 + 3, 83);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, sw / 2 - tw / 2, 80);

        g2d.setFont(FontManager.getFont(20f));
        g2d.setColor(new Color(200, 200, 200));
        String subtitle = "Choose your next tool of destruction";
        g2d.drawString(subtitle, sw / 2 - g2d.getFontMetrics().stringWidth(subtitle) / 2, 120);

        int cardW = 320, cardH = 460, spacing = 60;
        int totalW = (3 * cardW + 2 * spacing);
        int startX = (sw - totalW) / 2;
        int startY = (sh - cardH) / 2 + 40;

        for (int i = 0; i < 3; i++) {
            int bx = startX + i * (cardW + spacing);
            int by = startY;

            boolean isHovered = mx >= bx && mx <= bx + cardW && my >= by && my <= by + cardH;
            
            // Hover Animation Offset
            int hoverOffset = isHovered ? -10 : 0;
            Rectangle cardRect = new Rectangle(bx, by + hoverOffset, cardW, cardH);

            // Card Shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRoundRect(cardRect.x + 10, cardRect.y + 10, cardRect.width, cardRect.height, 25, 25);

            // Card Background (Glassmorphism + Gradient)
            GradientPaint cardBg = isHovered 
                ? new GradientPaint(cardRect.x, cardRect.y, new Color(55, 55, 75, 250), cardRect.x, cardRect.y + cardRect.height, new Color(25, 25, 35, 250))
                : new GradientPaint(cardRect.x, cardRect.y, new Color(35, 35, 45, 230), cardRect.x, cardRect.y + cardRect.height, new Color(15, 15, 25, 230));
            g2d.setPaint(cardBg);
            g2d.fillRoundRect(cardRect.x, cardRect.y, cardRect.width, cardRect.height, 25, 25);

            // Tech Grid Lines background
            g2d.setColor(new Color(255, 255, 255, 5));
            for(int j=20; j<cardRect.height; j+=20) {
                g2d.drawLine(cardRect.x, cardRect.y + j, cardRect.x + cardRect.width, cardRect.y + j);
            }
            for(int j=20; j<cardRect.width; j+=20) {
                g2d.drawLine(cardRect.x + j, cardRect.y, cardRect.x + j, cardRect.y + cardRect.height);
            }

            // Border
            if (isHovered) {
                g2d.setStroke(new BasicStroke(3));
                g2d.setColor(new Color(255, 215, 0)); // Gold glow
            } else {
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(new Color(80, 80, 100));
            }
            g2d.drawRoundRect(cardRect.x, cardRect.y, cardRect.width, cardRect.height, 25, 25);

            // Image Holder Section
            int imgSize = 130;
            int imgX = cardRect.x + cardW / 2 - imgSize / 2;
            int imgY = cardRect.y + 40;

            // Image Circle Backlight (Brighter on hover)
            Color glowColor = isHovered ? new Color(100, 200, 255, 60) : new Color(100, 100, 255, 30);
            g2d.setPaint(new RadialGradientPaint(imgX + imgSize/2, imgY + imgSize/2, imgSize/2 + 20, new float[]{0f, 1f}, new Color[]{glowColor, new Color(0, 0, 0, 0)}));
            g2d.fillOval(imgX - 10, imgY - 10, imgSize + 20, imgSize + 20);
            
            // Outer Circle
            g2d.setStroke(new BasicStroke(isHovered ? 3 : 2));
            g2d.setColor(isHovered ? new Color(150, 200, 255) : new Color(60, 60, 80));
            g2d.drawOval(imgX, imgY, imgSize, imgSize);

            // Draw actual weapon image with ASPECT RATIO preserved
            String weaponKey = "";
            if (options[i] instanceof gameproject.weapon.Shotgun) weaponKey = "shotgun";
            else if (options[i] instanceof gameproject.weapon.SniperRifle) weaponKey = "sniper_rifle";
            else if (options[i] instanceof gameproject.weapon.AssaultRifle) weaponKey = "assault_rifle";
            
            java.awt.image.BufferedImage wImg = gameproject.ImageManager.get(weaponKey);
            if (wImg != null) {
                int imgW = wImg.getWidth();
                int imgH = wImg.getHeight();
                int maxRenderSize = 100; // Phóng to ảnh lên một chút
                
                int drawW = maxRenderSize;
                int drawH = maxRenderSize;
                
                // Tính toán tỷ lệ (Aspect Ratio)
                if (imgW > imgH) {
                    drawH = (int) ((float) imgH / imgW * maxRenderSize);
                } else {
                    drawW = (int) ((float) imgW / imgH * maxRenderSize);
                }
                
                int rx = imgX + (imgSize - drawW) / 2;
                int ry = imgY + (imgSize - drawH) / 2;
                g2d.drawImage(wImg, rx, ry, drawW, drawH, null);
            } else {
                g2d.setFont(FontManager.getFont(12f));
                g2d.setColor(new Color(80, 80, 100));
                g2d.drawString("[ WEAPON ]", imgX + 25, imgY + imgSize / 2 + 5);
            }

            // Weapon Name
            g2d.setFont(FontManager.getFont(32f));
            g2d.setColor(isHovered ? Color.YELLOW : Color.CYAN);
            String name = options[i].name.toUpperCase();
            g2d.drawString(name, cardRect.x + cardW / 2 - g2d.getFontMetrics().stringWidth(name) / 2, imgY + imgSize + 55);

            // Subtle divider line
            g2d.setColor(new Color(255, 255, 255, 20));
            g2d.drawLine(cardRect.x + 30, imgY + imgSize + 75, cardRect.x + cardW - 30, imgY + imgSize + 75);

            // Stats Section
            int statsY = imgY + imgSize + 110;
            g2d.setFont(FontManager.getFont(18f));
            g2d.setColor(Color.WHITE);
            
            drawStatLine(g2d, "POWER", "x" + options[i].damageMultiplier, cardRect.x + 40, statsY, Color.RED);
            drawStatLine(g2d, "TEMPO", options[i].cooldown + "ms", cardRect.x + 40, statsY + 40, Color.GREEN);
            drawStatLine(g2d, "RANGE", "" + (int)options[i].range, cardRect.x + 40, statsY + 80, Color.CYAN);

            // Select Prompt
            if (isHovered) {
                g2d.setFont(FontManager.getFont(22f));
                g2d.setColor(Color.WHITE);
                String prompt = "PRESS TO EQUIP";
                g2d.drawString(prompt, cardRect.x + cardW / 2 - g2d.getFontMetrics().stringWidth(prompt) / 2, cardRect.y + cardH - 35);
                
                // Pulsing dot or underline
                g2d.fillRect(cardRect.x + 80, cardRect.y + cardH - 25, cardW - 160, 2);
            } else {
                g2d.setFont(FontManager.getFont(18f));
                g2d.setColor(new Color(150, 150, 150));
                String prompt = "SELECT";
                g2d.drawString(prompt, cardRect.x + cardW / 2 - g2d.getFontMetrics().stringWidth(prompt) / 2, cardRect.y + cardH - 35);
            }
        }
    }

    private static void drawStatLine(Graphics2D g2d, String label, String value, int x, int y, Color valColor) {
        g2d.setFont(FontManager.getFont(14f));
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawString(label, x, y);
        
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(valColor);
        g2d.drawString(value, x + 120, y);
    }
}
