package gameproject.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import gameproject.FontManager;

public class SettingsUI {
    public static void draw(Graphics g, int screenWidth, int screenHeight, boolean showDamageText, boolean pendingReset,
            boolean isAdminMode, boolean showAdminInput, String inputStr) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Nền tối với hiệu ứng mờ nhẹ (vignette giả)
        g2d.setColor(new Color(20, 20, 25));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // Vẽ khung trang trí chính
        int mainW = 600;
        int mainH = 650;
        int mainX = screenWidth / 2 - mainW / 2;
        int mainY = screenHeight / 2 - mainH / 2;
        
        g2d.setColor(new Color(40, 40, 50));
        g2d.fillRoundRect(mainX, mainY, mainW, mainH, 30, 30);
        g2d.setColor(new Color(100, 100, 120));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(mainX, mainY, mainW, mainH, 30, 30);

        // --- Tiêu đề ---
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(45f));
        g2d.drawString("SETTINGS", screenWidth / 2 - 100, mainY + 70);

        // --- SECTION 1: GENERAL ---
        int sec1Y = mainY + 120;
        drawSectionHeader(g2d, "GENERAL", mainX + 50, sec1Y);
        
        int btnW = 400;
        int btnH = 50;
        int btnX = screenWidth / 2 - btnW / 2;
        int btnY = sec1Y + 30;

        g2d.setColor(new Color(60, 60, 75));
        g2d.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
        g2d.setColor(showDamageText ? new Color(0, 255, 150) : new Color(255, 80, 80));
        g2d.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);
        
        g2d.setFont(FontManager.getFont(22f));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Damage Numbers", btnX + 30, btnY + 33);
        g2d.setColor(showDamageText ? Color.GREEN : Color.RED);
        g2d.drawString(showDamageText ? "ENABLED" : "DISABLED", btnX + 270, btnY + 33);

        // --- SECTION 2: ADMIN ---
        int sec2Y = btnY + 90;
        drawSectionHeader(g2d, "ADMIN CONSOLE", mainX + 50, sec2Y);

        if (isAdminMode) {
            int gridX = screenWidth / 2 - 210;
            int gridY = sec2Y + 30;
            int smallBtnW = 200;
            int smallBtnH = 45;

            // Row 1: Gold & Souls
            drawAdminButton(g2d, "+1000 GOLD", gridX, gridY, smallBtnW, smallBtnH, Color.YELLOW);
            drawAdminButton(g2d, "+100 SOULS", gridX + 220, gridY, smallBtnW, smallBtnH, Color.MAGENTA);

            // Row 2: Wave & Level
            drawAdminButton(g2d, "WAVE: " + gameproject.meta.PlayerData.debugStartWave, gridX, gridY + 60, smallBtnW, smallBtnH, Color.CYAN);
            drawAdminButton(g2d, "LEVEL: " + gameproject.meta.PlayerData.debugStartLevel, gridX + 220, gridY + 60, smallBtnW, smallBtnH, Color.ORANGE);
        } else if (!showAdminInput) {
            int aBtnX = screenWidth / 2 - 150;
            int aBtnY = sec2Y + 40;
            g2d.setColor(new Color(30, 80, 100));
            g2d.fillRoundRect(aBtnX, aBtnY, 300, 50, 15, 15);
            g2d.setColor(Color.CYAN);
            g2d.drawRoundRect(aBtnX, aBtnY, 300, 50, 15, 15);
            g2d.setFont(FontManager.getFont(20f));
            g2d.drawString("UNLOCK ADMIN TOOLS", aBtnX + 35, aBtnY + 33);
        } else {
            int boxX = screenWidth / 2 - 150;
            int boxY = sec2Y + 30;
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.getFont(18f));
            g2d.drawString("Enter Admin Key:", boxX, boxY + 15);
            
            g2d.setColor(Color.BLACK);
            g2d.fillRoundRect(boxX, boxY + 25, 300, 45, 10, 10);
            g2d.setColor(Color.CYAN);
            g2d.drawRoundRect(boxX, boxY + 25, 300, 45, 10, 10);

            String masked = "*".repeat(Math.min(inputStr.length(), 10));
            g2d.setFont(FontManager.getFont(24f));
            g2d.drawString(masked, boxX + 10, boxY + 58);
        }

        // --- SECTION 3: DATA ---
        int sec3Y = mainY + 450;
        drawSectionHeader(g2d, "DATA MANAGEMENT", mainX + 50, sec3Y);

        int rBtnX = screenWidth / 2 - 150;
        int rBtnY = sec3Y + 30;
        g2d.setColor(new Color(80, 20, 20));
        g2d.fillRoundRect(rBtnX, rBtnY, 300, 50, 10, 10);
        g2d.setColor(new Color(255, 50, 50));
        g2d.drawRoundRect(rBtnX, rBtnY, 300, 50, 10, 10);
        g2d.setFont(FontManager.getFont(20f));
        g2d.setColor(Color.WHITE);
        g2d.drawString("RESET ALL PROGRESS", rBtnX + 40, rBtnY + 33);
        
        g2d.setFont(FontManager.getFont(14f));
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawString("(Irreversible: Gold, Souls, Stats)", rBtnX + 45, rBtnY + 75);

        // Footer
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString("Press ESC to Save & Return", screenWidth / 2 - 130, mainY + mainH - 30);

        // --- Overlay xác nhận Reset ---
        if (pendingReset) {
            drawResetOverlay(g2d, screenWidth, screenHeight);
        }
    }

    private static void drawSectionHeader(Graphics2D g2d, String text, int x, int y) {
        g2d.setFont(FontManager.getFont(16f));
        g2d.setColor(new Color(180, 180, 200));
        g2d.drawString(text, x, y);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(x, y + 8, x + 500, y + 8);
    }

    private static void drawAdminButton(Graphics2D g2d, String text, int x, int y, int w, int h, Color accent) {
        g2d.setColor(new Color(50, 50, 60));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(accent);
        g2d.drawRoundRect(x, y, w, h, 8, 8);
        g2d.setFont(FontManager.getFont(18f));
        g2d.drawString(text, x + 20, y + 28);
    }

    private static void drawResetOverlay(Graphics2D g2d, int sw, int sh) {
        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRect(0, 0, sw, sh);

        int bw = 500, bh = 250;
        int bx = sw / 2 - bw / 2;
        int by = sh / 2 - bh / 2;

        g2d.setColor(new Color(40, 10, 10));
        g2d.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawRoundRect(bx, by, bw, bh, 20, 20);

        g2d.setFont(FontManager.getFont(30f));
        g2d.drawString("⚠ DANGER ZONE", bx + 120, by + 60);
        
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Are you sure you want to wipe all data?", bx + 80, by + 110);
        g2d.drawString("This cannot be undone.", bx + 150, by + 135);

        // Buttons
        int btnW = 140, btnH = 45;
        g2d.setColor(new Color(150, 30, 30));
        g2d.fillRoundRect(sw / 2 - 160, by + 170, btnW, btnH, 10, 10);
        g2d.setColor(new Color(30, 120, 30));
        g2d.fillRoundRect(sw / 2 + 20, by + 170, btnW, btnH, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(20f));
        g2d.drawString("YES, WIPE", sw / 2 - 142, by + 200);
        g2d.drawString("CANCEL", sw / 2 + 50, by + 200);
    }
}

