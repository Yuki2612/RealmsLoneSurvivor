package gameproject.ui;

import java.awt.*;
import java.util.List;
import gameproject.GamePanel;
import gameproject.Player;
import gameproject.FontManager;
import gameproject.skill.Upgrade;
import gameproject.meta.PlayerData;

public class CharacterStatsUI {
    public static void draw(Graphics g, GamePanel game, Player player) {
        Graphics2D g2d = (Graphics2D) g;
        int sw = game.screenWidth;
        int sh = game.screenHeight;

        // 1. Full screen blur/dim overlay
        g2d.setColor(new Color(5, 5, 15, 200));
        g2d.fillRect(0, 0, sw, sh);

        // 2. Main Panel (Wider)
        int panelW = 900;
        int panelH = 600;
        int px = (sw - panelW) / 2;
        int py = (sh - panelH) / 2;

        // Glassmorphism effect
        g2d.setColor(new Color(25, 25, 45, 230));
        g2d.fillRoundRect(px, py, panelW, panelH, 30, 30);
        
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(80, 80, 200, 100));
        g2d.drawRoundRect(px, py, panelW, panelH, 30, 30);

        // Header
        g2d.setFont(FontManager.getFont(42f));
        g2d.setColor(Color.WHITE);
        String title = "CHARACTER STATUS";
        g2d.drawString(title, px + (panelW - g2d.getFontMetrics().stringWidth(title)) / 2, py + 60);

        // Divider
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawLine(px + 40, py + 85, px + panelW - 40, py + 85);

        // Stats Content
        int col1X = px + 80;
        int col2X = px + panelW / 2 + 40;
        int startY = py + 130;
        int spacing = 45;

        g2d.setFont(FontManager.getFont(22f));

        // Column 1: Core Stats
        drawStatRow(g2d, "Level:", "" + game.upgradeManager.playerLevel, col1X, startY);
        drawStatRow(g2d, "EXP:", game.upgradeManager.currentExp + " / " + game.upgradeManager.expToNextLevel, col1X, startY + spacing);
        drawStatRow(g2d, "Health:", player.getHearts() + " / " + player.getMaxHearts() + " Hearts", col1X, startY + spacing * 2);
        drawStatRow(g2d, "Move Speed:", String.format("%.1f", player.getSpeed()), col1X, startY + spacing * 3);
        drawStatRow(g2d, "Dash CD:", (player.getDashCooldown() / 1000f) + "s", col1X, startY + spacing * 4);
        drawStatRow(g2d, "Survive Time:", (game.surviveTimeSeconds / 60) + "m " + (game.surviveTimeSeconds % 60) + "s", col1X, startY + spacing * 5);

        // Column 2: Combat Stats
        int critLevel = player.getUpgradeLevel(Upgrade.CRIT_CHANCE);
        float totalCrit = (PlayerData.statCritLevel * 0.01f) + (critLevel * 0.07f);
        int vampLevel = player.getUpgradeLevel(Upgrade.VAMPIRISM);
        
        drawStatRow(g2d, "Base Damage:", "" + game.upgradeManager.playerDamage, col2X, startY);
        drawStatRow(g2d, "Crit Chance:", String.format("%.0f%%", totalCrit * 100), col2X, startY + spacing);
        drawStatRow(g2d, "Crit Multi:", "150%", col2X, startY + spacing * 2);
        drawStatRow(g2d, "Atk Cooldown:", String.format("%.2fs", game.currentWeapon.cooldown / 1000f), col2X, startY + spacing * 3);
        drawStatRow(g2d, "Weapon Range:", String.format("%.0f px", game.currentWeapon.range), col2X, startY + spacing * 4);
        drawStatRow(g2d, "Vampirism:", vampLevel + "%", col2X, startY + spacing * 5);


        // Skills Section (Bottom)
        int skillY = py + 450;
        g2d.setFont(FontManager.getFont(24f));
        g2d.setColor(new Color(150, 150, 255));
        g2d.drawString("ACTIVE BREAKTHROUGHS", px + 80, skillY);
        
        List<Upgrade> breakthroughs = player.getOwnedBreakthroughs();
        int skillX = px + 80;
        
        if (breakthroughs.isEmpty()) {
            g2d.setFont(FontManager.getFont(18f));
            g2d.setColor(Color.GRAY);
            g2d.drawString("No breakthrough skills acquired yet.", px + 80, skillY + 45);
        } else {
            for (Upgrade u : breakthroughs) {
                // Background for skill box
                g2d.setColor(new Color(60, 60, 100, 150));
                g2d.fillRoundRect(skillX, skillY + 25, 200, 50, 15, 15);
                g2d.setColor(new Color(100, 100, 255));
                g2d.drawRoundRect(skillX, skillY + 25, 200, 50, 15, 15);
                
                g2d.setColor(Color.WHITE);
                g2d.setFont(FontManager.getFont(15f));
                String sName = u.name().replace("_", " ");
                g2d.drawString(sName, skillX + 15, skillY + 55);
                
                g2d.setColor(Color.YELLOW);
                g2d.setFont(FontManager.getFont(14f));
                g2d.drawString("LV." + player.getUpgradeLevel(u), skillX + 155, skillY + 55);
                
                skillX += 215;
                if (skillX + 200 > px + panelW - 40) {
                    skillX = px + 80;
                    skillY += 60;
                }
            }
        }

        // Close Hint
        g2d.setFont(FontManager.getFont(18f));
        g2d.setColor(new Color(255, 255, 255, 120));
        String hint = "Press 'I' to Resume Game";
        g2d.drawString(hint, px + (panelW - g2d.getFontMetrics().stringWidth(hint)) / 2, py + panelH - 30);
    }

    private static void drawStatRow(Graphics2D g2d, String label, String value, int x, int y) {
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString(label, x, y);
        g2d.setColor(Color.WHITE);
        g2d.drawString(value, x + 180, y);
    }
}
