package gameproject.state;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import gameproject.GamePanel;
import gameproject.ImageManager;
import gameproject.SoundManager;
import gameproject.ui.HUD;
import gameproject.skill.Upgrade;
import gameproject.skill.PassiveSkill;
import gameproject.skill.FrostAuraSkill;
import gameproject.skill.PoisonCloudSkill;
import gameproject.skill.OrbitingOrbsSkill;

public class PlayingState implements State {
    @Override
    public void update(GamePanel game) {
        if (game.input.escPressed) {
            game.player.resetMovement();
            game.input.isMouseHolding = false;
            game.input.clearClickAndKey();
            game.changeState(new PauseState());
            return;
        }

        long currentTime = System.currentTimeMillis();
        int surviveTimeSeconds = (int) ((currentTime - game.startTime) / 1000);
        game.surviveTimeSeconds = surviveTimeSeconds;

        game.player.update(game);

        // --- CẬP NHẬT CAMERA (Hoàn trả Snapping - Phản hồi tức thì) ---
        // Gán trực tiếp nhưng làm tròn số nguyên để camera bám khít nhân vật không độ
        // trễ
        game.cameraX = Math.round(game.player.getX() - game.screenWidth / 2f + game.player.getBounds().width / 2f);
        game.cameraY = Math.round(game.player.getY() - game.screenHeight / 2f + game.player.getBounds().height / 2f);

        // Giới hạn camera không trượt ra ngoài bản đồ
        if (game.cameraX < 0)
            game.cameraX = 0;
        if (game.cameraY < 0)
            game.cameraY = 0;
        if (game.cameraX > GamePanel.WORLD_WIDTH - game.screenWidth)
            game.cameraX = GamePanel.WORLD_WIDTH - game.screenWidth;
        if (game.cameraY > GamePanel.WORLD_HEIGHT - game.screenHeight)
            game.cameraY = GamePanel.WORLD_HEIGHT - game.screenHeight;

        game.vfxManager.update(currentTime);

        // Combo Sparkles (Tier 1+)
        int tier = game.player.getComboManager().getTier();
        if (tier >= 1) {
            game.vfxManager.spawnComboSparkles(game.player.getX() + 12, game.player.getY() + 12, currentTime,
                    game.player.getComboManager().getComboColor(), tier);
        }

        // Dash afterimage
        if (game.player.isDashing()) {
            game.vfxManager.addDashAfterimage(game.player.getX(), game.player.getY(), currentTime);
        }

        game.entityManager.update(game.player, game.vfxManager, game.activeSkills, game.screenWidth, game.screenHeight,
                currentTime, surviveTimeSeconds, game);

        float fireRateBonus = game.player.getComboManager().getFireRateBonus();
        if (game.input.isMouseHolding && game.currentWeapon.isAutomatic
                && game.currentWeapon.canShoot(currentTime, fireRateBonus)) {
            triggerShoot(game, currentTime);
        } else if (game.input.mouseClicked && !game.currentWeapon.isAutomatic
                && game.currentWeapon.canShoot(currentTime, fireRateBonus)) {
            triggerShoot(game, currentTime);
        }

        if (game.upgradeManager.processLevelUp(game.player)) {
            game.player.resetMovement();
            game.input.isMouseHolding = false;
            SoundManager.play("levelup");
            game.changeState(new LevelUpState());
        }

        // --- CẬP NHẬT MÔI TRƯỜNG ---
        game.mapManager.update((int) game.player.getX(), (int) game.player.getY());
        for (gameproject.environment.Building b : game.buildings) {
            b.update(game.player);
        }
    }

    private void triggerShoot(GamePanel game, long currentTime) {
        int critLevel = game.player.getUpgradeLevel(Upgrade.CRIT_CHANCE);
        int finalDamage = game.upgradeManager.playerDamage;
        float baseCrit = gameproject.meta.PlayerData.statCritLevel * 0.01f;
        float totalCrit = baseCrit + (critLevel * 0.07f);
        boolean isCrit = totalCrit > 0 && Math.random() < totalCrit;
        if (isCrit) {
            finalDamage = (int) (finalDamage * 1.5);
        }

        int bouncesAndPierces = game.player.getUpgradeLevel(Upgrade.CHAIN_LIGHTNING);

        // Kết hợp tọa độ camera và chuột để bắn theo đúng world coordinates
        float worldMouseX = game.input.mouseX + game.cameraX;
        float worldMouseY = game.input.mouseY + game.cameraY;

        // Ghi lại số lượng đạn trước khi bắn để xác định đạn mới
        int prevSize = game.entityManager.projectiles.size();
        game.currentWeapon.shoot(game.player.getX(), game.player.getY(), worldMouseX, worldMouseY,
                game.upgradeManager.bulletSpeedMulti, finalDamage, bouncesAndPierces, game.entityManager.projectiles,
                currentTime);

        // Gán flag isCrit cho tất cả đạn vừa được thêm
        if (isCrit) {
            for (int i = prevSize; i < game.entityManager.projectiles.size(); i++) {
                game.entityManager.projectiles.get(i).isCrit = true;
            }
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        java.awt.image.BufferedImage bg = gameproject.ImageManager.get(game.currentBgKey);
        if (bg != null) {
            int bgWidth = bg.getWidth();
            int bgHeight = bg.getHeight();
            // Lặp background vô tận theo camera
            int startX = (int) -(game.cameraX % bgWidth);
            if (startX > 0)
                startX -= bgWidth;
            int startY = (int) -(game.cameraY % bgHeight);
            if (startY > 0)
                startY -= bgHeight;

            for (int x = startX; x < game.screenWidth; x += bgWidth) {
                for (int y = startY; y < game.screenHeight; y += bgHeight) {
                    g.drawImage(bg, x, y, null);
                }
            }
        } else {
            g.setColor(java.awt.Color.DARK_GRAY);
            g.fillRect(0, 0, game.screenWidth, game.screenHeight);
        }

        game.vfxManager.applyScreenShake(g2d);

        // DỊCH CHUYỂN CAMERA CHO WORLD RENDERING
        // Translate dựa trên tọa độ camera đã làm tròn để tránh rung hình (jitter)
        g2d.translate(-Math.round(game.cameraX), -Math.round(game.cameraY));

        // 1. Vẽ Sàn nhà (Nằm dưới mọi vật thể nhưng trên cỏ)
        synchronized (game.buildings) {
            for (gameproject.environment.Building b : game.buildings) {
                b.renderFloor(g2d);
            }
        }

        for (PassiveSkill skill : game.activeSkills) {
            if (skill instanceof gameproject.skill.FrostAuraSkill ||
                    skill instanceof gameproject.skill.PoisonCloudSkill ||
                    skill instanceof gameproject.skill.PulseWaveSkill ||
                    skill instanceof gameproject.skill.EnergyShieldSkill) {
                skill.draw(g, game.player);
            }
        }

        game.vfxManager.draw(g, game.player);

        // 1. Vẽ vật phẩm dưới đất (Rương, Tim, Soul)
        game.entityManager.drawGroundItems(g);

        // 2. --- THUẬT TOÁN Y-SORTING (Z-INDEX) ---
        // Gom tất cả các đối tượng có độ sâu vào một danh sách
        java.util.List<gameproject.Renderable> renderList = new java.util.ArrayList<>();
        renderList.add(game.player);
        synchronized (game.entityManager.enemies) {
            renderList.addAll(game.entityManager.enemies);
        }
        renderList.addAll(game.mapManager.getAllObstacles());

        // Sắp xếp theo tọa độ chân (Bottom Y)
        java.util.Collections.sort(renderList,
                java.util.Comparator.comparingDouble(gameproject.Renderable::getBottomY));

        // 3. Vẽ các đối tượng đã được sắp xếp
        for (gameproject.Renderable r : renderList) {
            r.render(g2d);
        }

        // 4. Vẽ các hiệu ứng bay trên cao (Đạn)
        game.entityManager.drawProjectiles(g);

        for (PassiveSkill skill : game.activeSkills) {
            if (skill instanceof gameproject.skill.OrbitingOrbsSkill)
                skill.draw(g, game.player);
        }

        // 5. VẼ MÁI NHÀ (Trên cùng) - Đồng bộ hóa để tránh CME
        synchronized (game.buildings) {
            for (gameproject.environment.Building b : game.buildings) {
                b.renderRoof(g2d);
            }
        }

        // RESET CAMERA TRANSLATION CHO HUD
        g2d.translate(game.cameraX, game.cameraY);

        game.vfxManager.resetScreenShake(g2d);
        // --- HUD ---
        gameproject.ui.HUD.draw(g, game, game.player, game.entityManager.enemies);

        // Overlay toàn màn hình (flash đỏ, wave banner, combo vignette) — sau cùng
        long now = System.currentTimeMillis();
        game.vfxManager.drawOverlays(g, game.screenWidth, game.screenHeight, now, game.player);
    }
}
