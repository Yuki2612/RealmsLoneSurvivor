package gameproject.ui;

import java.awt.*;
import gameproject.FontManager;

public class PauseGameOverUI {
    public static void drawPaused(Graphics g, int sw, int sh) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- BACKGROUND OVERLAY ---
        g2d.setColor(new Color(10, 10, 15, 200));
        g2d.fillRect(0, 0, sw, sh);

        // Radial Glow
        int cx = sw / 2;
        int cy = sh / 2;
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {new Color(0, 150, 255, 30), new Color(0, 0, 0, 0)};
        RadialGradientPaint p = new RadialGradientPaint(cx, cy, sw / 1.5f, dist, colors);
        g2d.setPaint(p);
        g2d.fillRect(0, 0, sw, sh);

        // --- TITLE ---
        g2d.setFont(FontManager.getFont(75f));
        String title = "PAUSED";
        int tw = g2d.getFontMetrics().stringWidth(title);
        
        g2d.setColor(new Color(0, 180, 255, 100));
        g2d.drawString(title, cx - tw / 2 + 5, cy - 185);
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, cx - tw / 2, cy - 190);

        // --- BUTTONS SETUP ---
        int btnW = 320, btnH = 65;
        int btnX = cx - btnW / 2;
        int startY = cy - 60;
        int spacing = 25;

        // 1. RESUME
        drawMenuButton(g2d, "RESUME", btnX, startY, btnW, btnH, new Color(0, 255, 180));
        
        // 2. MENU
        drawMenuButton(g2d, "MAIN MENU", btnX, startY + (btnH + spacing), btnW, btnH, new Color(0, 180, 255));
        
        // 3. QUIT
        drawMenuButton(g2d, "EXIT GAME", btnX, startY + (btnH + spacing) * 2, btnW, btnH, new Color(255, 80, 80));

        // Instructions
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(180, 180, 180));
        String hint = "Press 'ESC' to Resume";
        g2d.drawString(hint, cx - g2d.getFontMetrics().stringWidth(hint) / 2, sh - 80);
    }

    private static void drawMenuButton(Graphics2D g2d, String text, int x, int y, int w, int h, Color accent) {
        // Shadow/Glow
        g2d.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20));
        g2d.fillRoundRect(x - 4, y - 4, w + 8, h + 8, 20, 20);

        // Main Body
        g2d.setColor(new Color(25, 25, 35, 230));
        g2d.fillRoundRect(x, y, w, h, 15, 15);

        // Border
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2d.drawRoundRect(x, y, w, h, 15, 15);

        // Text
        g2d.setFont(FontManager.getFont(24f));
        g2d.setColor(Color.WHITE);
        int tw = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, x + (w - tw) / 2, y + h / 2 + 10);
        
        // Accent Dot
        g2d.setColor(accent);
        g2d.fillOval(x + 20, y + h / 2 - 4, 8, 8);
    }

    public static void drawGameOver(Graphics g, int screenWidth, int screenHeight) {
        // Keep existing if preferred, or update to match. 
        // User said "Pause lech tong", implying GameOver is already good.
        // Let's keep it as is since GameOverState handles its own premium rendering now.
    }
}
