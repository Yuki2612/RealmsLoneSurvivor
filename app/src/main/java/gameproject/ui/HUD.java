package gameproject.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import gameproject.FontManager;
import gameproject.ImageManager;
import gameproject.Player;
import gameproject.entity.Enemy;
import gameproject.meta.PlayerData;
import gameproject.environment.*;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;

public class HUD {
    public static void draw(Graphics g, gameproject.GamePanel game, Player player, ArrayList<Enemy> enemies) {
        int screenWidth = game.screenWidth;
        int screenHeight = game.screenHeight;
        int score = game.score;
        int waveCount = game.entityManager.waveCount;
        int playerDamage = game.upgradeManager.playerDamage;
        long actualCooldown = game.currentWeapon.getActualCooldown(player.getComboManager().getFireRateBonus());
        float shotsPerSec = 1000.0f / actualCooldown;
        int dps = (int) (playerDamage * game.currentWeapon.damageMultiplier * game.currentWeapon.getProjectilesPerShot()
                * shotsPerSec);
        int currentExp = game.upgradeManager.currentExp;
        int expToNextLevel = game.upgradeManager.expToNextLevel;
        int playerLevel = game.upgradeManager.playerLevel;

        g.setFont(FontManager.getFont(20f));

        // Tọa độ các thành phần
        int scoreX = 15;
        int waveX = 200;
        int goldX = 30;
        int goldY = screenHeight - 95;
        int soulX = 180;
        int fpsX = screenWidth - 250; // Chuyển FPS sang bên trái Minimap (150px) để không bị đè

        java.awt.image.BufferedImage goldImg = ImageManager.get("gold");
        java.awt.image.BufferedImage soulImg = ImageManager.get("soul");

        // --- BƯỚC 1: VẼ ĐỔ BÓNG (SHADOW) ---
        g.setColor(Color.BLACK);
        g.drawString("Score: " + score, scoreX, 35);
        g.drawString("Wave: " + waveCount, waveX, 35);
        g.drawString("FPS: " + game.currentFPS, fpsX, 35);

        if (goldImg != null) {
            g.drawImage(goldImg, goldX, goldY - 20, 24, 24, null);
            g.drawString("" + PlayerData.gold, goldX + 30, goldY);
        } else {
            g.drawString("G: " + PlayerData.gold, goldX, goldY);
        }

        if (soulImg != null) {
            g.drawImage(soulImg, soulX, goldY - 20, 24, 24, null);
            g.drawString("" + PlayerData.soulStones, soulX + 30, goldY);
        } else {
            g.drawString("S: " + PlayerData.soulStones, soulX, goldY);
        }

        g.drawString("HP:", 15, 112);
        g.drawString("Dash:", 15, 155);

        // --- BƯỚC 2: VẼ CHỮ CHÍNH (MAIN COLOR) ---
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, scoreX - 2, 33);
        g.drawString("Wave: " + waveCount, waveX - 2, 33);
        g.drawString("FPS: " + game.currentFPS, fpsX - 2, 33);
        g.drawString("HP:", 13, 110);
        g.drawString("Dash:", 13, 153);

        if (goldImg != null) {
            g.drawImage(goldImg, goldX, goldY - 20, 24, 24, null);
            g.setColor(Color.YELLOW);
            g.drawString("" + PlayerData.gold, goldX - 2 + 30, goldY - 2);
        } else {
            g.setColor(Color.YELLOW);
            g.drawString("G: " + PlayerData.gold, goldX - 2, goldY - 2);
        }

        if (soulImg != null) {
            g.drawImage(soulImg, soulX, goldY - 20, 24, 24, null);
            g.setColor(Color.CYAN);
            g.drawString("" + PlayerData.soulStones, soulX - 2 + 30, goldY - 2);
        } else {
            g.setColor(Color.CYAN);
            g.drawString("S: " + PlayerData.soulStones, soulX - 2, goldY - 2);
        }

        // Trái tim
        java.awt.image.BufferedImage heartImg = ImageManager.get("heart");
        for (int i = 0; i < player.getHearts(); i++) {
            int hX = 70 + (i * 30);
            int hY = 90;
            if (heartImg != null)
                g.drawImage(heartImg, hX, hY, 26, 26, null);
            else {
                g.setColor(Color.RED);
                g.fillRect(hX, hY, 20, 20);
            }
        }

        // Trạng thái Dash
        long timeSinceLastDash = gameproject.GamePanel.getTickTime() - player.getLastDashTime();
        if (timeSinceLastDash >= player.getDashCooldown()) {
            g.setColor(Color.BLACK);
            g.drawString("READY", 102, 155);
            g.setColor(Color.GREEN);
            g.drawString("READY", 100, 153);
        } else {
            g.setColor(Color.BLACK);
            g.drawString("WAIT", 102, 155);
            g.setColor(Color.GRAY);
            g.drawString("WAIT", 100, 153);
        }

        // Thanh EXP
        int barHeight = 26;
        int barWidth = screenWidth - 60;
        int barX = 30;
        int barY = screenHeight - 60;

        g.setColor(Color.BLACK);
        g.fillRect(barX - 2, barY - 2, barWidth + 4, barHeight + 4);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(new Color(0, 200, 255));
        int currentExpWidth = (int) (((float) currentExp / expToNextLevel) * barWidth);
        g.fillRect(barX, barY, currentExpWidth, barHeight);

        // Chữ Level
        g.setFont(FontManager.getFont(18f));
        String expText = "Lv." + playerLevel + "  [" + currentExp + " / " + expToNextLevel + " EXP]";
        g.setColor(Color.BLACK);
        g.drawString(expText, screenWidth / 2 - 140 + 2, barY + 20 + 2);
        g.setColor(Color.WHITE);
        g.drawString(expText, screenWidth / 2 - 140, barY + 20);

        // Boss HP bar - HOÀN TRÀ PHONG CÁCH ĐƠN GIẢN (SIMPLE STYLE)
        synchronized (enemies) {
            for (Enemy e : enemies) {
                if (e.isBoss && !e.isDying) {
                    int bBarW = 600;
                    int bBarH = 20; 
                    int bBarX = screenWidth / 2 - bBarW / 2;
                    int bBarY = 25;

                    // 1. NỀN THANH MÁU
                    g.setColor(new Color(0, 0, 0, 180));
                    g.fillRoundRect(bBarX - 4, bBarY - 4, bBarW + 8, bBarH + 8, 10, 10);
                    g.setColor(new Color(80, 0, 0));
                    g.fillRoundRect(bBarX, bBarY, bBarW, bBarH, 6, 6);

                    // 2. PHẦN MÁU HIỆN TẠI
                    int hpW = (int) ((float) e.getHp() / e.getMaxHp() * bBarW);
                    if (hpW > 0) {
                        g.setColor(new Color(220, 40, 40));
                        g.fillRoundRect(bBarX, bBarY, hpW, bBarH, 6, 6);
                        
                        // Highlight đơn giản
                        g.setColor(new Color(255, 255, 255, 40));
                        g.fillRect(bBarX, bBarY, hpW, bBarH / 2);
                    }

                    // 3. TÊN BOSS
                    String bossName = e.getName();
                    g.setFont(FontManager.getFont(22f));
                    int nameW = g.getFontMetrics().stringWidth(bossName);
                    g.setColor(Color.BLACK);
                    g.drawString(bossName, screenWidth / 2 - nameW / 2 + 2, bBarY + bBarH + 26);
                    g.setColor(new Color(255, 215, 0));
                    g.drawString(bossName, screenWidth / 2 - nameW / 2, bBarY + bBarH + 24);

                    // 4. CON SỐ HP
                    g.setFont(FontManager.getFont(13f));
                    String hpText = Math.max(0, e.getHp()) + " / " + e.getMaxHp();
                    int hpTextW = g.getFontMetrics().stringWidth(hpText);
                    g.setColor(Color.WHITE);
                    g.drawString(hpText, screenWidth / 2 - hpTextW / 2, bBarY + bBarH - 5);
                    
                    break;
                }
            }
        }

        // --- COMBO UI ---
        gameproject.ComboManager cm = player.getComboManager();
        if (cm.getComboCount() > 0) {
            int comboCount = cm.getComboCount();
            float timerRatio = cm.getTimerRatio();
            java.awt.Color comboColor = cm.getComboColor();

            int centerX = screenWidth / 2;
            int comboY = 150; // Đẩy xuống thấp hơn để tránh đè Boss HP Bar

            // Rung lắc mạnh hơn khi combo cao
            int shakeX = 0;
            if (cm.getTier() >= 2) {
                shakeX = (int) (Math.random() * 6 - 3);
            }

            g.setFont(gameproject.FontManager.getFont(36f * cm.getPulseScale()));
            String comboText = comboCount + " COMBO";
            int tw = g.getFontMetrics().stringWidth(comboText);

            // Draw shadow
            g.setColor(java.awt.Color.BLACK);
            g.drawString(comboText, centerX - tw / 2 + 3 + shakeX, comboY + 3);

            // Draw main text
            g.setColor(comboColor);
            g.drawString(comboText, centerX - tw / 2 + shakeX, comboY);

            // Draw Tier Title
            String title = cm.getTierTitle();
            if (!title.isEmpty()) {
                float titleScale = cm.getTier() == 3 ? (cm.getPulseScale() * 1.2f) : 1.0f;
                g.setFont(gameproject.FontManager.getFont(28f * titleScale));
                int ttw = g.getFontMetrics().stringWidth(title);
                g.setColor(java.awt.Color.BLACK);
                g.drawString(title, centerX - ttw / 2 + 2, comboY - 45 + 2);
                g.setColor(comboColor);
                g.drawString(title, centerX - ttw / 2, comboY - 45);
            }

            // Draw timer bar
            int barW = 100;
            int barH = 8;
            int comboBarX = centerX - barW / 2;
            int comboBarY = comboY + 15;

            g.setColor(new java.awt.Color(0, 0, 0, 150));
            g.fillRect(comboBarX, comboBarY, barW, barH);
            g.setColor(comboColor);
            g.fillRect(comboBarX, comboBarY, (int) (barW * timerRatio), barH);

            // Draw Buff text
            float atkBonus = cm.getFireRateBonus() * 100;
            float spdBonus = cm.getMoveSpeedBonus() * 100;
            if (atkBonus > 0) {
                g.setFont(gameproject.FontManager.getFont(12f));
                String buffText = "+" + (int) atkBonus + "% FIRE RATE";
                if (spdBonus > 0)
                    buffText += " | +" + (int) spdBonus + "% SPEED";

                g.setColor(java.awt.Color.BLACK);
                g.drawString(buffText, centerX - g.getFontMetrics().stringWidth(buffText) / 2 + 1, comboBarY + 25);
                g.setColor(java.awt.Color.GREEN);
                g.drawString(buffText, centerX - g.getFontMetrics().stringWidth(buffText) / 2, comboBarY + 24);
            }
        }

        drawMinimap(g, game, player, enemies);
    }

    private static void drawMinimap(Graphics g, gameproject.GamePanel game, Player player, ArrayList<Enemy> enemies) {
        boolean isLarge = game.input.showLargeMap;
        int mapSize = isLarge ? 600 : 150;
        int padding = 20;
        int mapX = isLarge ? (game.screenWidth - mapSize) / 2 : (game.screenWidth - mapSize - padding);
        int mapY = isLarge ? (game.screenHeight - mapSize) / 2 : 20;

        // Vẽ nền minimap (Đậm hơn nếu ở chế độ lớn để dễ nhìn)
        g.setColor(new Color(0, 0, 0, isLarge ? 200 : 150));
        g.fillRect(mapX, mapY, mapSize, mapSize);
        g.setColor(Color.WHITE);
        g.drawRect(mapX, mapY, mapSize, mapSize);

        float scaleX = (float) mapSize / gameproject.GamePanel.WORLD_WIDTH;
        float scaleY = (float) mapSize / gameproject.GamePanel.WORLD_HEIGHT;

        Graphics2D g2d = (Graphics2D) g;

        // 1. Vẽ VẬT CẢN (Map Grid) - Tối ưu hóa điểm vẽ
        MapManager mm = game.mapManager;
        int rows = gameproject.GamePanel.WORLD_HEIGHT / MapManager.TILE_SIZE;
        int cols = gameproject.GamePanel.WORLD_WIDTH / MapManager.TILE_SIZE;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Obstacle obs = mm.getObstacleAt(r, c);
                if (obs != null) {
                    if (obs instanceof Wall || obs instanceof Rock)
                        g.setColor(Color.DARK_GRAY);
                    else if (obs instanceof Tree)
                        g.setColor(new Color(34, 139, 34));
                    else if (obs instanceof WoodenCrate)
                        g.setColor(new Color(139, 69, 19));
                    else
                        g.setColor(Color.GRAY);

                    int ox = mapX + (int) (c * MapManager.TILE_SIZE * scaleX);
                    int oy = mapY + (int) (r * MapManager.TILE_SIZE * scaleY);
                    int pointSize = isLarge ? 4 : 2;
                    g.fillRect(ox, oy, pointSize, pointSize);
                }
            }
        }

        // 2. Vẽ TÒA NHÀ (Sử dụng hệ thống Mái nhà chính xác)
        for (Building b : game.buildings) {
            b.drawOnMinimap(g2d, mapX, mapY, scaleX, scaleY);
        }

        // 3. Vẽ QUÁI (chấm đỏ) - ĐỒNG BỘ HÓA
        synchronized (enemies) {
            for (Enemy e : enemies) {
                if (e.isDead())
                    continue;
                int ex = mapX + (int) (e.getX() * scaleX);
                int ey = mapY + (int) (e.getY() * scaleY);

                // Mimic đặc biệt (màu tím sáng)
                if (e instanceof gameproject.entity.Mimic) {
                    g.setColor(Color.MAGENTA);
                    int dotSize = isLarge ? 6 : 4;
                    g.fillRect(ex, ey, dotSize, dotSize);
                } else {
                    g.setColor(Color.RED);
                    int dotSize = e.isBoss ? (isLarge ? 8 : 4) : (isLarge ? 4 : 2);
                    g.fillRect(ex, ey, dotSize, dotSize);
                }
            }
        }

        // 3.5 Vẽ RƯƠNG SỰ KIỆN (chấm cam)
        synchronized (game.entityManager.eventChests) {
            g.setColor(Color.ORANGE);
            for (gameproject.entity.EventTreasure et : game.entityManager.eventChests) {
                if (et.opened)
                    continue;
                int ex = mapX + (int) (et.x * scaleX);
                int ey = mapY + (int) (et.y * scaleY);
                int dotSize = isLarge ? 5 : 3;
                g.fillRect(ex, ey, dotSize, dotSize);
            }
        }

        // 4. Vẽ PLAYER (chấm trắng lớn)
        g.setColor(Color.WHITE);
        int px = mapX + (int) (player.getX() * scaleX);
        int py = mapY + (int) (player.getY() * scaleY);
        int pSize = isLarge ? 6 : 3;
        g.fillRect(px - pSize / 2, py - pSize / 2, pSize, pSize);

        // 5. Darkness on Minimap
        if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.DARKNESS &&
                gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {

            int miniVisionRadius = isLarge ? 90 : 30;
            float[] fractions = { 0.0f, 0.7f, 1.0f };
            Color[] colors = { new Color(0, 0, 0, 0), new Color(0, 0, 0, 180), Color.BLACK };

            RadialGradientPaint rgp = new RadialGradientPaint(new java.awt.geom.Point2D.Float(px, py), miniVisionRadius,
                    fractions, colors);
            g2d.setPaint(rgp);

            // Lưu lại clip cũ
            java.awt.Shape oldClip = g2d.getClip();
            g2d.clipRect(mapX, mapY, mapSize, mapSize);
            g2d.fillRect(mapX, mapY, mapSize, mapSize);
            g2d.setClip(oldClip);
            g2d.setPaint(null);
        }

        if (isLarge) {
            g.setFont(FontManager.getFont(20f));
            g.drawString("WORLD MAP (M to close)", mapX, mapY - 10);
        }
    }
}
