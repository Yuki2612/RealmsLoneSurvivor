package gameproject.ui;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import gameproject.FontManager;
import gameproject.ImageManager;
import gameproject.GamePanel;

public class MenuUI {
    private static float titleOscillation = 0;
    private static long lastTime = 0;

    public static void draw(Graphics g, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        long currentTime = GamePanel.getTickTime();
        if (lastTime == 0) lastTime = currentTime;
        float deltaTime = (currentTime - lastTime) / 1000f;
        lastTime = currentTime;

        titleOscillation += deltaTime * 2.0f;

        // --- Background: Cinematic Deep Space Gradient ---
        Paint oldPaint = g2d.getPaint();
        GradientPaint backgroundGradient = new GradientPaint(
            0, 0, new Color(10, 10, 20),
            0, screenHeight, new Color(25, 20, 45)
        );
        g2d.setPaint(backgroundGradient);
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        g2d.setPaint(oldPaint);

        // --- Background VFX: Particles ---
        drawParticles(g2d, screenWidth, screenHeight, currentTime);

        // --- Ambient Vignette ---
        RadialGradientPaint vignette = new RadialGradientPaint(
            new Point2D.Float(screenWidth / 2f, screenHeight / 2f),
            screenWidth * 0.9f,
            new float[]{0.0f, 1.0f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 220)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        g2d.setPaint(oldPaint);



        // --- ENHANCED GAME TITLE ---
        drawEnhancedTitle(g2d, screenWidth, screenHeight, titleOscillation);

        // --- REPOSITIONED MENU BUTTONS (Pushed up to avoid bottom edge) ---
        int btnW = 320;
        int btnH = 50;
        int btnX = (screenWidth - btnW) / 2;
        int startY = screenHeight / 2 - 80; // Pushed up from -20
        int spacing = 62;

        drawMenuButton(g2d, btnX, startY, btnW, btnH, "START GAME", mouseX, mouseY);
        drawMenuButton(g2d, btnX, startY + spacing, btnW, btnH, "CHARACTER STATS", mouseX, mouseY);
        drawMenuButton(g2d, btnX, startY + spacing * 2, btnW, btnH, "SKILLS", mouseX, mouseY);
        drawMenuButton(g2d, btnX, startY + spacing * 3, btnW, btnH, "ACHIEVEMENTS", mouseX, mouseY);
        drawMenuButton(g2d, btnX, startY + spacing * 4, btnW, btnH, "SETTINGS", mouseX, mouseY);
        drawMenuButton(g2d, btnX, startY + spacing * 5, btnW, btnH, "SURVIVAL GUIDE", mouseX, mouseY);
        drawMenuButton(g2d, btnX, startY + spacing * 6, btnW, btnH, "EXIT TO DESKTOP", mouseX, mouseY);
    }

    private static void drawEnhancedTitle(Graphics2D g2d, int screenWidth, int screenHeight, float osc) {
        String title = "PIXEL SURVIVOR";
        float floatingY = (float) Math.sin(osc) * 10f;
        
        g2d.setFont(FontManager.getFont(100f));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (screenWidth - fm.stringWidth(title)) / 2;
        int ty = screenHeight / 2 - 250 + (int)floatingY;

        // 1. Draw Glow Box (Rectangle matching the frame)
        int glowW = fm.stringWidth(title) + 60;
        int glowH = 110;
        int gx = (screenWidth - glowW) / 2;
        int gy = ty - 85;
        
        // Gradient for glow
        float pulse = (float) (Math.sin(osc * 1.5f) + 1) / 2f;
        Color innerGlow = new Color(100, 160, 255, (int)(40 + pulse * 20));
        
        // Linear Gradient matching the horizontal layout
        GradientPaint rectangleGlow = new GradientPaint(
            gx, gy, new Color(0,0,0,0),
            screenWidth / 2f, gy, innerGlow,
            true
        );
        g2d.setPaint(rectangleGlow);
        g2d.fillRect(gx, gy, glowW, glowH);

        // 2. Decorative Horizontal Lines (The "Frame")
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(gx, gy, gx + glowW, gy); // Top
        g2d.drawLine(gx, gy + glowH, gx + glowW, gy + glowH); // Bottom
        
        // 3. Multi-layer Text
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.drawString(title, tx + 5, ty + 5);

        // Small inner glow pulse
        g2d.setColor(new Color(150, 220, 255, (int)(20 + pulse * 30)));
        g2d.drawString(title, tx - 2, ty - 2);
        g2d.drawString(title, tx + 2, ty + 2);

        GradientPaint textGradient = new GradientPaint(
            tx, ty - 50, Color.WHITE,
            tx, ty, new Color(180, 220, 255)
        );
        g2d.setPaint(textGradient);
        g2d.drawString(title, tx, ty);
        
        g2d.setFont(FontManager.getFont(20f));
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.drawString("v1.2.0", tx + 5, ty + 45);
    }



    private static void drawMenuButton(Graphics2D g2d, int x, int y, int w, int h, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        
        int dx = x;
        int dw = w;
        if (hovered) {
            dx -= 8;
            dw += 16;
            g2d.setColor(new Color(255, 255, 255, 15));
            g2d.fillRect(dx - 4, y - 4, dw + 8, h + 8);
        }

        g2d.setColor(hovered ? new Color(60, 85, 160, 230) : new Color(45, 45, 70, 200));
        g2d.fillRect(dx, y, dw, h);
        
        g2d.setColor(hovered ? Color.WHITE : new Color(130, 160, 255, 180));
        g2d.fillRect(dx, y, 4, h);
        
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.drawRect(dx, y, dw, h);

        g2d.setFont(FontManager.getFont(hovered ? 25f : 23f));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = dx + (dw - fm.stringWidth(text)) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();

        g2d.setColor(hovered ? Color.WHITE : new Color(220, 220, 240));
        g2d.drawString(text, tx, ty);
    }

    private static void drawParticles(Graphics2D g2d, int screenWidth, int screenHeight, long time) {
        g2d.setColor(new Color(255, 255, 255, 25));
        for (int i = 0; i < 35; i++) {
            long seed = i * 98765L;
            float x = (float) ((seed % 1000) / 1000.0 * screenWidth);
            float y = (float) (((seed + time / 35) % 1000) / 1000.0 * screenHeight);
            int size = (int) (seed % 3) + 1;
            g2d.fillOval((int)x, (int)y, size, size);
        }
    }
}
