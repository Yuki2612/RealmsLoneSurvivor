package gameproject.entity;

import gameproject.*;
import gameproject.skill.PassiveSkill;
import gameproject.meta.PlayerData;
import gameproject.skill.Upgrade;
import gameproject.weapon.Projectile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntityManager {
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public ArrayList<HeartDrop> heartDrops = new ArrayList<>();
    public ArrayList<ChestDrop> weaponChests = new ArrayList<>();
    public ArrayList<ResourceDrop> resourceDrops = new ArrayList<>();
    public ArrayList<EventTreasure> eventChests = new ArrayList<>();
    public int bossesKilled = 0;
    public static final int FINAL_WAVE = 15; // Giới hạn Wave để kết thúc game (The King)

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

    private long lastEnemySpawnTime;
    public int waveCount = 0;
    public int activeBossCount = 0; // Cập nhật an toàn để tránh crash music
    private long currentSpawnInterval = 10000;

    public void startNewGame(long currentTime, int startingWave) {
        enemies.clear();
        projectiles.clear();
        heartDrops.clear();
        weaponChests.clear();
        activeBossCount = 0;
        synchronized (resourceDrops) {
            resourceDrops.clear();
        }
        eventChests.clear();
        bossesKilled = 0;
        waveCount = startingWave - 1;
        currentSpawnInterval = 10000;
        lastEnemySpawnTime = currentTime - currentSpawnInterval;
    }

    public void update(Player player, VFXManager vfxManager, List<PassiveSkill> activeSkills,
            int screenWidth, int screenHeight, long currentTime, int surviveTimeSeconds, GamePanel panel) {

        // Đếm boss đang hoạt động (không dùng stream để tránh
        // ConcurrentModificationException)
        int bCount = 0;
        for (Enemy e : enemies) {
            if (e.isBoss && !e.isDying)
                bCount++;
        }
        activeBossCount = bCount;

        // 1. SINH QUÁI VÀ BOSS
        if ((enemies.isEmpty() || currentTime - lastEnemySpawnTime >= currentSpawnInterval) && waveCount < FINAL_WAVE) {
            waveCount++;
            int waveSize = 2 + waveCount;
            if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON &&
                    gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
                waveSize = (int) (waveSize * 1.5f);
            }
            for (int i = 0; i < waveSize; i++) {
                Enemy newEnemy = spawnSafeEnemy(player, panel, surviveTimeSeconds);
                synchronized (enemies) {
                    enemies.add(newEnemy);
                }
            }

            if (waveCount % 5 == 0) {
                int bType = (waveCount / 5) % 3;
                if (bType == 0)
                    bType = 3;

                // Tìm vị trí an toàn cho Boss (Né vật cản và nằm trong map)
                float bossStartX = panel.cameraX + screenWidth / 2f;
                float bossStartY = panel.cameraY - 100f;

                for (int attempt = 0; attempt < 15; attempt++) {
                    float testX = panel.cameraX + (float) (Math.random() * screenWidth);
                    float testY = panel.cameraY - 150f; // Boss tới từ phía trên
                    if (Math.random() < 0.5)
                        testY = panel.cameraY + screenHeight + 150f; // Hoặc từ phía dưới

                    testX = Math.max(0, Math.min(testX, GamePanel.WORLD_WIDTH - 100));
                    testY = Math.max(0, Math.min(testY, GamePanel.WORLD_HEIGHT - 100));

                    if (panel.mapManager.isNavigable((int) testX + 50, (int) testY + 50)) {
                        bossStartX = testX;
                        bossStartY = testY;
                        break;
                    }
                }

                synchronized (enemies) {
                    if (bType == 1)
                        enemies.add(new SoulReaper(bossStartX, bossStartY, surviveTimeSeconds));
                    else if (bType == 2)
                        enemies.add(new ShadowBoss(bossStartX, bossStartY, surviveTimeSeconds));
                    else
                        enemies.add(new KingBoss(bossStartX, bossStartY, surviveTimeSeconds));
                }

                vfxManager.showWaveBanner("⚠  BOSS INCOMING!", new java.awt.Color(255, 80, 80), currentTime);
            } else {
                vfxManager.showWaveBanner("Wave " + waveCount, new java.awt.Color(255, 220, 80), currentTime);
            }

            lastEnemySpawnTime = currentTime;
            long interval = Math.min(10000 + ((waveCount - 1) * 5000), 25000);
            if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON &&
                    gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
                interval /= 1.5;
            }
            currentSpawnInterval = interval;
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
                            int goldToDrop = 2 + (int) (Math.random() * 4);
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
                                        e.takeDamage(p.damage, p.isCrit, vfxManager, currentTime);
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
                                    e.takeDamage(p.damage, p.isCrit, vfxManager, currentTime);
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
        float speedMultiplier = 1.0f + (surviveTimeSeconds / 60) * 0.12f;
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
                            int goldAmount = 100 + (waveCount / 5) * 50;
                            int soulAmount = 5 + (waveCount / 5) * 3;
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
                            float dropChance = 0.25f;
                            int goldBase = 1 + (int) (Math.random() * 2);
                            int lootMult = 1;
                            if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON
                                    &&
                                    gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
                                dropChance = 0.5f;
                                lootMult = 2;
                            }
                            if (Math.random() < dropChance) {
                                synchronized (resourceDrops) {
                                    spawnResource(enemy.getX(), enemy.getY(), ResourceDrop.Type.GOLD,
                                            goldBase * lootMult, currentTime, 20000);
                                    if (Math.random() < 0.1)
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
                    enemy.update(player.getX(), player.getY(), speedMultiplier, enemies, screenWidth, screenHeight, panel);

                    // 3. XÓA KHỎI DANH SÁCH KHI HẾT ANIMATION
                    if (enemy.shouldRemove()) {
                        panel.addScoreAndExp(enemy.getMaxHp());
                        SoundManager.play("hit");
                        vfxManager.spawnDeathParticles(enemy.getX() + enemy.size / 2f,
                                enemy.getY() + enemy.size / 2f, currentTime,
                                enemy.isBoss ? new java.awt.Color(255, 80, 80) : enemy.color);

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
                enemy.updateStatusEffects(currentTime, vfxManager);
                enemy.inAcidZone = false;

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
                        && !enemy.isDying) {
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

        // KIỂM TRA CHIẾN THẮNG: Dọn sạch toàn bộ quái sau Wave cuối
        if (waveCount >= FINAL_WAVE && enemies.isEmpty()) {
            panel.triggerVictory();
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

    private Enemy spawnSafeEnemy(Player player, GamePanel panel, int surviveTimeSeconds) {
        Random rand = new Random();
        float ex = 0, ey = 0;

        float camX = panel.cameraX;
        float camY = panel.cameraY;
        int sw = panel.screenWidth;
        int sh = panel.screenHeight;

        // Thử tìm vị trí an toàn (không vật cản và có đường đi) tối đa 10 lần
        for (int attempt = 0; attempt < 10; attempt++) {
            int side = rand.nextInt(4);
            if (side == 0) { // Top
                ex = camX + rand.nextInt(sw);
                ey = camY - 50;
            } else if (side == 1) { // Bottom
                ex = camX + rand.nextInt(sw);
                ey = camY + sh + 50;
            } else if (side == 2) { // Left
                ex = camX - 50;
                ey = camY + rand.nextInt(sh);
            } else { // Right
                ex = camX + sw + 50;
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

        int minTier = Math.min(5, (waveCount / 3) + 1);
        int maxTier = Math.min(5, Math.max(minTier, waveCount));
        int spawnTier = rand.nextInt((maxTier - minTier) + 1) + minTier;

        // Bắt đầu trộn các loại quái mới từ Wave 3
        if (waveCount >= 3) {
            double roll = Math.random();
            if (roll < 0.15)
                return new ShooterEnemy(ex, ey, spawnTier, surviveTimeSeconds);
            if (roll < 0.25)
                return new AssassinEnemy(ex, ey, spawnTier, surviveTimeSeconds);
            if (waveCount >= 5 && roll < 0.35)
                return new CannoneerEnemy(ex, ey, spawnTier, surviveTimeSeconds);
        }

        return new NormalEnemy(ex, ey, spawnTier, surviveTimeSeconds);
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