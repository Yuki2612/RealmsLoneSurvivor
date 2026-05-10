package gameproject.ui;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import gameproject.FontManager;
import gameproject.meta.PlayerData;
import gameproject.state.StatsState;

public class StatsUI {
    private static int lastMouseX, lastMouseY;
    private static boolean isDragging = false;
    private static float colorPhase = 0;
    private static final float SCALE = StatsState.TREE_SCALE; 

    // BAKING SYSTEM
    private static BufferedImage bakedTree = null;
    private static boolean needsRebake = true;
    private static final int BAKE_W = 4000;
    private static final int BAKE_H = 3000;
    private static final int BAKE_OFFSET_X = 2000;
    private static final int BAKE_OFFSET_Y = 2200;

    public static void draw(Graphics g, int sw, int sh, String[] names, String[] descs, int gold, StatsState.StatNode[] nodes, int mouseX, int mouseY, boolean isMouseDown) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        colorPhase += 0.008f; if (colorPhase > 1) colorPhase = 0;

        if (isMouseDown) {
            if (!isDragging) { isDragging = true; lastMouseX = mouseX; lastMouseY = mouseY; }
            else {
                StatsState.offsetX += (mouseX - lastMouseX) / SCALE;
                StatsState.offsetY += (mouseY - lastMouseY) / SCALE;
                lastMouseX = mouseX; lastMouseY = mouseY;
            }
        } else { isDragging = false; }

        // Background
        float[] distArr = {0f, 1f};
        Color[] colors = {new Color(15, 20, 30), new Color(5, 5, 10)};
        g2d.setPaint(new RadialGradientPaint(sw/2, sh/2, sw, distArr, colors));
        g2d.fillRect(0, 0, sw, sh);
        
        if (nodes == null) return;

        // BAKING LOGIC
        if (bakedTree == null || needsRebake) bakeTree(nodes, sw, sh);

        // --- WORLD SPACE (Scaled Tree) ---
        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
        
        g2d.translate(sw/2, sh/2);
        g2d.scale(SCALE, SCALE);
        g2d.translate(-sw/2 + StatsState.offsetX, -sh/2 + StatsState.offsetY);

        // Draw Baked Background (Lines & Labels)
        g2d.drawImage(bakedTree, sw/2 - BAKE_OFFSET_X, sh/2 - BAKE_OFFSET_Y, null);

        // Scaled mouse coords for node interactions
        int adjMx = (int)((mouseX - sw/2) / SCALE + sw/2 - StatsState.offsetX);
        int adjMy = (int)((mouseY - sh/2) / SCALE + sh/2 - StatsState.offsetY);

        // Viewport bounds for Culling
        int viewPadding = 200;
        int vx = (int)((0 - sw/2) / SCALE + sw/2 - StatsState.offsetX) - viewPadding;
        int vy = (int)((0 - sh/2) / SCALE + sh/2 - StatsState.offsetY) - viewPadding;
        int vw = (int)(sw / SCALE) + viewPadding * 2;
        int vh = (int)(sh / SCALE) + viewPadding * 2;
        Rectangle viewport = new Rectangle(vx, vy, vw, vh);

        // Re-draw glowing/hovered elements over baked layer if needed
        // (For now, we just draw nodes on top)
        for (StatsState.StatNode node : nodes) {
            if (viewport.contains(node.cx, node.cy)) {
                if (node.isEvolution) drawElegantEvoNode(g2d, node, names, gold, adjMx, adjMy);
                else drawRPGJewelNode(g2d, node, names, gold, adjMx, adjMy);
            }
        }

        // --- SCREEN SPACE (Fixed HUD) ---
        g2d.setTransform(oldTransform);

        // Draw Tooltips
        for (StatsState.StatNode node : nodes) {
            int r = node.isEvolution ? 70 : (node.isMajor ? 55 : 35);
            if (Math.pow(adjMx - node.cx, 2) + Math.pow(adjMy - node.cy, 2) <= r * r) {
                drawStatTooltip(g2d, node, names, descs, gold, mouseX, mouseY);
                break;
            }
        }

        drawHUD(g2d, sw, sh, gold);
    }

    private static void bakeTree(StatsState.StatNode[] nodes, int sw, int sh) {
        if (bakedTree == null) bakedTree = new BufferedImage(BAKE_W, BAKE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bakedTree.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Clear background
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, BAKE_W, BAKE_H);
        g.setComposite(AlphaComposite.SrcOver);

        // Translate to baking space
        g.translate(BAKE_OFFSET_X - sw/2, BAKE_OFFSET_Y - sh/2);

        // Draw Labels
        drawBranchLabels(g, sw, sh);

        // Draw All Paths
        for (StatsState.StatNode node : nodes) {
            if (node.parentIdx != -1) drawOrganicPath(g, nodes[node.parentIdx], node);
            if (node.isEvolution && node.parentIdx2 != -1) drawOrganicPath(g, nodes[node.parentIdx2], node);
        }

        g.dispose();
        needsRebake = false;
    }

    public static void requestRebake() { needsRebake = true; }

    private static void drawOrganicPath(Graphics2D g2d, StatsState.StatNode p, StatsState.StatNode c) {
        boolean isUnlocked = isNodeUnlocked(c);
        int curLvEvo = c.isEvolution ? StatsState.getEvolutionLevel(c.specialName) : 0;
        Color baseColor = (c.isEvolution && curLvEvo > 0) ? getIridescentColor(0) : c.branchColor;
        
        int ctrlX = (p.cx + c.cx) / 2 + (c.cy - p.cy) / 8;
        int ctrlY = (p.cy + c.cy) / 2 - (c.cx - p.cx) / 8;
        QuadCurve2D q = new QuadCurve2D.Float(p.cx, p.cy, ctrlX, ctrlY, c.cx, c.cy);

        if (isUnlocked || curLvEvo > 0) {
            g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 40));
            g2d.setStroke(new BasicStroke(14)); g2d.draw(q);
            g2d.setColor(baseColor);
            g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g2d.draw(q);
            g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(1.5f)); g2d.draw(q);
        } else {
            g2d.setColor(new Color(255, 255, 255, 50)); // Tăng alpha lên 50
            g2d.setStroke(new BasicStroke(3)); // Tăng stroke lên 3
            g2d.draw(q);
        }
    }

    private static void drawElegantEvoNode(Graphics2D g2d, StatsState.StatNode node, String[] names, int gold, int mx, int my) {
        int r = 70;
        boolean isHovered = Math.pow(mx - node.cx, 2) + Math.pow(my - node.cy, 2) <= r * r;
        int curLv = StatsState.getEvolutionLevel(node.specialName);
        boolean canUnlock = isParentUnlocked(node);
        Color rainbow = getIridescentColor(0);

        g2d.translate(node.cx, node.cy);
        g2d.rotate(Math.toRadians(45));
        
        if (curLv > 0) {
            g2d.setColor(new Color(15, 15, 25, 255));
            g2d.fillRoundRect(-r, -r, r*2, r*2, 25, 25);
            g2d.setColor(new Color(rainbow.getRed(), rainbow.getGreen(), rainbow.getBlue(), 50));
            g2d.fillRoundRect(-r, -r, r*2, r*2, 25, 25);
            g2d.setColor(rainbow);
        } else {
            g2d.setColor(new Color(15, 15, 20, 255));
            g2d.fillRoundRect(-r, -r, r*2, r*2, 25, 25);
            g2d.setColor(canUnlock ? Color.WHITE : new Color(50, 50, 60));
        }
        
        g2d.setStroke(new BasicStroke(isHovered ? 8 : 6));
        g2d.drawRoundRect(-r, -r, r*2, r*2, 25, 25);
        g2d.rotate(Math.toRadians(-45));

        g2d.setFont(FontManager.getFont(14f));
        g2d.setColor(curLv > 0 ? Color.WHITE : Color.GRAY);
        String displayName = node.specialName.toUpperCase();
        g2d.drawString(displayName, -g2d.getFontMetrics().stringWidth(displayName)/2, 5);
        
        g2d.setFont(FontManager.getFont(12f));
        g2d.setColor(curLv > 0 ? rainbow : Color.GRAY);
        String lv = curLv + "/5";
        g2d.drawString(lv, -g2d.getFontMetrics().stringWidth(lv)/2, 28);
        
        g2d.translate(-node.cx, -node.cy);
    }

    private static void drawRPGJewelNode(Graphics2D g2d, StatsState.StatNode node, String[] names, int gold, int mx, int my) {
        int r = node.isMajor ? 55 : 35;
        boolean isHovered = Math.pow(mx - node.cx, 2) + Math.pow(my - node.cy, 2) <= r * r;
        boolean isUnlocked = isNodeUnlocked(node);
        boolean canUnlock = isParentUnlocked(node);

        Polygon shape;
        if (node.cx == StatsState.nodes[0].cx && node.cy == StatsState.nodes[0].cy) shape = createPolygon(node.cx, node.cy, r, 24);
        else if (node.statIndex == 0) shape = createPolygon(node.cx, node.cy, r, 6);
        else if (node.statIndex == 1 || node.statIndex == 4 || node.statIndex == 5) shape = createPolygon(node.cx, node.cy, r, 4);
        else shape = createPolygon(node.cx, node.cy, r, 8);

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.translate(5, 5); g2d.fill(shape); g2d.translate(-5, -5);

        if (isUnlocked) {
            Color c = node.branchColor;
            g2d.setColor(new Color(c.getRed()/4, c.getGreen()/4, c.getBlue()/4, 255));
        } else if (canUnlock) {
            g2d.setColor(new Color(40, 45, 65, 255));
        } else {
            g2d.setColor(new Color(15, 15, 20, 255));
        }
        g2d.fill(shape);

        g2d.setStroke(new BasicStroke(isHovered ? 5 : 3));
        if (isUnlocked) {
            g2d.setColor(node.branchColor);
            g2d.draw(shape);
        } else if (canUnlock) {
            g2d.setColor(Color.WHITE);
            g2d.draw(shape);
        } else {
            g2d.setColor(new Color(60, 60, 80));
            g2d.draw(shape);
        }

        g2d.setColor(isUnlocked ? Color.WHITE : Color.GRAY);
        g2d.setFont(FontManager.getFont(node.isMajor ? 14f : 10f));
        String label = names[node.statIndex].toUpperCase();
        g2d.drawString(label, node.cx - g2d.getFontMetrics().stringWidth(label)/2, node.cy + 5);
    }

    private static Color getIridescentColor(float offset) {
        float h = (colorPhase + offset) % 1.0f;
        return Color.getHSBColor(h, 0.7f, 1.0f);
    }

    private static Polygon createPolygon(int x, int y, int r, int sides) {
        Polygon p = new Polygon();
        for (int i = 0; i < sides; i++) {
            double angle = i * 2 * Math.PI / sides - Math.PI/2;
            p.addPoint((int)(x + r * Math.cos(angle)), (int)(y + r * Math.sin(angle)));
        }
        return p;
    }

    private static void drawBranchLabels(Graphics2D g2d, int sw, int sh) {
        int cx = sw / 2;
        int cy = sh / 2 + 300;
        g2d.setFont(FontManager.getFont(32f));
        // ADJUSTED VITALITY: moved even further down and left
        g2d.setColor(new Color(50, 255, 100, 150)); g2d.drawString("VITALITY", cx - 600, cy - 1400);
        g2d.setColor(new Color(255, 50, 80, 150)); g2d.drawString("DAMAGE", cx + 1200, cy - 500);
        g2d.setColor(new Color(255, 220, 50, 150)); g2d.drawString("MOBILITY", cx - 1400, cy - 500);
    }

    private static void drawStatTooltip(Graphics2D g2d, StatsState.StatNode node, String[] names, String[] descs, int gold, int mx, int my) {
        int tw = 280, th = 170;
        int tx = mx + 20, ty = my + 20;
        if (tx + tw > 1920) tx = mx - tw - 20;
        if (ty + th > 1080) ty = my - th - 20;

        g2d.setColor(new Color(10, 12, 25, 252)); g2d.fillRoundRect(tx, ty, tw, th, 15, 15);
        int evoLv = node.isEvolution ? StatsState.getEvolutionLevel(node.specialName) : 0;
        Color borderColor = (node.isEvolution && evoLv > 0) ? getIridescentColor(0) : node.branchColor;
        g2d.setColor(borderColor); g2d.setStroke(new BasicStroke(3)); g2d.drawRoundRect(tx, ty, tw, th, 15, 15);
        
        g2d.setColor(Color.WHITE);
        if (node.isEvolution) {
            int curLv = StatsState.getEvolutionLevel(node.specialName);
            String displayName = node.specialName.toUpperCase();
            g2d.setFont(FontManager.getFont(24f)); g2d.drawString(displayName, tx + 20, ty + 45);
            g2d.setFont(FontManager.getFont(16f)); g2d.setColor(curLv > 0 ? borderColor : Color.GRAY); g2d.drawString("Evolution Tier: " + curLv + "/5", tx + 20, ty + 70);
            g2d.setFont(FontManager.getFont(14f)); g2d.setColor(new Color(210, 210, 240));
            String effect = "";
            if (node.specialName.equals("Vampiric")) effect = "Life on kill: " + String.format("%.1f", curLv * 0.2f) + "%";
            else if (node.specialName.equals("Deadly Focus") || node.specialName.equals("Bloodlust")) effect = "Crit Multiplier: +" + String.format("%.1f", curLv * 0.1f) + "x";
            else if (node.specialName.equals("Phantom Dash")) effect = "Explosive illusion power UP";
            else if (node.specialName.equals("Adrenaline")) effect = "Speed when damaged: +" + (curLv * 7) + "%";
            else if (node.specialName.equals("Frenzy")) effect = "On Crit: +" + (curLv * 5) + "% Fire Rate (2s)";
            g2d.drawString(effect, tx + 20, ty + 95);
            
            if (curLv < 5 && isParentUnlocked(node)) {
                int cost = StatsState.calculateEvoCost(node.specialName, curLv);
                g2d.setColor(PlayerData.soulStones >= cost ? Color.CYAN : Color.RED);
                g2d.drawString("• NEXT TIER: " + cost + " SOULS", tx + 20, ty + 130);
            } else if (curLv >= 5) { g2d.setColor(Color.GREEN); g2d.drawString("• SUPREME TIER ACHIEVED", tx + 20, ty + 130); }
        } else {
            g2d.setFont(FontManager.getFont(22f)); g2d.drawString(names[node.statIndex].toUpperCase(), tx + 20, ty + 40);
            g2d.setFont(FontManager.getFont(14f)); g2d.setColor(new Color(200, 200, 230));
            String actualBonus = "";
            switch(node.statIndex) {
                case 0 -> actualBonus = "+" + (node.bonusAmount / 10) + " HEART";
                case 1 -> actualBonus = "+" + node.bonusAmount + " DAMAGE";
                case 2 -> actualBonus = "+" + (node.bonusAmount * 2) + "% SPEED";
                case 3 -> actualBonus = "-" + (node.bonusAmount * 2) + "% DASH CD";
                case 4 -> actualBonus = "+" + node.bonusAmount + "% CRIT CHANCE";
                case 5 -> actualBonus = "-" + node.bonusAmount + "% ATK COOLDOWN";
            }
            g2d.drawString(actualBonus, tx + 20, ty + 65);
            if (isNodeUnlocked(node)) { g2d.setColor(Color.GREEN); g2d.drawString("• MAXED", tx + 20, ty + 125); }
            else if (isParentUnlocked(node)) {
                int currentLv = getPlayerDataLevel(node.statIndex);
                if (currentLv == node.levelTarget - node.bonusAmount) {
                    int cost = calculateNodeCost(node, currentLv);
                    g2d.setColor(gold >= cost ? Color.YELLOW : Color.RED);
                    g2d.drawString("• COST: " + cost + " GOLD", tx + 20, ty + 125);
                } else { g2d.setColor(Color.GRAY); g2d.drawString("• STATUS: LOCKED", tx + 20, ty + 125); }
            } else { g2d.setColor(Color.GRAY); g2d.drawString("• STATUS: PATH LOCKED", tx + 20, ty + 125); }
        }
    }

    private static int calculateNodeCost(StatsState.StatNode node, int currentLv) {
        if (node.statIndex == 0) {
            if (node.levelTarget == 10) return 3000;
            if (node.levelTarget == 20) return 7000;
            if (node.levelTarget == 30) return 12000;
        }
        int[] bases = {110, 320, 160, 130, 240, 280};
        int sum = 0;
        for (int i = 0; i < node.bonusAmount; i++) sum += (int)(bases[node.statIndex] * Math.pow(1.14, currentLv + i));
        return sum;
    }

    private static void drawHUD(Graphics2D g2d, int sw, int sh, int gold) {
        g2d.setStroke(new BasicStroke(2));
        int rx = sw - 300, ry = 50, rw = 250, rh = 80;
        g2d.setColor(new Color(20, 30, 50, 245)); g2d.fillRoundRect(rx, ry, rw, rh, 20, 20);
        g2d.setColor(new Color(100, 100, 160)); g2d.drawRoundRect(rx, ry, rw, rh, 20, 20);
        java.awt.image.BufferedImage goldImg = gameproject.ImageManager.get("gold");
        java.awt.image.BufferedImage soulImg = gameproject.ImageManager.get("soul");
        g2d.setFont(FontManager.getFont(20f));
        if (goldImg != null) { g2d.drawImage(goldImg, rx + 20, ry + 12, 24, 24, null); g2d.setColor(Color.YELLOW); g2d.drawString("" + gold, rx + 55, ry + 32); }
        if (soulImg != null) { g2d.drawImage(soulImg, rx + 20, ry + 45, 24, 24, null); g2d.setColor(Color.CYAN); g2d.drawString("" + PlayerData.soulStones, rx + 55, ry + 65); }
        g2d.setColor(Color.WHITE); g2d.setFont(FontManager.getFont(45f)); g2d.drawString("EVOLUTION TREE", 60, 80);
        int bx = 50, by = sh - 80;
        g2d.setColor(new Color(40, 50, 80)); g2d.fillRoundRect(bx, by, 140, 45, 15, 15);
        g2d.setColor(Color.WHITE); g2d.drawRoundRect(bx, by, 140, 45, 15, 15);
        g2d.setFont(FontManager.getFont(20f)); g2d.drawString("BACK", bx + 42, by + 30);
    }

    private static boolean isNodeUnlocked(StatsState.StatNode node) { 
        if (node.isEvolution) return StatsState.getEvolutionLevel(node.specialName) >= 5;
        return getPlayerDataLevel(node.statIndex) >= node.levelTarget; 
    }
    private static boolean isParentUnlocked(StatsState.StatNode node) { 
        if (node.parentIdx == -1) return true; 
        boolean p1 = isNodeUnlocked(StatsState.nodes[node.parentIdx]);
        if (node.isEvolution && node.parentIdx2 != -1) return p1 && isNodeUnlocked(StatsState.nodes[node.parentIdx2]);
        return p1;
    }
    private static int getPlayerDataLevel(int idx) {
        if (idx < 0) return 0; // Xử lý ô Tâm trung lập
        if (idx == 0) return PlayerData.statHealthLevel;
        if (idx == 1) return PlayerData.statDamageLevel;
        if (idx == 2) return PlayerData.statSpeedLevel;
        if (idx == 3) return PlayerData.statDashLevel;
        if (idx == 4) return PlayerData.statCritLevel;
        if (idx == 5) return PlayerData.statCooldownLevel;
        return 0;
    }
}
