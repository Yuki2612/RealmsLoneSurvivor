package gameproject.state;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import gameproject.GamePanel;
import gameproject.SoundManager;
import gameproject.ui.CharacterStatsUI;
import gameproject.skill.Upgrade;
import gameproject.skill.PassiveSkill;
import gameproject.environment.Building;
import gameproject.event.EventManager;

public class PlayingState implements State {
    public enum EventType {
        NONE, ACID_RAIN, DARKNESS, MIMIC_MANIA, BLOOD_MOON, TOXIC_WATERS, ALTAR_ASCENSION
    }

    public enum EventPhase {
        WARNING, ACTIVE, ENDING
    }

    public static EventType activeEvent = EventType.NONE;
    public static EventPhase eventPhase = EventPhase.WARNING;

    public static long eventEndTime = 0;
    private static long nextPhaseTime = 0;
    private static int lastCheckedWave = 0;

    private static EventManager eventManager = new EventManager();

    public static void resetEvents() {
        activeEvent = EventType.NONE;
        eventPhase = EventPhase.WARNING;
        eventEndTime = 0;
        nextPhaseTime = 0;
        lastCheckedWave = 0;
        phantomClonesHit = 0;
        eventManager.reset();
    }

    private boolean showStats = false;
    private boolean dialogueTriggered = false;
    private boolean enterKeyPrev = false;
    private boolean iKeyPrev = false;
    private boolean lostHealthThisRun = false;
    private int lastHearts = -1;
    private long clutchSurvivalStart = -1;
    private static final long CLUTCH_SURVIVAL_NEEDED = 300000; // 5 mins in ms
    private boolean clutchAchieved = false;

    // --- NEW ACHIEVEMENT TRACKING ---
    private boolean usedDashThisRun = false;
    public static int phantomClonesHit = 0; // Static to access from PhantomWarlock
    private float lastPlayerX = -1, lastPlayerY = -1;
    private int goldAtStart = -1;
    private java.util.Set<Integer> visitedBuildings = new java.util.HashSet<>();
    private long buildingStayStart = -1;
    private long totalBuildingTime = 0;
    private long waterStayStart = -1;
    private boolean diverAchieved = false;
    private int damageTakenThisWave = 0;
    private int bulletsShotThisWave = 0;
    private int noHitWaveStreak = 0;
    private int noShootWaveStreak = 0;
    private int currentWaveCheck = 1;

    // --- PERFORMANCE OPTIMIZATION (Part 1 & 2) ---
    private java.util.List<gameproject.Renderable> renderList = new java.util.ArrayList<>(500);
    private static final java.util.Comparator<gameproject.Renderable> Y_COMPARATOR = (a, b) -> Double
            .compare(a.getBottomY(), b.getBottomY());

    private float shakeOffsetDx = 0;
    private float shakeOffsetDy = 0;

    @Override
    public void update(GamePanel game) {
        // --- LOGIC HỘI THOẠI ---
        if (game.dialogueManager.isActive()) {
            game.dialogueManager.update();
            if (game.input.enterPressed && !enterKeyPrev) {
                game.dialogueManager.nextPace();
            }
            enterKeyPrev = game.input.enterPressed;
            return;
        }

        // Kích hoạt hội thoại lần đầu (Outskirts)
        if (!dialogueTriggered && game.surviveTimeSeconds == 0) {
            triggerInitialDialogue(game);
            dialogueTriggered = true;
            return;
        }

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

        if (showStats)
            return;

        long currentTime = gameproject.GamePanel.getTickTime();
        int surviveTimeSeconds = (int) ((currentTime - game.startTime) / 1000);
        game.surviveTimeSeconds = surviveTimeSeconds;

        // Init starting stats
        if (goldAtStart == -1)
            goldAtStart = gameproject.meta.PlayerData.gold;
        if (lastPlayerX == -1) {
            lastPlayerX = game.player.getX();
            lastPlayerY = game.player.getY();
        }

        // Track Distance
        float dx = game.player.getX() - lastPlayerX;
        float dy = game.player.getY() - lastPlayerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0 && dist < 100) { // Filter teleport/respawn
            gameproject.meta.AchievementManager.getInstance().updateDistance(dist);
            lastPlayerX = game.player.getX();
            lastPlayerY = game.player.getY();
        }

        // Track Wealth (In Run)
        int currentGoldInRun = gameproject.meta.PlayerData.gold - goldAtStart;
        gameproject.meta.AchievementManager.getInstance().onStatChanged("wealth", currentGoldInRun, "gold");

        // Track Dash
        if (game.player.isDashing())
            usedDashThisRun = true;

        // Track Buildings
        boolean inside = false;
        synchronized (game.buildings) {
            for (int i = 0; i < game.buildings.size(); i++) {
                if (game.buildings.get(i).isPlayerInside()) {
                    inside = true;
                    visitedBuildings.add(i);
                    break;
                }
            }
        }
        if (inside) {
            if (buildingStayStart == -1)
                buildingStayStart = currentTime;
            else {
                long duration = currentTime - buildingStayStart;
                totalBuildingTime += duration;
                gameproject.meta.AchievementManager.getInstance().onStatChanged("explorer",
                        (int) (totalBuildingTime / 60000), "homebody");
                buildingStayStart = currentTime;
            }
        } else {
            buildingStayStart = -1;
        }

        // --- TRACK WATER STAY (SECRET: DIVER) ---
        if (!diverAchieved) {
            float pMidX = game.player.getX() + 12.5f;
            float pMidY = game.player.getY() + 12.5f;
            if (game.mapManager.getTileTypeAtWorld(pMidX, pMidY).equals("water")) {
                if (waterStayStart == -1)
                    waterStayStart = currentTime;
                else if (currentTime - waterStayStart >= 60000) {
                    gameproject.meta.AchievementManager.getInstance().onSecretTriggered("diver");
                    diverAchieved = true;
                }
            } else {
                waterStayStart = -1;
            }
        }

        if (visitedBuildings.size() >= game.buildings.size() && game.buildings.size() > 0) {
            gameproject.meta.AchievementManager.getInstance().onStatChanged("explorer", 1, "architect");
        }

        // Track Combo
        int combo = game.player.getComboManager().getComboCount();
        gameproject.meta.AchievementManager.getInstance().onStatChanged("combat", combo, "combo");

        // Track Phantom Clones
        if (phantomClonesHit >= 10) {
            gameproject.meta.AchievementManager.getInstance().onStatChanged("secret", 1, "phantom_troll");
        }

        handleEvents(game, currentTime);

        game.player.update(game);

        // --- CẬP NHẬT CAMERA ---
        shakeOffsetDx = 0;
        shakeOffsetDy = 0;
        if (game.vfxManager.getShakeTimer() > 0) {
            // Giảm cường độ rung (từ 10 xuống 4) để bớt gây khó chịu
            shakeOffsetDx = (float) (Math.random() * 4 - 2);
            shakeOffsetDy = (float) (Math.random() * 4 - 2);
            game.vfxManager.decrementShakeTimer();
        }

        // Sử dụng Math.round để tọa độ camera luôn là số nguyên, tránh lệch pixel với
        // player/tiles
        game.cameraX = Math.round(game.player.getX() - game.screenWidth / 2f + game.player.getBounds().width / 2f)
                + shakeOffsetDx;
        game.cameraY = Math.round(game.player.getY() - game.screenHeight / 2f + game.player.getBounds().height / 2f)
                + shakeOffsetDy;

        // Giới hạn camera không trượt ra ngoài bản đồ
        if (game.cameraX < 0)
            game.cameraX = 0;
        if (game.cameraY < 0)
            game.cameraY = 0;
        if (game.cameraX > GamePanel.WORLD_WIDTH - game.screenWidth)
            game.cameraX = GamePanel.WORLD_WIDTH - game.screenWidth;
        if (game.cameraY > GamePanel.WORLD_HEIGHT - game.screenHeight)
            game.cameraY = GamePanel.WORLD_HEIGHT - game.screenHeight;

        game.camIntX = (int) (game.cameraX);
        game.camIntY = (int) (game.cameraY);

        game.vfxManager.update(currentTime);

        // Combo Sparkles (Tier 1+)
        int tier = game.player.getComboManager().getTier();
        if (tier >= 1) {
            game.vfxManager.spawnComboSparkles(game.player.getX() + 12, game.player.getY() + 12, currentTime,
                    game.player.getComboManager().getComboColor(), tier);
        }

        // Dash afterimage
        if (game.player.isDashing()) {
            int dSize = gameproject.Player.SIZE + 20;
            java.awt.image.BufferedImage currentFrame = null;
            if (game.player.getActiveAnim() != null) {
                currentFrame = game.player.getActiveAnim().getCurrentFrame();
            }
            if (currentFrame != null) {
                game.vfxManager.addDashAfterimage(
                        game.player.getX() - 10,
                        game.player.getY() - 20,
                        dSize, dSize,
                        currentTime,
                        currentFrame,
                        game.player.isFacingRight());
            }
        }

        game.entityManager.update(game.player, game.vfxManager, game.activeSkills, game.screenWidth, game.screenHeight,
                currentTime, surviveTimeSeconds, game);

        float fireRateBonus = game.player.getComboManager().getFireRateBonus();
        float frenzyBonus = game.player.getFrenzyFireRateBonus();
        if (game.input.isMouseHolding && game.currentWeapon.isAutomatic
                && game.currentWeapon.canShoot(currentTime, fireRateBonus, frenzyBonus)) {
            triggerShoot(game, currentTime);
        } else if (game.input.mouseClicked && !game.currentWeapon.isAutomatic
                && game.currentWeapon.canShoot(currentTime, fireRateBonus, frenzyBonus)) {
            triggerShoot(game, currentTime);
        }

        // Tích hợp Thành tựu
        gameproject.meta.AchievementManager.getInstance().updateSurvivalTime(surviveTimeSeconds);

        if (lastHearts == -1)
            lastHearts = game.player.getHearts();
        if (game.player.getHearts() < lastHearts) {
            lostHealthThisRun = true;
            int damage = lastHearts - game.player.getHearts();
            if (damage > 0)
                damageTakenThisWave += damage;
            clutchSurvivalStart = -1; // Reset clutch timer on damage
        }
        lastHearts = game.player.getHearts();

        // Check Clutch Survivor (Survive 5 mins with 1 HP)
        if (!clutchAchieved && game.player.getHearts() == 1) {
            if (clutchSurvivalStart == -1) {
                clutchSurvivalStart = currentTime;
            } else if (currentTime - clutchSurvivalStart >= CLUTCH_SURVIVAL_NEEDED) {
                gameproject.meta.AchievementManager.getInstance().onClutchSurvived();
                clutchAchieved = true;
            }
        } else {
            clutchSurvivalStart = -1;
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

        // --- KIỂM TRA WAVE TRANSITION (Thành tựu Chuỗi Streak) ---
        if (game.entityManager.waveCount > currentWaveCheck) {
            // Nếu currentWaveCheck > 0, nghĩa là một wave vừa kết thúc thực sự
            if (currentWaveCheck > 0) {
                checkWaveStreaks();
            }
            currentWaveCheck = game.entityManager.waveCount;
            damageTakenThisWave = 0;
            bulletsShotThisWave = 0;
        }

        // --- KIỂM TRA CHIẾN THẮNG ---
        if (game.entityManager.waveCount >= gameproject.entity.EntityManager.FINAL_WAVE
                && game.entityManager.enemies.isEmpty()) {
            // Kiểm tra nốt wave cuối cùng cho chuỗi streak trước khi thắng
            checkWaveStreaks();
            game.triggerVictory(!lostHealthThisRun, !usedDashThisRun);
        }

        // --- KIỂM TRA GAME OVER ---
        if (game.player.getHearts() <= 0) {
            game.triggerGameOver();
        }

        eventManager.update(game, currentTime);
    }

    private void handleEvents(GamePanel game, long currentTime) {
        int wave = game.entityManager.waveCount;

        if (wave > 0 && wave % 4 == 0 && wave != lastCheckedWave) {
            lastCheckedWave = wave;

            java.util.List<EventType> pool = game.currentMapConfig.possibleEvents;
            if (pool != null && !pool.isEmpty()) {
                activeEvent = pool.get((int) (Math.random() * pool.size()));
            } else {
                activeEvent = EventType.NONE;
            }

            eventPhase = EventPhase.WARNING;
            nextPhaseTime = currentTime + 10000;
            eventEndTime = currentTime + 50000;

            // Kích hoạt thành tựu gặp mặt sự kiện
            if (activeEvent != EventType.NONE) {
                String eventKey = activeEvent.name().toLowerCase();
                gameproject.meta.AchievementManager.getInstance().onEventMet(eventKey);
            }

            // Notification on phase change handled by Manager later
        }

        if (activeEvent != EventType.NONE) {
            if (currentTime > eventEndTime) {
                if (eventPhase != EventPhase.ENDING) {
                    EventPhase oldPhase = eventPhase;
                    eventPhase = EventPhase.ENDING;
                    eventEndTime = currentTime + 3000;
                    eventManager.onPhaseChanged(game, oldPhase, eventPhase, currentTime);
                } else {
                    activeEvent = EventType.NONE;
                }
                return;
            }

            if (eventPhase == EventPhase.WARNING) {
                long timeLeft = (nextPhaseTime - currentTime) / 1000;
                if (timeLeft >= 0) {
                    String msg = eventManager.getWarningMessage(timeLeft);
                    if (!msg.isEmpty()) {
                        game.vfxManager.showWaveBanner("event_warning", msg, Color.YELLOW, currentTime);
                    }
                }
                if (currentTime > nextPhaseTime) {
                    EventPhase oldPhase = eventPhase;
                    eventPhase = EventPhase.ACTIVE;
                    eventManager.onPhaseChanged(game, oldPhase, eventPhase, currentTime);
                    game.vfxManager.removeWaveBanner("event_warning");

                    String startMsg = eventManager.getStartMessage();
                    if (!startMsg.isEmpty()) {
                        game.vfxManager.showWaveBanner(startMsg, Color.RED, currentTime);
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
        synchronized (game.entityManager.projectiles) {
            int prevSize = game.entityManager.projectiles.size();
            game.currentWeapon.shoot(game.player.getX(), game.player.getY(), worldMouseX, worldMouseY,
                    game.upgradeManager.playerDamage, bouncesAndPierces, game.entityManager.projectiles,
                    currentTime);

            bulletsShotThisWave += (game.entityManager.projectiles.size() - prevSize);

            // Gán flag isCrit cho tất cả đạn vừa được thêm
            if (isCrit) {
                for (int i = prevSize; i < game.entityManager.projectiles.size(); i++) {
                    game.entityManager.projectiles.get(i).isCrit = true;
                }
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

        // Dịch chuyển toàn bộ thế giới theo tọa độ camera số thực để mượt mà nhất
        g2d.translate(-game.cameraX, -game.cameraY);

        // 0. Vẽ Tiles (Nước, Đất đặc thù)
        String mapPrefix = game.currentMapConfig.type.name().toLowerCase();
        game.mapManager.renderTiles(g2d, game.camIntX, game.camIntY, game.screenWidth, game.screenHeight, mapPrefix);

        // 1. Vẽ Sàn nhà (Nằm dưới mọi vật thể nhưng trên cỏ)
        synchronized (game.buildings) {
            for (gameproject.environment.Building b : game.buildings) {
                if (b.isVisible(game.camIntX, game.camIntY, game.screenWidth, game.screenHeight)) {
                    b.renderFloor(g2d);
                }
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
        // Tái sử dụng danh sách và áp dụng Culling ngay khi gom nhóm
        renderList.clear();
        renderList.add(game.player);

        int camX = game.camIntX;
        int camY = game.camIntY;
        int sw = game.screenWidth;
        int sh = game.screenHeight;

        synchronized (game.entityManager.enemies) {
            for (gameproject.entity.Enemy e : game.entityManager.enemies) {
                // Sơ bộ kiểm tra tầm nhìn để giảm tải Sorting
                if (e.getX() > camX - 100 && e.getX() < camX + sw + 100 &&
                        e.getY() > camY - 100 && e.getY() < camY + sh + 100) {
                    renderList.add(e);
                }
            }
        }

        for (gameproject.environment.Building b : game.buildings) {
            if (b.isVisible(camX, camY, sw, sh)) {
                // Các phần của building có tính Z-index (nếu có)
            }
        }

        for (gameproject.Renderable r : game.mapManager.getAllObstacles()) {
            if (r.getBottomY() > camY - 100 && r.getBottomY() < camY + sh + 200) {
                renderList.add(r);
            }
        }

        // Sắp xếp sử dụng Comparator tĩnh
        java.util.Collections.sort(renderList, Y_COMPARATOR);

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
                if (b.isVisible(game.camIntX, game.camIntY, game.screenWidth, game.screenHeight)) {
                    b.renderRoof(g2d);
                }
            }
        }

        // QUAY LẠI TỌA ĐỘ MÀN HÌNH (SCREEN SPACE) ĐỂ VẼ HUD
        g2d.translate(game.cameraX, game.cameraY);

        // Overlay toàn màn hình (Bóng tối, bão acid, flash đỏ, wave banner)
        long now = gameproject.GamePanel.getTickTime();

        // Vẽ Map Overlay Color (Chỉ áp dụng trong game, không áp dụng cho UI chọn map)
        if (game.currentMapConfig != null && game.currentMapConfig.overlayColor.getAlpha() > 0) {
            g2d.setColor(game.currentMapConfig.overlayColor);
            g2d.fillRect(0, 0, game.screenWidth, game.screenHeight);
        }

        eventManager.render(g2d, game, game.screenWidth, game.screenHeight, now);
        game.vfxManager.drawOverlay(g, game, game.screenWidth, game.screenHeight, now);

        // --- HUD --- (Vẽ HUD sau cùng để nổi lên trên các hiệu ứng môi trường)
        gameproject.ui.HUD.draw(g, game, game.player, game.entityManager.enemies);

        // --- DIALOGUE UI --- (Vẽ sau cùng để đè lên HUD)
        game.dialogueManager.draw(g, game.screenWidth, game.screenHeight);

        if (showStats) {
            CharacterStatsUI.draw(g, game, game.player);
        }
    }

    private void checkWaveStreaks() {
        if (damageTakenThisWave == 0) {
            noHitWaveStreak++;
            gameproject.meta.AchievementManager.getInstance().onStatChanged("combat", noHitWaveStreak, "flawless");
        } else {
            noHitWaveStreak = 0;
        }

        if (bulletsShotThisWave == 0) {
            noShootWaveStreak++;
            gameproject.meta.AchievementManager.getInstance().onStatChanged("combat", noShootWaveStreak, "pacifist");
        } else {
            noShootWaveStreak = 0;
        }
    }

    private void triggerInitialDialogue(GamePanel game) {
        java.util.ArrayList<gameproject.ui.DialogueManager.DialogueLine> lines = new java.util.ArrayList<>();
        String pKey = gameproject.meta.PlayerData.getPlayerImageKey();
        String pName = game.player.getCharClass().name;

        if (game.currentMapConfig.mapId == 0) { // Outskirts
            lines.add(new gameproject.ui.DialogueManager.DialogueLine(pName,
                    "The sun is shining for now, but I shouldn't let my guard down. These monsters are just the beginning.",
                    pKey));
            lines.add(new gameproject.ui.DialogueManager.DialogueLine(pName,
                    "I'll use this clear sky to gather what I can. I need to be ready for when the air shifts and things become unpredictable.", pKey));
            lines.add(new gameproject.ui.DialogueManager.DialogueLine(pName,
                    "Something tells me this peace won't last long. I must prepare before the day turns chaotic!",
                    pKey));
        } else if (game.currentMapConfig.mapId == 1) { // Swamp
            lines.add(new gameproject.ui.DialogueManager.DialogueLine(pName,
                    "This fog... it's not natural. I can sense a dark presence lurking deep within this rot.", pKey));
            lines.add(new gameproject.ui.DialogueManager.DialogueLine(pName,
                    "I need to find the ancient stones scattered in this mire... and bring them to the sacred place to break the seal.",
                    pKey));
            lines.add(new gameproject.ui.DialogueManager.DialogueLine(pName,
                    "I must be careful. If the fog thickens, the very waters here will turn into deadly poison.",
                    pKey));
        }

        game.dialogueManager.startDialogue(lines);
    }
}
