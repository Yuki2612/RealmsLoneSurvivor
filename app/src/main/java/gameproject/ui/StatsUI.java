package gameproject.ui;

import java.awt.*;
import gameproject.FontManager;
import gameproject.meta.PlayerData;

public class StatsUI {
    public static void draw(Graphics g, int sw, int sh, String[] names, String[] descs, int[] levels, int[] maxLevels, int gold, Object[] nodes, int mouseX, int mouseY) {
        if (nodes == null) return;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark Background with grid
        g2d.setColor(new Color(10, 10, 15));
        g2d.fillRect(0, 0, sw, sh);
        
        g2d.setColor(new Color(20, 20, 30));
        for (int i = 0; i < sw; i += 40) g2d.drawLine(i, 0, i, sh);
        for (int i = 0; i < sh; i += 40) g2d.drawLine(0, i, sw, i);

        // Header
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(45f));
        g2d.drawString("EVOLUTION TREE", sw / 2 - 200, 80);

        // Gold Balance
        int goldX = sw - 280;
        int goldY = 50;
        g2d.setColor(new Color(40, 40, 20));
        g2d.fillRoundRect(goldX, goldY, 230, 50, 20, 20);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(goldX, goldY, 230, 50, 20, 20);
        g2d.setFont(FontManager.getFont(24f));
        g2d.drawString("💰 " + gold, goldX + 30, goldY + 35);

        // Total Upgrades (for cost calculation logic display if needed)
        int totalUpgrades = 0;
        for (int l : levels) totalUpgrades += l;

        // Draw connections first
        g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Object nObj : nodes) {
            // We need to cast back or access fields. Assuming StatsState.StatNode is passed.
            // Since StatNode is inner class, we'll assume the caller passes coordinates.
        }
        // Actually, let's draw connections by index for simplicity if we know the tree
        drawConnection(g2d, nodes, 1, 0, levels); // Might -> HP
        drawConnection(g2d, nodes, 1, 2, levels); // Might -> Speed
        drawConnection(g2d, nodes, 0, 3, levels); // HP -> Dash
        drawConnection(g2d, nodes, 2, 5, levels); // Speed -> Cooldown
        drawConnection(g2d, nodes, 3, 4, levels); // Dash -> Crit
        drawConnection(g2d, nodes, 5, 4, levels); // Cooldown -> Crit

        // Draw Nodes
        for (Object nObj : nodes) {
            drawNode(g2d, nObj, names, descs, levels, maxLevels, gold, totalUpgrades, mouseX, mouseY);
        }

        // Back Button
        int backX = 50, backY = 50;
        g2d.setColor(new Color(40, 40, 50));
        g2d.fillRoundRect(backX, backY, 120, 45, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(backX, backY, 120, 45, 15, 15);
        g2d.setFont(FontManager.getFont(20f));
        g2d.drawString("BACK", backX + 32, backY + 30);
    }

    private static void drawConnection(Graphics2D g2d, Object[] nodes, int fromIdx, int toIdx, int[] levels) {
        // Reflection-less access to fields if possible or just pass coords.
        // For now, let's use a trick to get the node objects.
        // In StatsState, nodes are [Might, HP, Speed, Dash, Cooldown, Crit] based on index 1, 0, 2, 3, 5, 4.
        // Let's find nodes by their statIndex.
        Object fromNode = findNode(nodes, fromIdx);
        Object toNode = findNode(nodes, toIdx);
        if (fromNode == null || toNode == null) return;

        int fx = getX(fromNode), fy = getY(fromNode);
        int tx = getX(toNode), ty = getY(toNode);

        boolean fromUnlocked = levels[fromIdx] > 0;
        boolean toUnlocked = levels[toIdx] > 0;

        if (toUnlocked) {
            g2d.setColor(new Color(255, 255, 0, 180));
        } else if (fromUnlocked) {
            g2d.setColor(new Color(100, 100, 100, 100));
        } else {
            g2d.setColor(new Color(40, 40, 45, 100));
        }
        
        g2d.drawLine(fx, fy, tx, ty);
    }

    private static void drawNode(Graphics2D g2d, Object node, String[] names, String[] descs, int[] levels, int[] maxLevels, int gold, int totalUpgrades, int mx, int my) {
        int idx = getStatIndex(node);
        int cx = getX(node);
        int cy = getY(node);
        int r = 55;
        
        boolean isHovered = Math.pow(mx - cx, 2) + Math.pow(my - cy, 2) <= r * r;
        boolean isUnlocked = levels[idx] > 0;
        boolean canUnlock = checkRequirements(node, levels);
        boolean isMax = levels[idx] >= maxLevels[idx];

        // Outer Glow
        if (isHovered) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillOval(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2);
        }

        // Main Node Circle
        if (!canUnlock) g2d.setColor(new Color(30, 10, 10));
        else if (isMax) g2d.setColor(new Color(10, 40, 10));
        else g2d.setColor(new Color(30, 30, 40));
        
        g2d.fillOval(cx - r, cy - r, r * 2, r * 2);
        
        // Border
        if (!canUnlock) g2d.setColor(new Color(80, 20, 20));
        else if (isMax) g2d.setColor(Color.GREEN);
        else if (isUnlocked) g2d.setColor(Color.YELLOW);
        else g2d.setColor(Color.WHITE);
        
        g2d.setStroke(new BasicStroke(isHovered ? 4 : 2));
        g2d.drawOval(cx - r, cy - r, r * 2, r * 2);

        // Content
        g2d.setFont(FontManager.getFont(14f));
        g2d.setColor(Color.WHITE);
        String name = names[idx];
        g2d.drawString(name, cx - g2d.getFontMetrics().stringWidth(name)/2, cy - 10);
        
        g2d.setFont(FontManager.getFont(12f));
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString("Lv: " + levels[idx] + "/" + maxLevels[idx], cx - 25, cy + 10);

        if (isHovered) {
            drawStatTooltip(g2d, names[idx], descs[idx], levels[idx], maxLevels[idx], gold, totalUpgrades, idx, mx, my);
        }
    }

    private static void drawStatTooltip(Graphics2D g2d, String name, String desc, int lv, int max, int gold, int total, int idx, int mx, int my) {
        int tw = 220, th = 110;
        int tx = mx + 20, ty = my + 20;
        
        g2d.setColor(new Color(20, 20, 30, 230));
        g2d.fillRoundRect(tx, ty, tw, th, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(tx, ty, tw, th, 15, 15);

        g2d.setFont(FontManager.getFont(18f));
        g2d.drawString(name, tx + 15, ty + 30);
        
        g2d.setFont(FontManager.getFont(14f));
        g2d.setColor(new Color(150, 255, 150));
        g2d.drawString(desc, tx + 15, ty + 55);

        if (lv < max) {
            int cost = (int)(getCost(idx, total));
            g2d.setColor(gold >= cost ? Color.GREEN : Color.RED);
            g2d.drawString("Upgrade Cost: " + cost, tx + 15, ty + 85);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("MAX LEVEL REACHED", tx + 15, ty + 85);
        }
    }

    // Helper methods to access Node fields via reflection or assumes structure
    private static int getX(Object node) { try { return node.getClass().getField("cx").getInt(node); } catch(Exception e) {return 0;} }
    private static int getY(Object node) { try { return node.getClass().getField("cy").getInt(node); } catch(Exception e) {return 0;} }
    private static int getStatIndex(Object node) { try { return node.getClass().getField("statIndex").getInt(node); } catch(Exception e) {return 0;} }
    private static Object findNode(Object[] nodes, int idx) {
        for (Object n : nodes) { if (getStatIndex(n) == idx) return n; }
        return null;
    }
    private static boolean checkRequirements(Object node, int[] levels) {
        try {
            int[] reqs = (int[]) node.getClass().getField("required").get(node);
            for (int r : reqs) { if (levels[r] == 0) return false; }
            return true;
        } catch(Exception e) { return true; }
    }
    private static int getCost(int idx, int total) {
        int[] baseCosts = {100, 200, 150, 100, 150, 200};
        return (int)(baseCosts[idx] * Math.pow(1.1, total));
    }
}
