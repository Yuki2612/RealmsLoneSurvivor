package gameproject.entity;

import gameproject.*;
import gameproject.weapon.Projectile;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DarkFairy extends Enemy {
    public enum State {
        MOVING, SHOOTING, SUMMONING, LIGHTNING, TELEPORTING, TRANSFORMING, DYING
    }

    private State currentState = State.MOVING;
    private String bossKey = "boss4";
    private boolean facingRight = true;
    private Random rand = new Random();

    // Phase tracking
    private boolean isPhase2 = false;
    private boolean transformTriggered = false;

    // Animations - Phase 1
    private Animation moveAnim;
    private Animation castAnim;
    private Animation deathAnim;

    // Animations - Phase 2
    private Animation move2Anim;
    private Animation cast2Anim;
    private Animation teleportAnim;
    private Animation transformAnim;

    // Timers & Cooldowns
    private int actionTimer = 60;
    private long lastShootTime = 0;
    private long lastSummonTime = 0;
    private long lastLightningTime = 0;
    private long lastSkillTime = 0;

    private long shootCooldown = 1500;
    private long summonCooldown = 12000;
    private long lightningCooldown = 2500;
    private long globalSkillCooldown = 1200;

    // Summon tracking
    private List<Enemy> spawnedEnemies = new ArrayList<>();
    private int currentSurviveTime = 0;
    private static final int MAX_SUMMONS = 8;

    // Shooting
    private List<Projectile> pendingProjectiles = new ArrayList<>();
    private int shootVolleys = 0;
    private long lastVolleyTime = 0;

    // Lightning
    private boolean effectTriggered = false;
    private float[] lightningTargetsX;
    private float[] lightningTargetsY;
    private long lightningStartTime = 0;
    private static final long LIGHTNING_TELEGRAPH_DURATION = 900;

    // Transform
    private long transformStartTime = 0;

    public DarkFairy(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 60, (int) ((1100 + (surviveTimeSeconds * 8)) * 1.6f), 1.1f, new Color(80, 0, 120));
        this.isBoss = true;
        this.deathFadeDuration = 2000;
        this.currentSurviveTime = surviveTimeSeconds;
        initAnimations();
    }

    public String getName() {
        return "THE DARK FAIRY";
    }

    private void initAnimations() {
        ImageManager.loadAnimation(bossKey + "_move", "app/res/" + bossKey + "_move.png");
        ImageManager.loadAnimation(bossKey + "_cast", "app/res/" + bossKey + "_cast.png");
        ImageManager.loadAnimation(bossKey + "_death", "app/res/" + bossKey + "_death.png");
        ImageManager.loadAnimation(bossKey + "_move2", "app/res/" + bossKey + "_move2.png");
        ImageManager.loadAnimation(bossKey + "_cast2", "app/res/" + bossKey + "_cast2.png");
        ImageManager.loadAnimation(bossKey + "_teleport", "app/res/" + bossKey + "_teleport.png");
        ImageManager.loadAnimation(bossKey + "_transform", "app/res/" + bossKey + "_transform.png");

        moveAnim = new Animation(8);
        moveAnim.setFrames(ImageManager.getAnimation(bossKey + "_move"));

        castAnim = new Animation(5);
        castAnim.setFrames(ImageManager.getAnimation(bossKey + "_cast"));
        castAnim.setLooping(false);

        deathAnim = new Animation(8); // 8 frames × 8 delay ≈ 1.1s
        deathAnim.setFrames(ImageManager.getAnimation(bossKey + "_death"));
        deathAnim.setLooping(false);

        move2Anim = new Animation(12); // 2 frames × 12 delay = vỗ cánh bướm mượt
        move2Anim.setFrames(ImageManager.getAnimation(bossKey + "_move2"));

        cast2Anim = new Animation(6); // 4 frames × 6 delay = cast vừa đủ
        cast2Anim.setFrames(ImageManager.getAnimation(bossKey + "_cast2"));
        cast2Anim.setLooping(false);

        teleportAnim = new Animation(6); // 3 frames × 6 delay = biến mất/xuất hiện
        teleportAnim.setFrames(ImageManager.getAnimation(bossKey + "_teleport"));
        teleportAnim.setLooping(false);

        transformAnim = new Animation(14); // 8 frames × 14 delay ≈ 1.9 giây — chậm rãi, kịch tính
        transformAnim.setFrames(ImageManager.getAnimation(bossKey + "_transform"));
        transformAnim.setLooping(false);
    }

    @Override
    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
        // Boss không bị đẩy lùi
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {

        long currentTime = GamePanel.getTickTime();

        // --- DYING ---
        if (isDying) {
            if (currentState != State.DYING) {
                currentState = State.DYING;
                if (deathAnim != null)
                    deathAnim.reset();
            }
            if (deathAnim != null)
                deathAnim.update();
            return;
        }

        // --- TRANSFORM CHECK ---
        if (!transformTriggered && hp < maxHp / 2) {
            transformTriggered = true;
            currentState = State.TRANSFORMING;
            transformAnim.reset();
            transformStartTime = currentTime;
            panel.vfxManager.showWaveBanner("THE DARK FAIRY TRANSFORMS!", new Color(150, 50, 255), currentTime);
            panel.vfxManager.triggerScreenShake(15);
            SoundManager.play("shield");
        }

        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float dx = playerX - centerX;
        float dy = playerY - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (currentState != State.TRANSFORMING) {
            if (dx > 5)
                facingRight = true;
            else if (dx < -5)
                facingRight = false;
        }

        switch (currentState) {
            case MOVING:
                Animation currentMoveAnim = isPhase2 ? move2Anim : moveAnim;
                if (currentMoveAnim != null)
                    currentMoveAnim.update();

                EnemyController.moveEnemy(this, panel, speedMultiplier);

                if (actionTimer > 0)
                    actionTimer--;

                if (actionTimer <= 0 && currentTime - lastSkillTime > globalSkillCooldown) {
                    // Phase 2: Teleport khi người chơi quá xa
                    if (isPhase2 && dist > 600 && currentTime - lastSkillTime > 3000) {
                        currentState = State.TELEPORTING;
                        teleportAnim.reset();
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                    // Phase 1 & 2: Lightning
                    else if (dist < 600 && currentTime - lastLightningTime > lightningCooldown) {
                        currentState = State.LIGHTNING;
                        Animation ca = isPhase2 ? cast2Anim : castAnim;
                        if (ca != null)
                            ca.reset();
                        prepareLightningTargets(playerX, playerY);
                        lightningStartTime = currentTime;
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                    // Summon quân
                    else if (currentTime - lastSummonTime > summonCooldown
                            && countActiveSummons(allEnemies) < MAX_SUMMONS) {
                        currentState = State.SUMMONING;
                        Animation ca = isPhase2 ? cast2Anim : castAnim;
                        if (ca != null)
                            ca.reset();
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                    // Bắn đạn
                    else if (dist < 600 && currentTime - lastShootTime > shootCooldown) {
                        currentState = State.SHOOTING;
                        Animation ca = isPhase2 ? cast2Anim : castAnim;
                        if (ca != null)
                            ca.reset();
                        shootVolleys = 0;
                        lastVolleyTime = 0;
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                }
                break;

            case SHOOTING:
                Animation shootCastAnim = isPhase2 ? cast2Anim : castAnim;
                if (shootCastAnim != null) {
                    shootCastAnim.update();

                    // Bắn đạn: Bullet Hell
                    int shootTrigger = isPhase2 ? 2 : 3;
                    int maxVolleys = isPhase2 ? 2 : 1;

                    if (shootCastAnim.getFrameIndex() >= shootTrigger) {
                        if (shootVolleys < maxVolleys && currentTime - lastVolleyTime > 300) {
                            fireBulletHell(playerX, playerY, shootVolleys);
                            lastVolleyTime = currentTime;
                            shootVolleys++;
                            SoundManager.play("laser");
                        }
                    }

                    if (shootCastAnim.hasFinishedCycle()) {
                        if (shootVolleys >= maxVolleys) {
                            currentState = State.MOVING;
                            actionTimer = 30;
                            lastShootTime = currentTime;
                        } else {
                            // Reset cycle if it hasn't finished firing volleys
                            shootCastAnim.reset();
                        }
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case SUMMONING:
                Animation summonCastAnim = isPhase2 ? cast2Anim : castAnim;
                if (summonCastAnim != null) {
                    summonCastAnim.update();

                    int summonTrigger = isPhase2 ? 2 : 3;
                    if (summonCastAnim.getFrameIndex() >= summonTrigger && !effectTriggered) {
                        triggerSummon(panel, currentTime);
                        lastSummonTime = currentTime;
                        effectTriggered = true;
                        SoundManager.play("shield");
                        panel.vfxManager.triggerScreenShake(8);
                    }

                    if (summonCastAnim.hasFinishedCycle()) {
                        currentState = State.MOVING;
                        actionTimer = 40;
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case LIGHTNING:
                Animation lightCastAnim = isPhase2 ? cast2Anim : castAnim;
                if (lightCastAnim != null) {
                    lightCastAnim.update();

                    // Sau khi telegraph hết thời gian, gây sát thương
                    if (currentTime - lightningStartTime >= LIGHTNING_TELEGRAPH_DURATION && !effectTriggered) {
                        triggerLightningStrike(panel, currentTime);
                        effectTriggered = true;
                    }

                    if (lightCastAnim.hasFinishedCycle() && effectTriggered) {
                        currentState = State.MOVING;
                        lastLightningTime = currentTime;
                        actionTimer = 50;
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case TELEPORTING:
                if (teleportAnim != null) {
                    teleportAnim.update();

                    // Giữa animation: dịch chuyển
                    int tpFrames = teleportAnim.getTotalFrames();
                    if (tpFrames > 0 && teleportAnim.getFrameIndex() == tpFrames / 2 && !effectTriggered) {
                        float angle = (float) (Math.random() * Math.PI * 2);
                        float offset = 250f;
                        this.x = playerX + (float) Math.cos(angle) * offset;
                        this.y = playerY + (float) Math.sin(angle) * offset;

                        // Clamp trong bản đồ
                        this.x = Math.max(0, Math.min(this.x, GamePanel.WORLD_WIDTH - size));
                        this.y = Math.max(0, Math.min(this.y, GamePanel.WORLD_HEIGHT - size));

                        panel.vfxManager.spawnComboSparkles(x, y, currentTime, new Color(150, 50, 255), 5);
                        SoundManager.play("shield");
                        effectTriggered = true;
                    }

                    if (teleportAnim.hasFinishedCycle()) {
                        currentState = State.MOVING;
                        actionTimer = 25;
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case TRANSFORMING:
                // Bất tử trong lúc chuyển dạng
                if (transformAnim != null)
                    transformAnim.update();

                if (transformAnim != null && transformAnim.hasFinishedCycle()) {
                    isPhase2 = true;
                    speed = 1.6f; // Nhanh hơn Phase 1 (khôi phục theo yêu cầu)
                    shootCooldown = 1200;
                    summonCooldown = 8000;
                    currentState = State.MOVING;
                    actionTimer = 30;
                    panel.vfxManager.addExplosion(centerX, centerY, 300, currentTime);
                    SoundManager.play("explosion");
                }
                break;

            case DYING:
                break;
        }
    }

    // --- Bắn đạn Bullet Hell (Tròn xung quanh) ---
    private void fireBulletHell(float playerX, float playerY, int volleyIndex) {
        float cx = x + size / 2;
        float cy = y + size / 2;

        // Offset góc cho loạt thứ 2 để đạn lấp đầy khoảng trống
        float angleOffset = (volleyIndex == 1) ? 0.15f : 0f;

        int bulletCount = isPhase2 ? 18 : 10;

        for (int i = 0; i < bulletCount; i++) {
            float angle = (float) (Math.PI * 2 * i / bulletCount) + angleOffset;
            float targetX = cx + (float) Math.cos(angle) * 500;
            float targetY = cy + (float) Math.sin(angle) * 500;

            Projectile p = new Projectile(cx, cy, targetX, targetY, 0.45f, 700f);
            p.isEnemyBullet = true;
            p.isFairyBullet = true;
            p.damage = 1;
            pendingProjectiles.add(p);
        }
    }

    // --- Triệu hồi quân ---
    private void triggerSummon(GamePanel panel, long currentTime) {
        int tier = Math.min(5, 1 + (currentSurviveTime / 60));
        int timeBonus = (int) (currentSurviveTime * 1.2f);
        int count = isPhase2 ? 4 + rand.nextInt(2) : 3 + rand.nextInt(2);

        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.PI * 2 * i / count);
            float spawnX = x + (float) Math.cos(angle) * 100;
            float spawnY = y + (float) Math.sin(angle) * 100;

            Enemy enemy;
            int type = rand.nextInt(4);
            switch (type) {
                case 0 -> enemy = new NormalEnemy(spawnX, spawnY, tier, timeBonus);
                case 1 -> enemy = new ShooterEnemy(spawnX, spawnY, tier, timeBonus);
                case 2 -> enemy = new WizardEnemy(spawnX, spawnY, tier, timeBonus);
                default -> enemy = new AssassinEnemy(spawnX, spawnY, tier, timeBonus);
            }
            spawnedEnemies.add(enemy);
        }

        panel.vfxManager.addExplosion(x + size / 2, y + size / 2, 150, currentTime);
    }

    // --- Sấm sét ---
    private void prepareLightningTargets(float playerX, float playerY) {
        int count = isPhase2 ? 5 : 2;
        lightningTargetsX = new float[count];
        lightningTargetsY = new float[count];

        for (int i = 0; i < count; i++) {
            float offsetX = (rand.nextFloat() - 0.5f) * 200;
            float offsetY = (rand.nextFloat() - 0.5f) * 200;
            lightningTargetsX[i] = playerX + offsetX;
            lightningTargetsY[i] = playerY + offsetY;
        }
    }

    // Lightning visual tracking
    private long lightningStrikeTime = 0;
    private static final long LIGHTNING_VISUAL_DURATION = 500;

    private void triggerLightningStrike(GamePanel panel, long currentTime) {
        if (lightningTargetsX == null)
            return;

        lightningStrikeTime = currentTime;

        float pCenterX = panel.player.getX() + Player.SIZE / 2;
        float pCenterY = panel.player.getY() + Player.SIZE / 2;

        for (int i = 0; i < lightningTargetsX.length; i++) {
            float lx = lightningTargetsX[i];
            float ly = lightningTargetsY[i];

            panel.vfxManager.triggerScreenShake(10);
            SoundManager.play("explosion");

            float distToPlayer = (float) Math.sqrt(Math.pow(pCenterX - lx, 2) + Math.pow(pCenterY - ly, 2));
            if (distToPlayer < 90) {
                panel.player.takeDamage(1);
            }
        }
    }

    // --- Đếm quái triệu hồi còn sống ---
    private int countActiveSummons(ArrayList<Enemy> allEnemies) {
        int count = 0;
        for (Enemy e : allEnemies) {
            if (!e.isBoss && !e.isDying)
                count++;
        }
        return count;
    }

    // --- Override shoot() để trả đạn cho EntityManager xử lý ---
    @Override
    public java.util.List<Projectile> shoot() {
        if (!pendingProjectiles.isEmpty()) {
            List<Projectile> result = new ArrayList<>(pendingProjectiles);
            pendingProjectiles.clear();
            return result;
        }
        return null;
    }

    // --- Override summon() để trả quái cho EntityManager xử lý ---
    @Override
    public List<Enemy> summon() {
        if (!spawnedEnemies.isEmpty()) {
            List<Enemy> result = new ArrayList<>(spawnedEnemies);
            spawnedEnemies.clear();
            return result;
        }
        return null;
    }

    // --- Bất tử khi đang transform ---
    @Override
    public void takeDamage(int damage, boolean isCrit, VFXManager vfxManager, long currentTime) {
        if (currentState == State.TRANSFORMING)
            return; // Bất tử
        super.takeDamage(damage, isCrit, vfxManager, currentTime);
    }

    @Override
    public void takeDamage(int damage, VFXManager vfxManager, long currentTime) {
        if (currentState == State.TRANSFORMING)
            return; // Bất tử
        super.takeDamage(damage, vfxManager, currentTime);
    }

    // --- DRAW ---
    @Override
    public void draw(Graphics g) {
        BufferedImage img = null;

        if (isDying) {
            img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
        } else {
            switch (currentState) {
                case MOVING:
                    Animation ma = isPhase2 ? move2Anim : moveAnim;
                    img = (ma != null) ? ma.getCurrentFrame() : null;
                    break;
                case SHOOTING:
                case SUMMONING:
                case LIGHTNING:
                    Animation ca = isPhase2 ? cast2Anim : castAnim;
                    img = (ca != null) ? ca.getCurrentFrame() : null;
                    break;
                case TELEPORTING:
                    img = (teleportAnim != null) ? teleportAnim.getCurrentFrame() : null;
                    break;
                case TRANSFORMING:
                    img = (transformAnim != null) ? transformAnim.getCurrentFrame() : null;
                    break;
                case DYING:
                    img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
                    break;
            }
        }

        if (img != null) {
            drawBoss(g, img);
        } else {
            // Fallback: vẽ hình vuông nếu chưa có sprite
            g.setColor(isPhase2 ? new Color(180, 50, 255) : color);
            g.fillRect((int) x, (int) y, size, size);
        }

        // --- VẼ TELEGRAPH SẤM SÉT ---
        if (currentState == State.LIGHTNING && lightningTargetsX != null && !effectTriggered) {
            long elapsed = GamePanel.getTickTime() - lightningStartTime;
            float progress = Math.min(1.0f, (float) elapsed / LIGHTNING_TELEGRAPH_DURATION);
            for (int i = 0; i < lightningTargetsX.length; i++) {
                drawLightningTelegraph(g, lightningTargetsX[i], lightningTargetsY[i], progress);
            }
        }

        // --- VẼ SÉT ĐÁNH (sau khi telegraph kết thúc) ---
        if (lightningTargetsX != null && lightningStrikeTime > 0) {
            long elapsed = GamePanel.getTickTime() - lightningStrikeTime;
            if (elapsed < LIGHTNING_VISUAL_DURATION) {
                float fadeAlpha = 1.0f - (float) elapsed / LIGHTNING_VISUAL_DURATION;
                for (int i = 0; i < lightningTargetsX.length; i++) {
                    drawLightningBolt(g, lightningTargetsX[i], lightningTargetsY[i], fadeAlpha, elapsed);
                }
            } else {
                lightningStrikeTime = 0;
            }
        }

        // --- HIỆU ỨNG BÓNG CHẮN KHI ĐANG TRANSFORM ---
        if (currentState == State.TRANSFORMING) {
            Graphics2D g2d = (Graphics2D) g.create();
            long elapsed = GamePanel.getTickTime() - transformStartTime;
            float pulse = 0.3f + 0.2f * (float) Math.sin(elapsed / 150.0);
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, pulse));
            g2d.setColor(new Color(180, 50, 255));
            int shieldSize = size + 40;
            g2d.fillOval((int) x - 20, (int) y - 20, shieldSize, shieldSize);
            g2d.dispose();
        }

        // --- DEBUG HITBOX ---
        if (GamePanel.showHitboxes) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.GREEN);
            g2d.drawRect((int) x, (int) y, size, size);
            g2d.dispose();
        }
    }

    private void drawLightningTelegraph(Graphics g, float tx, float ty, float progress) {
        Graphics2D g2d = (Graphics2D) g.create();
        int maxSize = 180;

        // Vòng tròn cảnh báo màu vàng
        g2d.setColor(new Color(255, 255, 0, 100));
        g2d.drawOval((int) tx - maxSize / 2, (int) ty - maxSize / 2, maxSize, maxSize);

        // Vùng fill tiến dần
        g2d.setColor(new Color(255, 255, 0, 60));
        int fillSize = (int) (maxSize * progress);
        g2d.fillOval((int) tx - fillSize / 2, (int) ty - fillSize / 2, fillSize, fillSize);

        g2d.dispose();
    }

    private void drawLightningBolt(Graphics g, float tx, float ty, float alpha, long elapsed) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Hiệu ứng chớp nháy
        float flicker = 0.7f + 0.3f * (float) Math.sin(elapsed / 30.0);
        float finalAlpha = Math.max(0, Math.min(1, alpha * flicker));
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, finalAlpha));

        int boltX = (int) tx;
        int boltY = (int) ty;

        // Glow vàng bên ngoài (vầng sáng rộng)
        g2d.setColor(new Color(255, 255, 100, 80));
        g2d.fillRect(boltX - 20, boltY - 400, 40, 400);

        // Cột sét trắng sáng ở giữa
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillRect(boltX - 6, boltY - 400, 12, 400);

        // Cột sét vàng viền
        g2d.setColor(new Color(255, 230, 50));
        g2d.fillRect(boltX - 10, boltY - 400, 4, 400);
        g2d.fillRect(boltX + 6, boltY - 400, 4, 400);

        // Vòng sáng tại mặt đất (nơi sét đánh)
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, finalAlpha * 0.6f));
        g2d.setColor(new Color(255, 255, 150));
        int groundGlow = 120;
        g2d.fillOval(boltX - groundGlow / 2, boltY - groundGlow / 2, groundGlow, groundGlow);

        // Vòng tròn sáng trắng nhỏ tại tâm
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, finalAlpha));
        g2d.setColor(Color.WHITE);
        g2d.fillOval(boltX - 15, boltY - 15, 30, 30);

        g2d.dispose();
    }

    private void drawBoss(Graphics g, BufferedImage img) {
        Graphics2D g2d = (Graphics2D) g.create();
        long now = GamePanel.getTickTime();

        float scale = isPhase2 ? 2.5f : 2.3f;
        int drawW = (int) (img.getWidth() * scale);
        int drawH = (int) (img.getHeight() * scale);
        int drawX = (int) x - drawW / 2 + size / 2;
        int drawY = (int) y - drawH + size;

        float alpha = 1.0f;
        if (currentState == State.TELEPORTING && teleportAnim != null) {
            int frame = teleportAnim.getFrameIndex();
            int total = teleportAnim.getTotalFrames();
            if (total > 0) {
                int mid = total / 2;
                if (frame <= mid)
                    alpha = 1.0f - (float) frame / (float) mid;
                else
                    alpha = (float) (frame - mid) / (float) (total - mid);
            }
        } else if (isDying) {
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        }

        g2d.setComposite(
                java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha))));

        if (!facingRight)
            g2d.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null);
        else
            g2d.drawImage(img, drawX, drawY, drawW, drawH, null);

        // Flash trắng khi bị đánh
        if (!isDying && now < hitFlashEndTime && currentState != State.TELEPORTING) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f * alpha));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(drawX, drawY, drawW, drawH);
        }

        g2d.dispose();
    }
}
