package gameproject.state;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import gameproject.GamePanel;
import gameproject.ImageManager;
import gameproject.FontManager;
import gameproject.meta.Achievement;
import gameproject.meta.AchievementManager;
import gameproject.meta.PlayerData;

public class AchievementState implements State {
    private int currentPage = 0;
    private final int pageSize = 4;
    private List<Achievement> achievementList;
    
    // UI Layout Constants (Optimized to fit within decorative frame)
    private final int listStartY = 140;
    private final int itemHeight = 110;
    private final int itemSpacing = 10;
    private final int cardWidth = 800;

    public AchievementState() {
        achievementList = new ArrayList<>(AchievementManager.getInstance().getAchievements().values());
        // Sorted Sort: Unclaimed (Top) -> Ongoing -> Claimed (Bottom)
        achievementList.sort((a, b) -> {
            if (a.isClaimed != b.isClaimed) return a.isClaimed ? 1 : -1;
            if (a.isCompleted != b.isCompleted) return a.isCompleted ? -1 : 1;
            return a.id.compareTo(b.id);
        });
    }

    @Override
    public void update(GamePanel game) {
        if (game.input.escPressed) {
            game.changeState(new MenuState());
            game.input.clearClickAndKey();
            return;
        }

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            // Back Button (At y=50 as requested previously)
            if (mx >= 40 && mx <= 160 && my >= 50 && my <= 95) {
                game.changeState(new MenuState());
                game.input.clearClickAndKey();
                return;
            }

            // Pagination Arrows (Moved to sides)
            int totalPages = (int) Math.ceil((double) achievementList.size() / pageSize);
            int arrowY = game.screenHeight / 2 - 25;
            
            // Left Arrow (Left side)
            if (currentPage > 0 && mx >= 20 && mx <= 65 && my >= arrowY && my <= arrowY + 45) {
                currentPage--;
                gameproject.SoundManager.play("shoot");
            }
            // Right Arrow (Right side)
            if (currentPage < totalPages - 1 && mx >= game.screenWidth - 65 && mx <= game.screenWidth - 20 && my >= arrowY && my <= arrowY + 45) {
                currentPage++;
                gameproject.SoundManager.play("shoot");
            }

            // Claim Buttons
            int startIdx = currentPage * pageSize;
            int endIdx = Math.min(startIdx + pageSize, achievementList.size());
            int cardX = (game.screenWidth - cardWidth) / 2;

            for (int i = startIdx; i < endIdx; i++) {
                int displayIdx = i - startIdx;
                int itemY = listStartY + displayIdx * (itemHeight + itemSpacing);
                
                Achievement a = achievementList.get(i);
                if (a.isCompleted && !a.isClaimed) {
                    int btnX = cardX + cardWidth - 140;
                    int btnY = itemY + 35;
                    int btnW = 110;
                    int btnH = 50;
                    
                    if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                        AchievementManager.getInstance().claimReward(a.id);
                        gameproject.SoundManager.play("levelup");
                    }
                }
            }
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background Image (Dimmed)
        java.awt.image.BufferedImage bg = ImageManager.get("background1");
        if (bg != null) {
            g2d.drawImage(bg, 0, 0, game.screenWidth, game.screenHeight, null);
            g2d.setColor(new Color(10, 10, 15, 230)); 
            g2d.fillRect(0, 0, game.screenWidth, game.screenHeight);
        } else {
            g2d.setColor(new Color(15, 15, 20));
            g2d.fillRect(0, 0, game.screenWidth, game.screenHeight);
        }
        
        // Ambient Vignette
        RadialGradientPaint vignette = new RadialGradientPaint(
            new Point2D.Double(game.screenWidth / 2.0, game.screenHeight / 2.0),
            (float) (game.screenWidth * 0.9),
            new float[]{0.0f, 1.0f},
            new Color[]{new Color(40, 40, 60, 0), new Color(0, 0, 0, 240)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, game.screenWidth, game.screenHeight);

        // Decorative RPG Frame
        drawRPGFrame(g2d, game.screenWidth, game.screenHeight);

        // Header Title
        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(FontManager.getFont(70f));
        String title = "ACHIEVEMENTS";
        int titleW = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (game.screenWidth - titleW) / 2, 85);
        
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(game.screenWidth / 2 - 200, 105, game.screenWidth / 2 + 200, 105);

        // Back Button
        drawButton(g2d, 40, 50, 120, 45, "BACK", false);

        // Render List
        int startIdx = currentPage * pageSize;
        int endIdx = Math.min(startIdx + pageSize, achievementList.size());
        int cardX = (game.screenWidth - cardWidth) / 2;

        for (int i = startIdx; i < endIdx; i++) {
            Achievement a = achievementList.get(i);
            int displayIdx = i - startIdx;
            int itemY = listStartY + displayIdx * (itemHeight + itemSpacing);

            // Card Shadow
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRoundRect(cardX + 6, itemY + 6, cardWidth, itemHeight, 12, 12);

            // Card Background
            if (a.isClaimed) {
                g2d.setColor(new Color(30, 30, 35, 180));
            } else if (a.isCompleted) {
                g2d.setColor(new Color(45, 65, 45, 240)); 
            } else {
                g2d.setColor(new Color(30, 30, 40, 220));
            }
            g2d.fillRoundRect(cardX, itemY, cardWidth, itemHeight, 12, 12);

            // Card Border
            if (a.isCompleted && !a.isClaimed) {
                g2d.setColor(new Color(255, 215, 0));
                g2d.setStroke(new BasicStroke(3));
            } else {
                g2d.setColor(new Color(70, 70, 90));
                g2d.setStroke(new BasicStroke(1));
            }
            g2d.drawRoundRect(cardX, itemY, cardWidth, itemHeight, 12, 12);

            // Icon Slot
            g2d.setColor(new Color(20, 20, 30));
            g2d.fillOval(cardX + 25, itemY + 15, 80, 80); // Adjusted Y
            g2d.setFont(FontManager.getFont(40f));
            g2d.setColor(a.isCompleted ? new Color(255, 215, 0) : Color.DARK_GRAY);
            String icon = a.isCompleted ? "🏆" : "🔒";
            g2d.drawString(icon, cardX + 40, itemY + 70); // Adjusted Y

            // Text Info
            g2d.setColor(a.isCompleted ? Color.WHITE : new Color(140, 140, 150));
            g2d.setFont(FontManager.getFont(24f));
            g2d.drawString(a.title.toUpperCase(), cardX + 130, itemY + 40); // Adjusted Y

            g2d.setColor(new Color(180, 180, 190));
            g2d.setFont(FontManager.getFont(16f));
            g2d.drawString(a.description, cardX + 130, itemY + 65); // Adjusted Y

            // Progress Bar
            if (!a.isCompleted) {
                int pW = 350;
                int pH = 10;
                int pX = cardX + 130;
                int pY = itemY + 80; // Adjusted Y
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(pX, pY, pW, pH);
                g2d.setColor(new Color(50, 150, 255));
                int fillW = (int) (pW * Math.min(1.0, (double) a.currentValue / a.targetValue));
                g2d.fillRect(pX, pY, fillW, pH);
                
                g2d.setFont(FontManager.getFont(13f));
                g2d.setColor(Color.WHITE);
                g2d.drawString(a.currentValue + " / " + a.targetValue, pX + pW + 15, pY + 11);
            }

            // Reward & Button
            if (a.isClaimed) {
                g2d.setColor(new Color(100, 255, 100, 150));
                g2d.setFont(FontManager.getFont(20f));
                g2d.drawString("COLLECTED", cardX + cardWidth - 170, itemY + 63); // Adjusted Y
            } else if (a.isCompleted) {
                drawButton(g2d, cardX + cardWidth - 145, itemY + 30, 115, 50, "CLAIM", true); // Adjusted Y
                renderRewards(g2d, cardX + cardWidth - 300, itemY + 40, a.goldReward, a.soulReward); // Adjusted Y
            } else {
                renderRewards(g2d, cardX + cardWidth - 160, itemY + 40, a.goldReward, a.soulReward); // Adjusted Y
            }
        }

        // Pagination Info (Centered Bottom)
        int totalPages = (int) Math.ceil((double) achievementList.size() / pageSize);
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(22f));
        String pageTxt = "PAGE " + (currentPage + 1) + " / " + totalPages;
        g2d.drawString(pageTxt, (game.screenWidth - g2d.getFontMetrics().stringWidth(pageTxt)) / 2, game.screenHeight - 50);

        // Arrows (Moved to sides with better design)
        int arrowY = game.screenHeight / 2 - 22;
        drawEnhancedArrow(g2d, 20, arrowY, 45, 45, true, currentPage > 0);
        drawEnhancedArrow(g2d, game.screenWidth - 65, arrowY, 45, 45, false, currentPage < totalPages - 1);
    }

    private void renderRewards(Graphics2D g, int x, int y, int gold, int soul) {
        g.setFont(FontManager.getFont(18f));
        int curY = y;
        if (gold > 0) {
            g.drawImage(ImageManager.get("gold"), x, curY, 26, 26, null);
            g.setColor(new Color(255, 215, 0));
            g.drawString("+" + gold, x + 35, curY + 22);
            curY += 32;
        }
        if (soul > 0) {
            g.drawImage(ImageManager.get("soul"), x, curY, 26, 26, null);
            g.setColor(new Color(100, 200, 255));
            g.drawString("+" + soul, x + 35, curY + 22);
        }
    }

    private void drawButton(Graphics2D g2d, int x, int y, int w, int h, String text, boolean isGold) {
        // Button Shadow
        g2d.setColor(new Color(0,0,0,100));
        g2d.fillRoundRect(x+3, y+3, w, h, 10, 10);

        g2d.setColor(isGold ? new Color(180, 130, 0) : new Color(45, 45, 55));
        g2d.fillRoundRect(x, y, w, h, 10, 10);
        g2d.setColor(isGold ? new Color(255, 215, 0) : new Color(100, 100, 120));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, w, h, 10, 10);
        
        g2d.setFont(FontManager.getFont(20f));
        int tw = g2d.getFontMetrics().stringWidth(text);
        g2d.setColor(isGold ? Color.BLACK : Color.WHITE);
        g2d.drawString(text, x + (w - tw) / 2, y + h / 2 + 8);
    }

    private void drawEnhancedArrow(Graphics2D g2d, int x, int y, int w, int h, boolean isLeft, boolean active) {
        if (!active) {
            g2d.setColor(new Color(60, 60, 70, 100));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(x, y, w, h, 8, 8);
            return;
        }

        // Glow
        g2d.setColor(new Color(255, 215, 0, 40));
        g2d.fillRoundRect(x - 4, y - 4, w + 8, h + 8, 12, 12);

        // Base
        g2d.setColor(new Color(40, 40, 50, 200));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // Arrow head (Pixel style)
        int cx = x + w / 2;
        int cy = y + h / 2;
        int size = 12;
        if (isLeft) {
            g2d.fillPolygon(new int[]{cx - size, cx + size, cx + size}, new int[]{cy, cy - size, cy + size}, 3);
        } else {
            g2d.fillPolygon(new int[]{cx + size, cx - size, cx - size}, new int[]{cy, cy - size, cy + size}, 3);
        }
    }

    private void drawRPGFrame(Graphics2D g, int sw, int sh) {
        g.setColor(new Color(255, 215, 0, 100));
        g.setStroke(new BasicStroke(3));
        
        int margin = 20;
        int cornerSize = 120;
        
        // Top Left Corner
        g.drawLine(margin, margin, margin + cornerSize, margin);
        g.drawLine(margin, margin, margin, margin + cornerSize);
        g.fillOval(margin - 5, margin - 5, 10, 10);
        
        // Top Right Corner
        g.drawLine(sw - margin, margin, sw - margin - cornerSize, margin);
        g.drawLine(sw - margin, margin, sw - margin, margin + cornerSize);
        g.fillOval(sw - margin - 5, margin - 5, 10, 10);
        
        // Bottom Left Corner
        g.drawLine(margin, sh - margin, margin + cornerSize, sh - margin);
        g.drawLine(margin, sh - margin, margin, sh - margin - cornerSize);
        g.fillOval(margin - 5, sh - margin - 5, 10, 10);
        
        // Bottom Right Corner
        g.drawLine(sw - margin, sh - margin, sw - margin - cornerSize, sh - margin);
        g.drawLine(sw - margin, sh - margin, sw - margin, sh - margin - cornerSize);
        g.fillOval(sw - margin - 5, sh - margin - 5, 10, 10);
        
        // Decorative lines
        g.setColor(new Color(255, 215, 0, 30));
        g.drawLine(margin, 120, sw - margin, 120);
        g.drawLine(margin, sh - 100, sw - margin, sh - 100);
    }
}
