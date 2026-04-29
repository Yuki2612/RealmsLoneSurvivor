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

        game.player.update(game.screenWidth, game.screenHeight);

        // --- CẬP NHẬT CAMERA ---
        game.cameraX = game.player.getX() - game.screenWidth / 2f + game.player.getBounds().width / 2f;
        game.cameraY = game.player.getY() - game.screenHeight / 2f + game.player.getBounds().height / 2f;

        // Giới hạn camera không trượt ra ngoài bản đồ
        if (game.cameraX < 0) game.cameraX = 0;
        if (game.cameraY < 0) game.cameraY = 0;
        if (game.cameraX > GamePanel.WORLD_WIDTH - game.screenWidth) game.cameraX = GamePanel.WORLD_WIDTH - game.screenWidth;
        if (game.cameraY > GamePanel.WORLD_HEIGHT - game.screenHeight) game.cameraY = GamePanel.WORLD_HEIGHT - game.screenHeight;

        game.vfxManager.update(currentTime);

        // Dash afterimage
        if (game.player.isDashing()) {
            game.vfxManager.addDashAfterimage(game.player.getX(), game.player.getY(), currentTime);
        }

        game.entityManager.update(game.player, game.vfxManager, game.activeSkills, game.screenWidth, game.screenHeight,
                currentTime, surviveTimeSeconds, game);

        if (game.input.isMouseHolding && game.currentWeapon.isAutomatic && game.currentWeapon.canShoot(currentTime)) {
            triggerShoot(game, currentTime);
        } else if (game.input.mouseClicked && !game.currentWeapon.isAutomatic
                && game.currentWeapon.canShoot(currentTime)) {
            triggerShoot(game, currentTime);
        }

        if (game.upgradeManager.processLevelUp(game.player)) {
            game.player.resetMovement();
            game.input.isMouseHolding = false;
            SoundManager.play("levelup");
            game.changeState(new LevelUpState());
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
            if (startX > 0) startX -= bgWidth;
            int startY = (int) -(game.cameraY % bgHeight);
            if (startY > 0) startY -= bgHeight;

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
        g2d.translate(-game.cameraX, -game.cameraY);

        for (PassiveSkill skill : game.activeSkills) {
            if (skill instanceof gameproject.skill.FrostAuraSkill || 
                skill instanceof gameproject.skill.PoisonCloudSkill ||
                skill instanceof gameproject.skill.PulseWaveSkill ||
                skill instanceof gameproject.skill.EnergyShieldSkill) {
                skill.draw(g, game.player);
            }
        }

        game.vfxManager.draw(g, game.player);
        game.entityManager.draw(g);
        game.player.draw(g);

        for (PassiveSkill skill : game.activeSkills) {
            if (skill instanceof gameproject.skill.OrbitingOrbsSkill)
                skill.draw(g, game.player);
        }

        // RESET CAMERA TRANSLATION CHO HUD
        g2d.translate(game.cameraX, game.cameraY);

        game.vfxManager.resetScreenShake(g2d);

        HUD.draw(g, game.screenWidth, game.screenHeight, game.score, game.entityManager.waveCount,
                game.upgradeManager.playerDamage, game.currentWeapon.getActualCooldown(), game.player,
                game.upgradeManager.currentExp, game.upgradeManager.expToNextLevel, game.upgradeManager.playerLevel,
                game.entityManager.enemies);

        // Overlay toàn màn hình (flash đỏ, wave banner) — sau cùng
        long now = System.currentTimeMillis();
        game.vfxManager.drawOverlays(g, game.screenWidth, game.screenHeight, now);
    }
}
