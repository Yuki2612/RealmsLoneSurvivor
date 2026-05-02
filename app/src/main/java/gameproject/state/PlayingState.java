package gameproject.state;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import gameproject.GamePanel;
import gameproject.ImageManager;
import gameproject.SoundManager;
import gameproject.ui.HUD;
import gameproject.ui.CharacterStatsUI;
import gameproject.skill.Upgrade;
import gameproject.skill.PassiveSkill;
import gameproject.skill.FrostAuraSkill;
import gameproject.skill.PoisonCloudSkill;
import gameproject.skill.OrbitingOrbsSkill;
import gameproject.environment.Building;

public class PlayingState implements State {
    public enum EventType { NONE, ACID_RAIN, DARKNESS, MIMIC_MANIA, BLOOD_MOON }
    public enum EventPhase { WARNING, ACTIVE, ENDING }

    public static EventType activeEvent = EventType.NONE;
    public static EventPhase eventPhase = EventPhase.WARNING;
    
    public static long eventEndTime = 0;
    private static long nextPhaseTime = 0;
    private static int lastCheckedWave = 0;
    private static long lastDamageTick = 0;

    public static void resetEvents() {
        activeEvent = EventType.NONE;
        eventPhase = EventPhase.WARNING;
        eventEndTime = 0;
        nextPhaseTime = 0;
        lastCheckedWave = 0;
        lastDamageTick = 0;
    }
    
    private boolean showStats = false;
    private boolean iKeyPrev = false;

    @Override
    public void update(GamePanel game) {
        if (game.input.escPressed) {
            if (showStats) {
                showStats = false;
                gameproject.GamePanel.resumeGame();
            }
            game.player.resetMovement();
            game.input.isMouseHolding = false;
            game.input.clearClickAndKey();
            game.changeState(new PauseState());
            return;
        }

        // Toggle Stats with 'I' key
        if (game.input.iPressed && !iKeyPrev) {
            showStats = !showStats;
            if (showStats) {
                game.player.resetMovement();
                game.input.isMouseHolding = false;
                gameproject.GamePanel.pauseGame();
            } else {
                gameproject.GamePanel.resumeGame();
            }
        }
        iKeyPrev = game.input.iPressed;

        if (showStats) return;

        long currentTime = gameproject.GamePanel.getTickTime();
        int surviveTimeSeconds = (int) ((currentTime - game.startTime) / 1000);
        game.surviveTimeSeconds = surviveTimeSeconds;

        handleEvents(game, currentTime);

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
        synchronized (game.buildings) {
            for (gameproject.environment.Building b : game.buildings) {
                b.update(game.player);
            }
        }
    }

    private void handleEvents(GamePanel game, long currentTime) {
        int wave = game.entityManager.waveCount;
        
        // Trigger mới mỗi 4 wave (bắt đầu từ wave 4)
        if (wave > 0 && wave % 4 == 0 && wave != lastCheckedWave) {
            lastCheckedWave = wave;
            // Chọn ngẫu nhiên 1 trong các event
            double rnd = Math.random();
            if (rnd < 0.25) activeEvent = EventType.ACID_RAIN;
            else if (rnd < 0.50) activeEvent = EventType.DARKNESS;
            else if (rnd < 0.75) activeEvent = EventType.MIMIC_MANIA;
            else activeEvent = EventType.BLOOD_MOON;
            
            eventPhase = EventPhase.WARNING;
            nextPhaseTime = currentTime + 10000;
            eventEndTime = currentTime + 50000; // Tổng 50s (10s báo + 40s chạy)
        }

        if (activeEvent != EventType.NONE) {
            // Timeout logic: Khi hết thời gian, các treasure chưa mở sẽ hóa mimic
            // Timeout logic
            if (currentTime > eventEndTime) {
                if (eventPhase != EventPhase.ENDING) {
                    // Chuyển sang phase kết thúc (dần dần sáng)
                    eventPhase = EventPhase.ENDING;
                    eventEndTime = currentTime + 3000; // 3 giây để tan biến bóng tối
                    
                    if (activeEvent == EventType.MIMIC_MANIA) {
                        java.util.List<gameproject.entity.EventTreasure> chestsToConvert;
                        synchronized (game.entityManager.eventChests) {
                            chestsToConvert = new java.util.ArrayList<>(game.entityManager.eventChests);
                            game.entityManager.eventChests.clear();
                        }
                        synchronized (game.entityManager.enemies) {
                            for (gameproject.entity.EventTreasure et : chestsToConvert) {
                                game.entityManager.enemies.add(new gameproject.entity.Mimic(et.x, et.y, wave));
                            }
                        }
                        game.vfxManager.showWaveBanner("THE REMAINING CHESTS AWAKEN!", Color.RED, currentTime);
                    }
                    return;
                } else {
                    activeEvent = EventType.NONE;
                    return;
                }
            }

            if (eventPhase == EventPhase.WARNING) {
                long timeLeft = (nextPhaseTime - currentTime) / 1000;
                if (timeLeft >= 0) {
                    String msg = "";
                    if (activeEvent == EventType.ACID_RAIN) msg = "⚠ ACID RAIN IN " + timeLeft + "s! FIND SHELTER!";
                    else if (activeEvent == EventType.DARKNESS) msg = "⚠ DARKNESS APPROACHING IN " + timeLeft + "s!";
                    else if (activeEvent == EventType.BLOOD_MOON) msg = "⚠ BLOOD MOON IN " + timeLeft + "s! PREPARE FOR CARNAGE!";
                    else if (activeEvent == EventType.MIMIC_MANIA) {
                        msg = "⚠ FIND TREASURES IN BUILDINGS! (" + timeLeft + "s)";
                    }
                    
                    // Chỉ show banner mỗi giây
                    if (currentTime % 1000 < 50) {
                        game.vfxManager.showWaveBanner(msg, Color.YELLOW, currentTime);
                    }
                }
                if (currentTime > nextPhaseTime) {
                    eventPhase = EventPhase.ACTIVE;
                    String startMsg = "NIGHTFALL!";
                    if (activeEvent == EventType.ACID_RAIN) startMsg = "ACID RAIN ACTIVE!";
                    else if (activeEvent == EventType.MIMIC_MANIA) {
                        startMsg = "FIND THEM BEFORE THEY AWAKEN!";
                        // Spawn rương khi bắt đầu phase ACTIVE (hết 10s đếm ngược)
                        synchronized (game.buildings) {
                            for (Building b : game.buildings) {
                                java.awt.Rectangle r = b.getBounds();
                                game.entityManager.eventChests.add(new gameproject.entity.EventTreasure(r.x + r.width/2 - 20, r.y + r.height/2 - 20));
                            }
                        }
                    }
                    else if (activeEvent == EventType.BLOOD_MOON) startMsg = "THE BLOOD MOON RISES!";
                    
                    game.vfxManager.showWaveBanner(startMsg, Color.RED, currentTime);
                }
            } else if (eventPhase == EventPhase.ACTIVE) {
                if (activeEvent == EventType.ACID_RAIN) {
                    // Gây sát thương mỗi 500ms
                    if (currentTime - lastDamageTick > 500) {
                        boolean safe = false;
                        synchronized (game.buildings) {
                            for (Building b : game.buildings) {
                                if (b.isPlayerInside()) {
                                    safe = true;
                                    break;
                                }
                            }
                        }
                        if (!safe) {
                            int oldHearts = game.player.getHearts();
                            if (game.player.takeHit()) {
                                game.triggerGameOver();
                            } else if (game.player.getHearts() < oldHearts) {
                                game.vfxManager.triggerPlayerDamageFlash(currentTime);
                            }
                        }
                        lastDamageTick = currentTime;
                    }
                }
            }
        }
    }

    private void triggerShoot(GamePanel game, long currentTime) {
        int critLevel = game.player.getUpgradeLevel(Upgrade.CRIT_CHANCE);
        float baseCrit = gameproject.meta.PlayerData.statCritLevel * 0.01f;
        float totalCrit = baseCrit + (critLevel * 0.07f);
        boolean isCrit = totalCrit > 0 && Math.random() < totalCrit;

        int bouncesAndPierces = game.player.getUpgradeLevel(Upgrade.CHAIN_LIGHTNING);

        // Kết hợp tọa độ camera và chuột để bắn theo đúng world coordinates
        float worldMouseX = game.input.mouseX + game.cameraX;
        float worldMouseY = game.input.mouseY + game.cameraY;

        // Ghi lại số lượng đạn trước khi bắn để xác định đạn mới
        int prevSize = game.entityManager.projectiles.size();
        game.currentWeapon.shoot(game.player.getX(), game.player.getY(), worldMouseX, worldMouseY,
                game.upgradeManager.playerDamage, bouncesAndPierces, game.entityManager.projectiles,
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
        
        // CHỐT SỔ TỌA ĐỘ CAMERA DUY NHẤT 1 LẦN CHO CHU KỲ RENDER
        game.camIntX = (int) Math.round(game.cameraX);
        game.camIntY = (int) Math.round(game.cameraY);

        game.vfxManager.applyScreenShake(g2d);

        // QUY CHUẨN PIXEL-PERFECT: Dịch chuyển toàn bộ thế giới theo tọa độ camera đã chốt sổ
        g2d.translate(-game.camIntX, -game.camIntY);

        // 1. Vẽ Sàn nhà (Nằm dưới mọi vật thể nhưng trên cỏ)
        synchronized (game.buildings) {
            for (gameproject.environment.Building b : game.buildings) {
                b.renderFloor(g2d);
            }
        }

        synchronized (game.activeSkills) {
            for (PassiveSkill skill : game.activeSkills) {
                if (skill instanceof gameproject.skill.FrostAuraSkill ||
                        skill instanceof gameproject.skill.PoisonCloudSkill ||
                        skill instanceof gameproject.skill.PulseWaveSkill ||
                        skill instanceof gameproject.skill.EnergyShieldSkill) {
                    skill.draw(g, game.player);
                }
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

        synchronized (game.activeSkills) {
            for (PassiveSkill skill : game.activeSkills) {
                if (skill instanceof gameproject.skill.OrbitingOrbsSkill)
                    skill.draw(g, game.player);
            }
        }

        // 5. VẼ MÁI NHÀ (Trên cùng) - Đồng bộ hóa để tránh CME
        synchronized (game.buildings) {
            for (gameproject.environment.Building b : game.buildings) {
                b.renderRoof(g2d);
            }
        }

        // QUAY LẠI TỌA ĐỘ MÀN HÌNH (SCREEN SPACE) ĐỂ VẼ HUD
        g2d.translate(game.camIntX, game.camIntY);

        game.vfxManager.resetScreenShake(g2d);
        // Overlay toàn màn hình (Bóng tối, bão acid, flash đỏ, wave banner)
        long now = gameproject.GamePanel.getTickTime();
        game.vfxManager.drawOverlays(g, game.screenWidth, game.screenHeight, now, game);

        // --- HUD --- (Vẽ HUD sau cùng để nổi lên trên các hiệu ứng môi trường)
        gameproject.ui.HUD.draw(g, game, game.player, game.entityManager.enemies);

        if (showStats) {
            CharacterStatsUI.draw(g, game, game.player);
        }
    }
}
