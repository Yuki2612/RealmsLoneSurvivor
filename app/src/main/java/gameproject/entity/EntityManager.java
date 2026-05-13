package gameproject.entity;

import gameproject.*;
import gameproject.skill.PassiveSkill;
import gameproject.weapon.Projectile;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntityManager {
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public final java.util.List<HeartDrop> heartDrops = java.util.Collections
            .synchronizedList(new java.util.ArrayList<>());
    public ArrayList<ChestDrop> weaponChests = new ArrayList<>();
    public final java.util.List<ResourceDrop> resourceDrops = java.util.Collections
            .synchronizedList(new java.util.ArrayList<>());
    public final java.util.List<RuneItem> runeDrops = java.util.Collections
            .synchronizedList(new java.util.ArrayList<>());
    public ArrayList<EventTreasure> eventChests = new ArrayList<>();
    public int bossesKilled = 0;
    public static final int FINAL_WAVE = 25; // Giới hạn Wave để kết thúc game

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

    private long lastEnemySpawnTime;
    public int waveCount = 0;
    public int activeBossCount = 0; // Cập nhật an toàn để tránh crash music
    private long currentSpawnInterval = 10000;
    private int enemiesLeftInWave = 0; // Số quái còn lại của đợt 2 (dành cho wave >= 10)
    private long lastBurstTime = 0;
    private static final long BURST_INTERVAL = 8000; // Khoảng cách giữa 2 đợt quái (8 giây)

    public void startNewGame(long currentTime, int startingWave) {
        synchronized (enemies) {
            enemies.clear();
        }
        synchronized (projectiles) {
            projectiles.clear();
        }
        synchronized (heartDrops) {
            heartDrops.clear();
        }
        synchronized (runeDrops) {
            runeDrops.clear();
        }
        synchronized (weaponChests) {
            weaponChests.clear();
        }
        activeBossCount = 0;
        synchronized (resourceDrops) {
            resourceDrops.clear();
        }
        synchronized (eventChests) {
            eventChests.clear();
        }
        bossesKilled = 0;
        waveCount = startingWave - 1;
        currentSpawnInterval = 10000;
        lastEnemySpawnTime = currentTime - currentSpawnInterval;
        enemiesLeftInWave = 0;
        lastBurstTime = 0;
    }

    public void update(Player player, VFXManager vfxManager, List<PassiveSkill> activeSkills,
            int screenWidth, int screenHeight, long currentTime, int surviveTimeSeconds, GamePanel panel) {
        List<Enemy> pendingSummons = new ArrayList<>();

        // Đếm boss đang hoạt động (không dùng stream để tránh
        // ConcurrentModificationException)
        int bCount = 0;
        for (Enemy e : enemies) {
            if (e.isBoss && !e.isDying)
                bCount++;
        }
        activeBossCount = bCount;

        // 1. SINH QUÁI VÀ BOSS
        if ((enemies.isEmpty() || currentTime - lastEnemySpawnTime >= currentSpawnInterval) && waveCount < FINAL_WAVE
                && enemiesLeftInWave == 0) {
            waveCount++;
            // TĂNG SỐ LƯỢNG QUÁI: 5 + 2.0f mỗi wave (Kéo dài trải nghiệm chiến đấu)
            int totalWaveSize = 5 + (int) (waveCount * 2.0f);
            if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON &&
                    gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
                totalWaveSize = (int) (totalWaveSize * 1.5f);
            }

            if (waveCount >= 10) {
                // Cơ chế Phân đợt (Multi-Burst): Chia đôi quái, đợt 2 ra sau 8 giây
                int firstBurst = totalWaveSize / 2;
                enemiesLeftInWave = totalWaveSize - firstBurst;

                for (int i = 0; i < firstBurst; i++) {
                    Enemy newEnemy = spawnSafeEnemy(player, panel, surviveTimeSeconds);
                    synchronized (enemies) {
                        enemies.add(newEnemy);
                    }
                }
                lastBurstTime = currentTime;
            } else {
                // Wave thấp: Spawn 1 đợt duy nhất
                for (int i = 0; i < totalWaveSize; i++) {
                    Enemy newEnemy = spawnSafeEnemy(player, panel, surviveTimeSeconds);
                    synchronized (enemies) {
                        enemies.add(newEnemy);
                    }
                }
            }

            if (waveCount % 5 == 0) {
                int bossWave = waveCount / 5; // 1, 2, 3, 4, 5

                // Tìm vị trí an toàn cho Boss (Né vật cản và nằm trong map)
                float bossStartX = panel.cameraX + screenWidth / 2f;
                float bossStartY = panel.cameraY - 100f;

                for (int attempt = 0; attempt < 15; attempt++) {
                    float testX = panel.cameraX + (float) (Math.random() * screenWidth);
                    float testY = panel.cameraY - 150f;
                    if (Math.random() < 0.5)
                        testY = panel.cameraY + screenHeight + 150f;

                    testX = Math.max(0, Math.min(testX, GamePanel.WORLD_WIDTH - 100));
                    testY = Math.max(0, Math.min(testY, GamePanel.WORLD_HEIGHT - 100));

                    if (panel.mapManager.isNavigable((int) testX + 50, (int) testY + 50)) {
                        bossStartX = testX;
                        bossStartY = testY;
                        break;
                    }
                }

                synchronized (enemies) {
                    int bossToSpawn = bossWave;

                    // Nếu là wave cuối (25), ưu tiên dùng Boss của Map
                    if (bossWave == 5) {
                        bossToSpawn = panel.currentMapConfig.bossIndex;
                    }

                    Enemy boss;
                    switch (bossToSpawn) {
                        case 1 -> boss = new SoulReaper(bossStartX, bossStartY, surviveTimeSeconds);
                        case 2 -> {
                            if (panel.currentMapConfig.type == gameproject.environment.MapType.SWAMP) {
                                boss = new SwampCannonBoss(bossStartX, bossStartY, surviveTimeSeconds);
                            } else {
                                boss = new ShadowBoss(bossStartX, bossStartY, surviveTimeSeconds);
                            }
                        }
                        case 3 -> boss = new DarkFairy(bossStartX, bossStartY, surviveTimeSeconds);
                        case 4 -> {
                            if (panel.currentMapConfig.type == gameproject.environment.MapType.SWAMP) {
                                boss = new PriestBoss(bossStartX, bossStartY, surviveTimeSeconds);
                            } else {
                                boss = new KingBoss(bossStartX, bossStartY, surviveTimeSeconds);
                            }
                        }
                        case 5 -> boss = new PhantomWarlock(bossStartX, bossStartY, surviveTimeSeconds);
                        default -> boss = new SoulReaper(bossStartX, bossStartY, surviveTimeSeconds);
                    }

                    if (bossWave == 5) {
                        boss.isFinalBoss = true;
                    }

                    // Áp dụng hệ số HP theo Map ID cho Boss
                    float mapMultiplier = 1.0f + panel.currentMapConfig.mapId * 0.1f;
                    boss.maxHp = (int) (boss.maxHp * mapMultiplier);
                    boss.hp = boss.maxHp;

                    enemies.add(boss);
                }

                vfxManager.showWaveBanner("boss_incoming", "⚠  BOSS INCOMING!", new java.awt.Color(255, 80, 80),
                        currentTime);
            } else {
                vfxManager.showWaveBanner("wave_banner", "Wave " + waveCount, new java.awt.Color(255, 220, 80),
                        currentTime);
            }

            lastEnemySpawnTime = currentTime;
            // Kéo giãn nhịp độ Wave (15s - 45s) để đạt thời lượng game ~17 phút
            long interval = Math.min(15000 + ((waveCount - 1) * 3000), 45000);
            if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON &&
                    gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
                interval /= 1.5;
            }
            currentSpawnInterval = interval;
        }

        // Xử lý đợt quái thứ 2 (Second Burst) cho các wave cao
        if (enemiesLeftInWave > 0 && currentTime - lastBurstTime >= BURST_INTERVAL) {
            for (int i = 0; i < enemiesLeftInWave; i++) {
                Enemy newEnemy = spawnSafeEnemy(player, panel, surviveTimeSeconds);
                synchronized (enemies) {
                    enemies.add(newEnemy);
                }
            }
            enemiesLeftInWave = 0;
            vfxManager.showWaveBanner("⚠  REINFORCEMENTS ARRIVED!", new java.awt.Color(255, 100, 100), currentTime);
        }

        // 2. CẬP NHẬT KỸ NĂNG BỊ ĐỘNG
        for (PassiveSkill skill : activeSkills) {
            skill.update(player, enemies, vfxManager, currentTime);
        }

        // 3. XỬ LÝ ĐẠN (CẢ ĐẠN TA VÀ ĐẠN ĐỊCH)
        ArrayList<Projectile> pendingProjectiles = new ArrayList<>();
        ArrayList<Projectile> newEnemyProjectiles = new ArrayList<>();
        ArrayList<Enemy> newEnemies = new ArrayList<>();
        synchronized (projectiles) {
            Iterator<Projectile> pIt = projectiles.iterator();
            while (pIt.hasNext()) {
                Projectile p = pIt.next();
                p.update(GamePanel.WORLD_WIDTH, GamePanel.WORLD_HEIGHT);

                // THÊM: Va chạm với vật cản trên Map (Tường/Thùng gỗ)
                gameproject.environment.Obstacle obs = panel.mapManager.getObstacleAtWorld(p.getX(), p.getY());
                if (obs != null && obs.isSolid()) {
                    obs.takeDamage(p.damage);
                    if (obs.isDestroyed() && obs instanceof gameproject.environment.WoodenCrate) {
                        synchronized (resourceDrops) {
                            // WoodenCrates drop 2-5 gold and 5% chance for a soul
                            int goldToDrop = 2 + (int) (Math.random() * 8);
                            spawnResource(p.getX(), p.getY(), ResourceDrop.Type.GOLD, goldToDrop, currentTime, 15000);
                            if (Math.random() < 0.05) {
                                spawnResource(p.getX(), p.getY(), ResourceDrop.Type.SOUL, 1, currentTime, 30000);
                            }
                        }
                    }
                    p.setActive(false);
                    // Nếu đạn nổ của địch trúng tường, cho nổ luôn
                    if (p.isEnemyBullet && p.isExplosive) {
                        handleExplosiveEnemyBullet(p, player, vfxManager, currentTime, panel);
                    }
                }

                // Kiểm tra đạn nổ của địch khi hết tầm bay
                if (!p.isActive() && p.isEnemyBullet && p.isExplosive) {
                    handleExplosiveEnemyBullet(p, player, vfxManager, currentTime, panel);
                    pIt.remove();
                    continue;
                }

                if (!p.isActive()) {
                    pIt.remove();
                    continue;
                }

                boolean hit = false;

                // Phân luồng: Đạn địch bắn vào Player
                if (p.isEnemyBullet) {
                    if (!player.isDashing() && !player.isInvulnerable()
                            && p.getBounds().intersects(player.getBounds())) {
                        if (p.isExplosive) {
                            handleExplosiveEnemyBullet(p, player, vfxManager, currentTime, panel);
                        } else {
                            if (player.takeHit()) {
                                panel.triggerGameOver();
                            } else {
                                vfxManager.triggerScreenShake(15);
                            }
                        }
                        p.setActive(false);
                        hit = true;
                    }
                }
                // Phân luồng: Đạn Player bắn vào Địch
                else {
                    if (p.isRailgun) {
                        if (p.bouncesLeft == 1) {
                            p.bouncesLeft = 0;
                            float dx = p.speedX;
                            float dy = p.speedY;
                            float len = (float) Math.sqrt(dx * dx + dy * dy);
                            if (len == 0)
                                len = 1;
                            float dirX = dx / len;
                            float dirY = dy / len;

                            float endX = p.startX + dirX * p.maxRange;
                            float endY = p.startY + dirY * p.maxRange;

                            // --- Railgun Environment Interaction ---
                            float currentEndX = endX;
                            float currentEndY = endY;

                            // Check for obstacles along the beam path
                            int steps = 20;
                            for (int s = 0; s <= steps; s++) {
                                float checkX = p.startX + (dirX * p.maxRange * s / steps);
                                float checkY = p.startY + (dirY * p.maxRange * s / steps);
                                gameproject.environment.Obstacle wallObs = panel.mapManager.getObstacleAtWorld(checkX,
                                        checkY);
                                if (wallObs != null && wallObs.isSolid()) {
                                    currentEndX = checkX;
                                    currentEndY = checkY;
                                    wallObs.takeDamage(p.damage);
                                    break;
                                }
                            }

                            vfxManager.addLaser(p.startX, p.startY, currentEndX, currentEndY, currentTime);

                            synchronized (enemies) {
                                for (Enemy e : enemies) {
                                    if (!e.isDead()
                                            && distanceToLineSegment(e.getX() + e.size / 2, e.getY() + e.size / 2,
                                                    p.startX, p.startY, currentEndX, currentEndY) <= 40) {
                                        e.takeDamageDirect(p.damage, p.isCrit, vfxManager, currentTime);
                                        if (p.isCrit)
                                            player.triggerFrenzy();
                                        for (PassiveSkill skill : activeSkills) {
                                            skill.onProjectileHit(p, e, player, vfxManager, currentTime);
                                        }
                                    }
                                }
                            }
                        }
                        p.setActive(false);
                        hit = true;
                    } else {
                        synchronized (enemies) {
                            for (Enemy e : enemies) {
                                if (e != p.ignoredEnemy && p.getBounds().intersects(e.getBounds())) {
                                    e.takeDamageDirect(p.damage, p.isCrit, vfxManager, currentTime);
                                    if (p.isCrit)
                                        player.triggerFrenzy();
                                    for (PassiveSkill skill : activeSkills) {
                                        skill.onProjectileHit(p, e, player, vfxManager, currentTime);
                                    }
                                    if (p.isShocking) {
                                        e.applyShock(1000, vfxManager, enemies);
                                    }
                                    p.setActive(false);

                                    if (p.bouncesLeft > 0) {
                                        float soulMulti = 1.0f + (gameproject.meta.PlayerData.skillSoulLevels
                                                .getOrDefault(gameproject.skill.Upgrade.CHAIN_LIGHTNING, 0) * 0.05f);
                                        // Chain Lightning Range (Lv1: 250px Lv5: 650px)
                                        float maxRange = (150.0f
                                                + (player
                                                        .getBreakthroughLevel(gameproject.skill.Upgrade.CHAIN_LIGHTNING)
                                                        * 100))
                                                * soulMulti;
                                        Enemy closest = getClosestEnemy(e, enemies, maxRange);
                                        if (closest != null) {
                                            Projectile bounceProj = new Projectile(e.getX(), e.getY(), closest.getX(),
                                                    closest.getY(),
                                                    1.5f, 300f);
                                            bounceProj.isShocking = true;
                                            bounceProj.damage = (int) ((Math.max(1, p.damage / 5)
                                                    // Lv1: +4 Lv5: +20
                                                    + (player.getBreakthroughLevel(
                                                            gameproject.skill.Upgrade.CHAIN_LIGHTNING)
                                                            * 4))
                                                    * soulMulti);
                                            bounceProj.bouncesLeft = p.bouncesLeft - 1;
                                            bounceProj.ignoredEnemy = e;
                                            pendingProjectiles.add(bounceProj);
                                        }
                                    }
                                    hit = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (hit)
                    pIt.remove();
            }
        }
        synchronized (projectiles) {
            projectiles.addAll(pendingProjectiles);
        }

        // 4. XỬ LÝ QUÁI VẬT DI CHUYỂN, BẮN ĐẠN VÀ VA CHẠM
        // Giảm hệ số tăng tốc độ (0.12f -> 0.05f) để quái không quá nhanh ở wave cao
        // (Wave 15+)
        float speedMultiplier = 1.0f + (surviveTimeSeconds / 60) * 0.05f;
        synchronized (enemies) {
            // Duyệt qua bản sao để tránh ConcurrentModificationException khi đang loop
            for (Enemy enemy : new ArrayList<>(enemies)) {

                if (enemy.isDying) {
                    // 1. RƠI ĐỒ NGAY LẬP TỨC KHI VỪA CHẾT (Không đợi fade xong)
                    if (!enemy.rewardDropped) {
                        enemy.rewardDropped = true;

                        if (enemy.isBoss) {
                            boolean isRare = (bossesKilled == 0);
                            bossesKilled++;
                            synchronized (weaponChests) {
                                weaponChests
                                        .add(new ChestDrop(enemy.getX(), enemy.getY(), isRare, currentTime + 300000));
                            }
                            // Rơi tài nguyên Boss
                            int goldAmount = 200 + (waveCount / 5) * 100;
                            int soulAmount = 20 + (waveCount / 5) * 5;
                            synchronized (resourceDrops) {
                                spawnResource(enemy.getX(), enemy.getY(), ResourceDrop.Type.GOLD, goldAmount,
                                        currentTime, 45000);
                                spawnResource(enemy.getX(), enemy.getY(), ResourceDrop.Type.SOUL, soulAmount,
                                        currentTime, 60000);
                            }
                            // Rơi tim Boss
                            synchronized (heartDrops) {
                                heartDrops.add(new HeartDrop(enemy.getX(), enemy.getY(), currentTime + 20000));
                                heartDrops.add(new HeartDrop(enemy.getX() + 30, enemy.getY(), currentTime + 20000));
                            }
                        } else {
                            // Rơi tài nguyên quái thường
                            float dropChance = 0.35f;
                            int goldBase = 2 + (int) (Math.random() * 8);
                            int lootMult = 1;
                            if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON
                                    &&
                                    gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
                                dropChance = 0.5f;
                                lootMult = 2;
                            }
                            if (enemy.isElite) {
                                dropChance = 1.0f; // Elite luôn rơi đồ
                                goldBase *= 3; // Gold x3
                                // Luôn rơi 1 Soul
                                synchronized (resourceDrops) {
                                    spawnResource(enemy.getX(), enemy.getY(), ResourceDrop.Type.SOUL, 1,
                                            currentTime, 45000);
                                }
                            }

                            if (Math.random() < dropChance) {
                                synchronized (resourceDrops) {
                                    spawnResource(enemy.getX(), enemy.getY(), ResourceDrop.Type.GOLD,
                                            goldBase * lootMult, currentTime, 20000);
                                    if (!enemy.isElite && Math.random() < 0.1)
                                        spawnResource(enemy.getX(), enemy.getY(), ResourceDrop.Type.SOUL, 1,
                                                currentTime, 30000);
                                }
                            }
                            // Tỉ lệ rơi tim quái thường
                            if (Math.random() < 0.005) {
                                synchronized (heartDrops) {
                                    heartDrops.add(new HeartDrop(enemy.getX(), enemy.getY(), currentTime + 10000));
                                }
                            }
                        }

                        if (enemy.triggerCorrosiveMelt) {
                            vfxManager.addAcidZone(enemy.getX(), enemy.getY(), 80, currentTime);
                        }

                        // Trigger skills on death
                        for (PassiveSkill skill : activeSkills) {
                            skill.onEnemyDeath(enemy, player, enemies, vfxManager, currentTime);
                        }
                        player.getComboManager().onEnemyKilled(enemy.isBoss);

                    }

                    // 2. CẬP NHẬT HOẠT ẢNH CHẾT (Đối với Boss có death animation)
                    enemy.update(player.getX(), player.getY(), speedMultiplier, enemies, screenWidth, screenHeight,
                            panel);

                    // 3. XÓA KHỎI DANH SÁCH KHI HẾT ANIMATION
                    if (enemy.shouldRemove()) {
                        // Trứng không cho EXP
                        if (!(enemy instanceof EggEntity)) {
                            // Lượng EXP gốc (Tương đương maxHp)
                            int baseExp = enemy.getExpValue();
                            if (enemy.isElite)
                                baseExp *= 5; // BƯỚC 2: Elite x5 EXP

                            // Cộng Bonus theo Wave
                            int finalExp = baseExp + (waveCount * 3);

                            panel.addScoreAndExp(finalExp);
                            SoundManager.play("hit");
                        }

                        // Tích hợp Thành tựu
                        if (!enemy.isBoss) {
                            gameproject.meta.AchievementManager.getInstance().addKill();
                        } else {
                            gameproject.meta.AchievementManager.getInstance().onBossKilled(enemy.getName());
                            
                            if (enemy.isFinalBoss) {
                                synchronized (enemies) {
                                    enemies.clear();
                                }
                            }

                            checkRuneDrop(enemy, panel, currentTime, player, vfxManager);
                        }

                        vfxManager.spawnDeathParticles(enemy.getX() + enemy.size / 2f,
                                enemy.getY() + enemy.size / 2f, currentTime,
                                enemy.isBoss ? new java.awt.Color(255, 80, 80) : enemy.color);

                        // Xử lý Affix EXPLOSIVE: Nổ khi chết
                        if (enemy.isElite && enemy.eliteAffix == Enemy.EliteAffix.EXPLOSIVE) {
                            float dx = (player.getX() + Player.SIZE / 2) - (enemy.getX() + enemy.size / 2);
                            float dy = (player.getY() + Player.SIZE / 2) - (enemy.getY() + enemy.size / 2);
                            float dist = (float) Math.sqrt(dx * dx + dy * dy);
                            if (dist < 120) {
                                player.takeDamage(1);
                            }
                            vfxManager.triggerScreenShake(5);
                            SoundManager.play("explosion");
                        }

                        synchronized (enemies) {
                            enemies.remove(enemy);
                        }
                    }
                    continue; // Không cho quái đang chết di chuyển vật lý hay nhận thêm damage
                }

                // KHÔI PHỤC TÍNH TOÁN KHOẢNG CÁCH
                float dx = (player.getX() + player.SIZE / 2f) - (enemy.getX() + enemy.size / 2f);
                float dy = (player.getY() + player.SIZE / 2f) - (enemy.getY() + enemy.size / 2f);
                float distSq = dx * dx + dy * dy;

                float currentEnemySpeedMulti = speedMultiplier;
                if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON &&
                        gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE
                        && !enemy.isBoss) {
                    currentEnemySpeedMulti *= 1.25f;
                }

                if (enemy.chillEndTime > currentTime)
                    currentEnemySpeedMulti *= 0.7f;
                if (enemy.inAcidZone)
                    currentEnemySpeedMulti *= 0.5f;

                enemy.playerDamageCache = panel.upgradeManager.playerDamage;
                enemy.updateStatusEffects(currentTime, vfxManager, panel);
                enemy.inAcidZone = false;

                // --- Xử lý Affix SPLITTING: Tách đôi khi HP < 50% ---
                if (enemy.isElite && enemy.eliteAffix == Enemy.EliteAffix.SPLITTING && !enemy.hasSplit
                        && enemy.hp < enemy.maxHp / 2) {
                    enemy.hasSplit = true;
                    vfxManager.spawnDeathParticles(enemy.x + enemy.size / 2, enemy.y + enemy.size / 2, currentTime,
                            java.awt.Color.MAGENTA);
                    SoundManager.play("explosion");

                    for (int i = 0; i < 2; i++) {
                        float offX = (i == 0) ? -25 : 25;
                        Enemy child;
                        if (enemy instanceof ShooterEnemy)
                            child = new ShooterEnemy(enemy.x + offX, enemy.y, 3, surviveTimeSeconds);
                        else if (enemy instanceof AssassinEnemy)
                            child = new AssassinEnemy(enemy.x + offX, enemy.y, 3, surviveTimeSeconds);
                        else if (enemy instanceof WizardEnemy)
                            child = new WizardEnemy(enemy.x + offX, enemy.y, 3, surviveTimeSeconds);
                        else
                            child = new NormalEnemy(enemy.x + offX, enemy.y, 3, surviveTimeSeconds,
                                    panel.currentMapConfig.mapId);

                        child.isElite = false; // Đệ tử không phải Elite
                        child.size = (int) (enemy.size * 0.7f);
                        child.maxHp = (int) (enemy.maxHp * 0.4f);
                        child.hp = child.maxHp;
                        pendingSummons.add(child);
                    }
                }

                if (enemy.freezeEndTime <= currentTime) {
                    enemy.update(player.getX(), player.getY(), currentEnemySpeedMulti, enemies, GamePanel.WORLD_WIDTH,
                            GamePanel.WORLD_HEIGHT, panel);
                }

                java.util.List<Projectile> enemyProjs = enemy.shoot();
                if (enemyProjs != null) {
                    newEnemyProjectiles.addAll(enemyProjs);
                }

                java.util.List<Enemy> summoned = enemy.summon();
                if (summoned != null) {
                    newEnemies.addAll(summoned);
                }

                // TĂNG TẦM VỚI TẤN CÔNG (Attack Reach)
                float attackReach = (enemy.size + player.SIZE) * 0.95f;

                if (!player.isDashing() && !player.isInvulnerable() && distSq < attackReach * attackReach
                        && !enemy.isDying && !(enemy instanceof EggEntity)) {
                    if (player.takeHit()) {
                        panel.triggerGameOver();
                    } else {
                        vfxManager.triggerScreenShake(15);
                        vfxManager.triggerPlayerDamageFlash(currentTime);
                        for (Enemy e : enemies)
                            e.applyKnockback(player.getX(), player.getY(), 40f);
                    }
                    break;
                }
            }
        }
        // Đưa toàn bộ đạn mới của địch vào luồng đạn chính
        synchronized (projectiles) {
            projectiles.addAll(newEnemyProjectiles);
        }
        synchronized (enemies) {
            enemies.addAll(newEnemies);
        }

        // XỬ LÝ VẬT PHẨM (RESOURCE DROPS)
        synchronized (resourceDrops) {
            Iterator<ResourceDrop> rdIt = resourceDrops.iterator();
            while (rdIt.hasNext()) {
                ResourceDrop rd = rdIt.next();
                if (currentTime > rd.expireTime) {
                    rdIt.remove();
                } else {
                    rd.update(player.getX() + player.SIZE / 2, player.getY() + player.SIZE / 2);
                    if (rd.isCollected(player.getX() + player.SIZE / 2, player.getY() + player.SIZE / 2)) {
                        rd.applyToPlayer();
                        gameproject.SoundManager.play("pickup");
                        rdIt.remove();
                    }
                }
            }
        }

        // 5. XỬ LÝ VẬT PHẨM (MÁU, RƯƠNG)
        synchronized (heartDrops) {
            Iterator<HeartDrop> hIt = heartDrops.iterator();
            while (hIt.hasNext()) {
                HeartDrop hd = hIt.next();
                if (currentTime > hd.expireTime)
                    hIt.remove();
                else if (player.getBounds().intersects(new Rectangle((int) hd.x, (int) hd.y, 15, 15))) {
                    player.addHeart();
                    hIt.remove();
                }
            }
        }

        // 6. XỬ LÝ RUNE (THU THẬP)
        synchronized (runeDrops) {
            Iterator<RuneItem> rIt = runeDrops.iterator();
            while (rIt.hasNext()) {
                RuneItem ri = rIt.next();
                if (player.getBounds().intersects(new Rectangle((int) ri.x - 10, (int) ri.y - 10, 20, 20))) {
                    player.collectRune(ri);
                    rIt.remove();
                }
            }
        }

        synchronized (vfxManager.fireZones) {
            for (VFXManager.FireZone fz : vfxManager.fireZones) {
                if (fz.isAcid) {
                    Rectangle acidBox = new Rectangle((int) fz.x, (int) fz.y, fz.radius, fz.radius);
                    synchronized (enemies) {
                        for (Enemy e : enemies) {
                            if (e.getBounds().intersects(acidBox)) {
                                e.inAcidZone = true;
                                e.applyPoison(500);
                            }
                        }
                    }
                }
                if (fz.isToxic) {
                    // Hiệu ứng hạt bay lên cho bãi độc
                    if (currentTime % 5 == 0) {
                        vfxManager.spawnToxicSmoke(fz.x + (float)Math.random() * fz.radius, fz.y + (float)Math.random() * fz.radius, currentTime);
                    }
                    
                    // Gây sát thương cho người chơi mỗi 1 giây
                    Rectangle toxicBox = new Rectangle((int) fz.x, (int) fz.y, fz.radius, fz.radius);
                    if (player.getBounds().intersects(toxicBox)) {
                        if (currentTime % 1000 < 20) { 
                             if (player.takeHit()) {
                                panel.triggerGameOver();
                             }
                        }
                    }
                }
            }
        }

        synchronized (weaponChests) {
            Iterator<ChestDrop> cIt = weaponChests.iterator();
            while (cIt.hasNext()) {
                ChestDrop chest = cIt.next();
                if (currentTime > chest.expirationTime) {
                    cIt.remove();
                    continue;
                }
                if (player.getBounds().intersects(chest.getBounds())) {
                    if (chest.isRare) {
                        panel.openWeaponSelect();
                    } else {
                        panel.triggerBreakthroughUpgrade();
                    }
                    if (Math.random() < 0.05) { // 5% chance for "Jackpot!"
                        gameproject.meta.AchievementManager.getInstance().onTripleChest();
                    }
                    cIt.remove();
                }
            }
        }

        // Xử lý Event Treasure
        synchronized (eventChests) {
            Iterator<EventTreasure> etIt = eventChests.iterator();
            while (etIt.hasNext()) {
                EventTreasure et = etIt.next();
                if (player.getBounds().intersects(et.getBounds())) {
                    et.interact(panel);
                    etIt.remove();
                }
            }
        }

        // KIỂM TRA CHIẾN THẮNG: Đã được chuyển sang PlayingState để theo dõi No-Hit
        // if (waveCount >= FINAL_WAVE) { ... }

        // 7. THÊM QUÁI VẬT TỪ CHIÊU THỨC/AFFIX
        if (!pendingSummons.isEmpty()) {
            synchronized (enemies) {
                enemies.addAll(pendingSummons);
            }
        }
    }

    // HÀM HỖ TRỢ: Xử lý đạn nổ của Pháo thủ
    private void handleExplosiveEnemyBullet(Projectile p, Player player, VFXManager vfxManager, long currentTime,
            GamePanel panel) {
        vfxManager.addExplosion(p.getX(), p.getY(), p.explosionRadius, currentTime);
        vfxManager.triggerScreenShake(10);
        SoundManager.play("explosion");

        float distToPlayer = (float) Math
                .sqrt(Math.pow(p.getX() - player.getX(), 2) + Math.pow(p.getY() - player.getY(), 2));
        if (distToPlayer <= p.explosionRadius && !player.isDashing() && !player.isInvulnerable()) {
            if (player.takeHit()) {
                panel.triggerGameOver();
            }
        }
    }

    public Enemy spawnSafeEnemy(Player player, GamePanel panel, int surviveTimeSeconds) {
        Random rand = new Random();
        float ex = 0, ey = 0;

        float camX = panel.cameraX;
        float camY = panel.cameraY;
        int sw = panel.screenWidth;
        int sh = panel.screenHeight;

        // Thử tìm vị trí an toàn (không vật cản và có đường đi) tối đa 10 lần
        for (int attempt = 0; attempt < 10; attempt++) {
            // BƯỚC 3: Spawn quái ở xa hơn (500-800px) để người chơi có không gian thở
            int spawnOffset = 500 + rand.nextInt(300);
            int side = rand.nextInt(4);
            if (side == 0) { // Top
                ex = camX + rand.nextInt(sw);
                ey = camY - spawnOffset;
            } else if (side == 1) { // Bottom
                ex = camX + rand.nextInt(sw);
                ey = camY + sh + spawnOffset;
            } else if (side == 2) { // Left
                ex = camX - spawnOffset;
                ey = camY + rand.nextInt(sh);
            } else { // Right
                ex = camX + sw + spawnOffset;
                ey = camY + rand.nextInt(sh);
            }

            // Clamp inside WORLD
            ex = Math.max(0, Math.min(ex, GamePanel.WORLD_WIDTH - 30));
            ey = Math.max(0, Math.min(ey, GamePanel.WORLD_HEIGHT - 30));

            // Nếu vị trí này có thể đi được, có đường tới Player, và KHÔNG PHẢI cửa, chấp
            // nhận luôn
            if (panel.mapManager.isNavigable((int) ex + 15, (int) ey + 15)
                    && !panel.mapManager.isEntrance((int) ex + 15, (int) ey + 15)) {
                break;
            }
        }

        // Kéo dãn tiến trình: đỉnh điểm tier 5 sẽ rơi vào khoảng Wave 20-25
        int minTier = Math.min(5, (waveCount / 6) + 1);
        int maxTier = Math.min(5, (waveCount / 4) + 2);
        maxTier = Math.max(minTier, maxTier); // Đảm bảo an toàn
        int spawnTier = rand.nextInt((maxTier - minTier) + 1) + minTier;

        // Bắt đầu trộn các loại quái mới dựa trên cấu hình Map
        Enemy e;
        String typeKey = "normal";
        List<String> pool = panel.currentMapConfig.normalEnemies;
        if (!pool.isEmpty()) {
            typeKey = pool.get(rand.nextInt(pool.size()));
        }

        if (waveCount >= 3) {
            switch (typeKey) {
                case "shooter" -> e = new ShooterEnemy(ex, ey, spawnTier, surviveTimeSeconds);
                case "assassin" -> e = new AssassinEnemy(ex, ey, spawnTier, surviveTimeSeconds);
                case "spawner" ->
                    e = new SpawnerEnemy(ex, ey, spawnTier, surviveTimeSeconds, panel.currentMapConfig.mapId);
                case "wizard" -> {
                    if (waveCount >= 5)
                        e = new WizardEnemy(ex, ey, spawnTier, surviveTimeSeconds);
                    else
                        e = new NormalEnemy(ex, ey, spawnTier, surviveTimeSeconds, panel.currentMapConfig.mapId);
                }
                default -> e = new NormalEnemy(ex, ey, spawnTier, surviveTimeSeconds, panel.currentMapConfig.mapId);
            }
        } else {
            e = new NormalEnemy(ex, ey, spawnTier, surviveTimeSeconds, panel.currentMapConfig.mapId);
        }

        // BƯỚC 2: Logic Quái Tinh Anh (Elite)
        if (waveCount >= 5 && !e.isBoss) {
            // Tỉ lệ spawn tăng dần từ 5% lên 15% (giảm so với plan gốc để cân bằng với max
            // cap 4)
            double eliteChance = 0.05 + (Math.min(waveCount, 20) / 20.0) * 0.10;

            // Giới hạn tối đa 4 quái Elite trên màn hình
            int currentElites = 0;
            synchronized (enemies) {
                for (Enemy active : enemies) {
                    if (active.isElite && !active.isDead())
                        currentElites++;
                }
            }

            if (currentElites < 4 && Math.random() < eliteChance) {
                e.isElite = true;
                e.maxHp *= 3; // Máu trâu gấp 3
                e.hp = e.maxHp;
                e.size = (int) (e.size * 1.4f); // To hơn 40%
                e.speed *= 0.85f; // Chậm hơn 15%

                // Gắn Affix ngẫu nhiên
                Enemy.EliteAffix[] affixes = Enemy.EliteAffix.values();
                e.eliteAffix = affixes[rand.nextInt(affixes.length)];
                if (e.eliteAffix == Enemy.EliteAffix.SHIELDED) {
                    e.shieldHits = 3;
                }
            }
        }

        // Áp dụng hệ số HP theo Map ID (Ví dụ: Map 1 thì máu x1.1)
        float mapMultiplier = 1.0f + panel.currentMapConfig.mapId * 0.1f;
        e.maxHp = (int) (e.maxHp * mapMultiplier);
        e.hp = e.maxHp;

        return e;
    }

    private Enemy getClosestEnemy(Enemy source, ArrayList<Enemy> allEnemies, float maxDist) {
        Enemy closest = null;
        float minDist = maxDist;
        synchronized (allEnemies) {
            for (Enemy other : allEnemies) {
                if (other == source || other.isDead())
                    continue;
                float dist = (float) Math
                        .sqrt(Math.pow(other.getX() - source.getX(), 2) + Math.pow(other.getY() - source.getY(), 2));
                if (dist < minDist) {
                    minDist = dist;
                    closest = other;
                }
            }
        }
        return closest;
    }

    private void checkRuneDrop(Enemy boss, GamePanel panel, long currentTime, Player player, VFXManager vfxManager) {
        // LOGIC RUNE CHO SWAMP: Rơi Rune sau khi hạ gục Boss 1, 2, 3, 4
        if (panel.currentMapConfig.type == gameproject.environment.MapType.SWAMP) {
            // Sử dụng bossesKilled (đã được tăng ở line 384 ngay khi boss bắt đầu chết)
            if (bossesKilled >= 1 && bossesKilled <= 4) {
                float rx, ry;
                int maxAttempts = 100; // Tăng số lần thử tìm vị trí an toàn
                int attempts = 0;
                do {
                    rx = (float) (Math.random() * (GamePanel.WORLD_WIDTH - 600) + 300);
                    ry = (float) (Math.random() * (GamePanel.WORLD_HEIGHT - 600) + 300);
                    attempts++;
                } while (attempts < maxAttempts && (!panel.mapManager.isNavigable((int) rx, (int) ry)
                        || Math.sqrt(Math.pow(rx - player.getX(), 2)
                                + Math.pow(ry - player.getY(), 2)) < 400));

                synchronized (runeDrops) {
                    runeDrops.add(new RuneItem(rx, ry, currentTime + 1200000)); // Tồn tại 20 phút
                }

                // Hiển thị thông báo rõ ràng cho người chơi
                vfxManager.showWaveBanner("rune_drop", "A MYSTERIOUS RUNE HAS APPEARED. BRING IT TO THE ALTAR!",
                        java.awt.Color.CYAN, currentTime);
            }
        }
    }

    public void drawGroundItems(Graphics g) {
        synchronized (weaponChests) {
            for (ChestDrop c : weaponChests) {
                long remaining = c.expirationTime - GamePanel.getTickTime();
                if (remaining < 3000 && (remaining / 150) % 2 == 0)
                    continue;

                int dx = (int) Math.round(c.x);
                int dy = (int) Math.round(c.y);
                java.awt.image.BufferedImage img = gameproject.ImageManager.get(c.isRare ? "chest2" : "chest1");
                if (img != null) {
                    g.drawImage(img, dx, dy, 80, 80, null);
                } else {
                    g.setColor(c.isRare ? java.awt.Color.MAGENTA : java.awt.Color.ORANGE);
                    g.fillRect(dx, dy, 80, 80);
                    g.setColor(java.awt.Color.WHITE);
                    g.drawString(c.isRare ? "RARE" : "CHEST", dx - 5, dy - 5);
                }
            }
        }

        synchronized (eventChests) {
            for (EventTreasure et : eventChests) {
                et.draw((java.awt.Graphics2D) g);
            }
        }

        synchronized (heartDrops) {
            for (HeartDrop hd : heartDrops) {
                long remaining = hd.expireTime - GamePanel.getTickTime();
                if (remaining < 3000 && (remaining / 150) % 2 == 0)
                    continue;

                int hdx = (int) Math.round(hd.x);
                int hdy = (int) Math.round(hd.y);
                java.awt.image.BufferedImage heartImg = gameproject.ImageManager.get("heart");
                if (heartImg != null) {
                    g.drawImage(heartImg, hdx - 2, hdy - 2, 20, 20, null);
                } else {
                    g.setColor(java.awt.Color.PINK);
                    g.fillRect(hdx, hdy, 15, 15);
                }
            }
        }

        synchronized (runeDrops) {
            for (RuneItem ri : runeDrops) {
                int rx = (int) Math.round(ri.x);
                int ry = (int) Math.round(ri.y);
                java.awt.image.BufferedImage runeImg = gameproject.ImageManager.get("rune");
                if (runeImg != null) {
                    g.drawImage(runeImg, rx - 20, ry - 20, 40, 40, null);
                } else {
                    g.setColor(java.awt.Color.CYAN);
                    g.fillOval(rx - 10, ry - 10, 20, 20);
                }
            }
        }

        synchronized (resourceDrops) {
            for (ResourceDrop rd : resourceDrops) {
                rd.draw(g);
            }
        }
    }

    public void drawProjectiles(Graphics g) {
        synchronized (projectiles) {
            for (Projectile p : projectiles) {
                p.draw(g);
            }
        }
    }

    private float distanceToLineSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (l2 == 0)
            return (float) Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        float t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        float projX = x1 + t * (x2 - x1);
        float projY = y1 + t * (y2 - y1);
        return (float) Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }

    public void spawnResource(float x, float y, ResourceDrop.Type type, int amount, long currentTime, long duration) {
        int cap = (type == ResourceDrop.Type.GOLD) ? 100 : 10;
        int remaining = amount;
        synchronized (resourceDrops) {
            while (remaining > 0) {
                int toSpawn = Math.min(remaining, cap);
                resourceDrops.add(new ResourceDrop(x, y, type, toSpawn, currentTime + duration));
                remaining -= toSpawn;
            }
        }
    }
}