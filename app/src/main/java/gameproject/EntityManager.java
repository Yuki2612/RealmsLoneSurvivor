package gameproject;

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

    private long lastEnemySpawnTime;
    public int waveCount = 0;
    private long currentSpawnInterval = 10000;

    public void startNewGame(long currentTime) {
        enemies.clear();
        projectiles.clear();
        heartDrops.clear();
        waveCount = 0;
        currentSpawnInterval = 10000;
        lastEnemySpawnTime = currentTime - currentSpawnInterval;
    }

    public void update(Player player, VFXManager vfxManager, List<PassiveSkill> activeSkills,
            int screenWidth, int screenHeight, long currentTime, int surviveTimeSeconds, GamePanel panel) {

        // 1. Sinh Quái
        if (enemies.isEmpty() || currentTime - lastEnemySpawnTime >= currentSpawnInterval) {
            waveCount++;
            int waveSize = 2 + waveCount;
            for (int i = 0; i < waveSize; i++) {
                enemies.add(spawnSafeEnemy(player, screenWidth, screenHeight, surviveTimeSeconds));
            }

            if (waveCount % 5 == 0) {
                int bType = (waveCount / 5) % 3;
                if (bType == 0)
                    bType = 3;
                float bossStartX = screenWidth / 2f;
                float bossStartY = 50f;
                if (bType == 1)
                    enemies.add(new ChargerBoss(bossStartX, bossStartY, surviveTimeSeconds));
                else if (bType == 2)
                    enemies.add(new TeleporterBoss(bossStartX, bossStartY, surviveTimeSeconds));
                else
                    enemies.add(new TankBoss(bossStartX, bossStartY, surviveTimeSeconds));
            }

            lastEnemySpawnTime = currentTime;
            currentSpawnInterval = Math.min(10000 + ((waveCount - 1) * 5000), 25000);
        }

        // 2. Cập nhật Kỹ năng thụ động
        for (PassiveSkill skill : activeSkills) {
            skill.update(player, enemies, vfxManager, currentTime);
        }

        // 3. Cập nhật Đạn
        ArrayList<Projectile> pendingProjectiles = new ArrayList<>();
        Iterator<Projectile> pIt = projectiles.iterator();
        while (pIt.hasNext()) {
            Projectile p = pIt.next();
            p.update(screenWidth, screenHeight);
            if (!p.isActive()) {
                pIt.remove();
                continue;
            }

            boolean hit = false;
            for (Enemy e : enemies) {
                if (p.getBounds().intersects(e.getBounds())) {
                    e.takeDamage(p.damage);
                    p.setActive(false);

                    if (p.bouncesLeft > 0) {
                        float maxRange = 150.0f + (player.getBreakthroughLevel(Upgrade.CHAIN_LIGHTNING) * 50);
                        Enemy closest = getClosestEnemy(e, enemies, maxRange);
                        if (closest != null) {
                            Projectile bounceProj = new Projectile(e.getX(), e.getY(), closest.getX(), closest.getY(),
                                    1.5f);
                            bounceProj.damage = Math.max(1, p.damage / 2)
                                    + (player.getBreakthroughLevel(Upgrade.CHAIN_LIGHTNING) * 3);
                            bounceProj.bouncesLeft = p.bouncesLeft - 1;
                            pendingProjectiles.add(bounceProj);
                        }
                    }
                    hit = true;
                    break;
                }
            }
            if (hit)
                pIt.remove();
        }
        projectiles.addAll(pendingProjectiles);

        // 4. Cập nhật Quái vật & Va chạm
        float speedMultiplier = 1.0f + (surviveTimeSeconds / 60) * 0.12f;
        Iterator<Enemy> eIt = enemies.iterator();
        while (eIt.hasNext()) {
            Enemy enemy = eIt.next();
            float currentEnemySpeedMulti = speedMultiplier * FrostAuraSkill.getSlowMultiplier(player, enemy);
            enemy.update(player.getX(), player.getY(), currentEnemySpeedMulti, enemies, screenWidth, screenHeight);

            if (!player.isDashing() && !player.isInvulnerable() && player.getBounds().intersects(enemy.getBounds())) {
                if (player.takeHit()) {
                    panel.triggerGameOver();
                } else {
                    vfxManager.triggerScreenShake(15);
                    for (Enemy e : enemies)
                        e.applyKnockback(player.getX(), player.getY(), 150f);
                }
                break;
            }

            if (enemy.isDead()) {
                panel.addScoreAndExp(enemy.getMaxHp());
                SoundManager.play("hit");

                if (!enemy.isBoss && Math.random() < 0.02) {
                    heartDrops.add(new HeartDrop(enemy.getX(), enemy.getY(), currentTime + 10000));
                } else if (enemy.isBoss) {
                    heartDrops.add(new HeartDrop(enemy.getX(), enemy.getY(), currentTime + 20000));
                    heartDrops.add(new HeartDrop(enemy.getX() + 30, enemy.getY(), currentTime + 20000));
                }

                for (PassiveSkill skill : activeSkills) {
                    skill.onEnemyDeath(enemy, player, enemies, vfxManager, currentTime);
                }

                eIt.remove();
            }
        }

        // 5. Cập nhật nhặt đồ
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

    private Enemy spawnSafeEnemy(Player player, int screenWidth, int screenHeight, int surviveTimeSeconds) {
        Random rand = new Random();
        float ex, ey;
        while (true) {
            ex = rand.nextInt(screenWidth - 30);
            ey = rand.nextInt(screenHeight - 30);
            float dx = ex - player.getX();
            float dy = ey - player.getY();
            if ((float) Math.sqrt(dx * dx + dy * dy) >= 250.0f)
                break;
        }
        int minTier = Math.min(5, (waveCount / 3) + 1);
        int maxTier = Math.min(5, Math.max(minTier, waveCount));
        int spawnTier = rand.nextInt((maxTier - minTier) + 1) + minTier;
        return new NormalEnemy(ex, ey, spawnTier, surviveTimeSeconds);
    }

    private Enemy getClosestEnemy(Enemy source, ArrayList<Enemy> allEnemies, float maxDist) {
        Enemy closest = null;
        float minDist = maxDist;
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
        return closest;
    }

    public void draw(Graphics g) {
        for (HeartDrop hd : heartDrops) {
            g.setColor(java.awt.Color.PINK);
            g.fillRect((int) hd.x, (int) hd.y, 15, 15);
        }
        for (Projectile p : projectiles)
            p.draw(g);
        for (Enemy enemy : enemies)
            enemy.draw(g);
    }
}