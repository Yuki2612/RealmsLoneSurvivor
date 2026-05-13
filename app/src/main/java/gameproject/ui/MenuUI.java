package gameproject.ui;

import java.awt.*;
import java.awt.geom.Point2D;
import gameproject.FontManager;
import gameproject.GamePanel;

public class MenuUI {
    private static float titleOscillation = 0;
    private static long lastTime = 0;

    public static void draw(Graphics g, int sw, int sh, int mx, int my) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        long currentTime = GamePanel.getTickTime();
        if (lastTime == 0) lastTime = currentTime;
        float deltaTime = (currentTime - lastTime) / 1000f;
        lastTime = currentTime;
        titleOscillation += deltaTime * 2.0f;

        // 1. BACKGROUND
        if (GamePanel.instance.menuParallax != null) {
            GamePanel.instance.menuParallax.draw(g2d);
        } else {
            g2d.setColor(new Color(10, 10, 20));
            g2d.fillRect(0, 0, sw, sh);
        }

        // 2. OVERLAYS
        g2d.setColor(new Color(15, 15, 35, 100)); 
        g2d.fillRect(0, 0, sw, sh);
        RadialGradientPaint vignette = new RadialGradientPaint(
            new Point2D.Float(sw / 2f, sh / 2f), sw * 0.9f,
            new float[]{0.0f, 1.0f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 180)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, sw, sh);

        // 3. THE ANCHOR TITLE (Locked at 100)
        drawAnchorTitle(g2d, 100, 120, titleOscillation);

        // 4. BUTTONS - CLUSTER 1: ACTION (Optical adjustment to 98)
        int actionY = (int)(sh * 0.32f); 
        if (actionY < 260) actionY = 260;
        drawMinimalButton(g2d, 98, actionY, "START GAME", 38f, Color.WHITE, mx, my);

        // 5. BUTTONS - CLUSTER 2: META (Subtle Indent at 125)
        int metaY = actionY + 90; 
        int mSpc = 55; 
        drawMinimalButton(g2d, 125, metaY, "CHARACTER STATS", 18f, new Color(200, 200, 220), mx, my);
        drawMinimalButton(g2d, 125, metaY + mSpc, "SKILLS", 18f, new Color(200, 200, 220), mx, my);
        drawMinimalButton(g2d, 125, metaY + mSpc * 2, "ACHIEVEMENTS", 18f, new Color(200, 200, 220), mx, my);

        // 6. BUTTONS - CLUSTER 3: UTILITY (Aligned with Meta at 125)
        int exitY = sh - 70; 
        int uSpc = 45;
        drawMinimalButton(g2d, 125, exitY - uSpc * 2, "SETTINGS", 18f, new Color(255, 255, 255, 120), mx, my);
        drawMinimalButton(g2d, 125, exitY - uSpc, "SURVIVAL GUIDE", 18f, new Color(255, 255, 255, 120), mx, my);
        drawMinimalButton(g2d, 125, exitY, "EXIT TO DESKTOP", 18f, new Color(255, 255, 255, 120), mx, my);
        
        // 7. VERSION (Bottom-Right)
        g2d.setFont(FontManager.getFont(16f));
        g2d.setColor(new Color(255, 255, 255, 60));
        String ver = "V2.0.0";
        g2d.drawString(ver, sw - g2d.getFontMetrics().stringWidth(ver) - 40, sh - 40);
    }

    private static void drawAnchorTitle(Graphics2D g2d, int x, int y, float osc) {
        float f = (float) Math.sin(osc) * 5f;
        g2d.setFont(FontManager.getFont(120f));
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.drawString("REALMS", x + 4, y + 4 + (int)f);
        g2d.setColor(Color.WHITE);
        g2d.drawString("REALMS", x, y + (int)f);

        g2d.setFont(FontManager.getFont(35f));
        String t2 = "L O N E   S U R V I V O R";
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(t2, x + 2, y + 55 + (int)f);
        g2d.setColor(new Color(218, 165, 32));
        g2d.drawString(t2, x, y + 50 + (int)f);
    }

    private static void drawMinimalButton(Graphics2D g2d, int x, int y, String text, float size, Color color, int mx, int my) {
        g2d.setFont(FontManager.getFont(size));
        FontMetrics fm = g2d.getFontMetrics();
        boolean h = mx >= x && mx <= x + fm.stringWidth(text) && my >= y - fm.getHeight() + 10 && my <= y + 10;
        if (h) {
            g2d.setColor(new Color(255, 220, 100, 60));
            g2d.drawString(text, x - 1, y - 1);
            g2d.drawString(text, x + 1, y + 1);
            g2d.setColor(new Color(255, 230, 120));
            g2d.setFont(FontManager.getFont(size + 4));
            g2d.drawString(">", x - 40, y);
            g2d.setFont(FontManager.getFont(size));
        } else {
            g2d.setColor(color);
        }
        g2d.drawString(text, x, y);
    }
}
