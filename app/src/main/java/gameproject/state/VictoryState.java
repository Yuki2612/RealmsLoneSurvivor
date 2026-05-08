package gameproject.state;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import gameproject.FontManager;
import gameproject.GamePanel;
import gameproject.Player;
import gameproject.skill.PassiveSkill;
import gameproject.skill.Upgrade;

public class VictoryState implements State {
    private final int score;
    private final int surviveTime;
    private final String weaponName;
    private final List<String> upgradeLines;
    private final String characterName;
    private long startTime;
    private final long inputDelay = 0; // No cooldown

    public VictoryState(int score, int wave, int surviveTime, String weaponName, Player player, List<PassiveSkill> activeSkills) {
        this.score = score;
        this.surviveTime = surviveTime;
        this.weaponName = weaponName;
        this.characterName = player.getCharClass().name;
        this.upgradeLines = new ArrayList<>();
        this.startTime = gameproject.GamePanel.getTickTime();

        for (Upgrade u : Upgrade.values()) {
            int level = player.getUpgradeLevel(u);
            if (level > 0) {
                String[] parts = u.description.split("\\(");
                upgradeLines.add("• " + parts[0].trim() + "  [Lv." + level + "]");
            }
        }
    }

    @Override
    public void update(GamePanel game) {
        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;
            
            // Check for Menu Button at (50, 50, 160, 50)
            if (mx >= 50 && mx <= 210 && my >= 50 && my <= 100) {
                if (gameproject.GamePanel.getTickTime() - startTime > inputDelay) {
                    game.input.clearClickAndKey();
                    game.changeState(new MenuState());
                    return;
                }
            }
            game.input.clearClickAndKey();
        }
        
        // Block other inputs as requested
        game.input.clearClickAndKey();
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int sw = game.screenWidth;
        int sh = game.screenHeight;
        int cx = sw / 2;
        int cy = sh / 2;

        // --- BACKGROUND ---
        g2d.setColor(new Color(15, 12, 5, 245));
        g2d.fillRect(0, 0, sw, sh);
        
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {new Color(255, 215, 0, 40), new Color(0, 0, 0, 0)};
        RadialGradientPaint p = new RadialGradientPaint(cx, cy, sw / 1.5f, dist, colors);
        g2d.setPaint(p);
        g2d.fillRect(0, 0, sw, sh);

        // --- BACK TO MENU BUTTON (Top Left) ---
        int bX = 50, bY = 50, bW = 160, bH = 50;
        g2d.setColor(new Color(30, 25, 10, 200));
        g2d.fillRoundRect(bX, bY, bW, bH, 15, 15);
        g2d.setColor(new Color(255, 215, 0, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(bX, bY, bW, bH, 15, 15);
        g2d.setFont(FontManager.getFont(20f));
        g2d.setColor(Color.WHITE);
        g2d.drawString("MENU", bX + 48, bY + 33);

        // --- VICTORY TITLE ---
        String title = "YOU SURVIVED";
        g2d.setFont(FontManager.getFont(85f));
        int tw = g2d.getFontMetrics().stringWidth(title);
        g2d.setColor(new Color(255, 200, 0, 100));
        g2d.drawString(title, cx - tw / 2 + 5, cy - 225);
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, cx - tw / 2, cy - 230);

        // --- SUBTITLE ---
        g2d.setFont(FontManager.getFont(24f));
        g2d.setColor(new Color(255, 230, 150));
        String sub = "The Phantom Warlock is defeated. The dark magic fades... for now.";
        int subW = g2d.getFontMetrics().stringWidth(sub);
        g2d.drawString(sub, cx - subW / 2, cy - 180);

        // --- STATS BOX ---
        int boxW = 800, boxH = 350;
        int bx = cx - boxW / 2, by = cy - 140;
        g2d.setColor(new Color(30, 25, 10, 180));
        g2d.fillRoundRect(bx, by, boxW, boxH, 30, 30);
        g2d.setColor(new Color(255, 215, 0, 80));
        g2d.drawRoundRect(bx, by, boxW, boxH, 30, 30);

        // Stats Content
        int drawY = by + 50;
        drawStat(g2d, "Hero:", characterName, bx + 60, drawY, Color.CYAN);
        drawStat(g2d, "Score:", "" + score, bx + 320, drawY, Color.YELLOW);
        drawStat(g2d, "Time:", formatTime(surviveTime), bx + 580, drawY, Color.GREEN);
        drawY += 50;
        drawStat(g2d, "Arsenal:", weaponName, bx + 60, drawY, new Color(255, 100, 100));

        g2d.setColor(new Color(255, 215, 0, 40));
        g2d.drawLine(bx + 50, drawY + 30, bx + boxW - 50, drawY + 30);

        drawY += 70;
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(200, 200, 200));
        int half = (upgradeLines.size() + 1) / 2;
        for (int i = 0; i < upgradeLines.size(); i++) {
            int col = i < half ? 0 : 1;
            int row = i < half ? i : i - half;
            int lx = bx + 70 + col * 360;
            int ly = drawY + row * 25;
            if (ly < by + boxH - 40) g2d.drawString(upgradeLines.get(i), lx, ly);
        }
    }

    private void drawStat(Graphics2D g2d, String label, String value, int x, int y, Color valColor) {
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString(label, x, y);
        g2d.setFont(FontManager.getFont(20f));
        g2d.setColor(valColor);
        g2d.drawString(value, x + g2d.getFontMetrics().stringWidth(label) + 15, y);
    }

    private String formatTime(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
