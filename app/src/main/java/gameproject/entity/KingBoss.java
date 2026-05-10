package gameproject.entity;

import gameproject.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class KingBoss extends Enemy {
    public enum State {
        MOVING, ATTACKING_1, ATTACKING_2, ATTACKING_GROUND, TELEPORTING, DYING
    }

    private State currentState = State.MOVING;
    private String bossKey = "boss3";

    // Animations
    private Animation moveAnim;
    private Animation attack1Anim;
    private Animation attack2Anim;
    private Animation groundAnim;
    private Animation teleportAnim;
    private Animation deathAnim;

    // Timers & Logic
    private int actionTimer = 40;
    private boolean facingRight = true;
    private float targetX, targetY;
    private boolean[] thresholds = { false, false, false, false }; // 80%, 60%, 40%, 20%
    private List<Enemy> spawnedEnemies = new ArrayList<>();
    private int currentSurviveTime = 0;

    // Skill variables
    private long lastAttack2Time = 0;
    private long lastGroundTime = 0;
    private long globalSkillCooldown = 1800;
    private long lastSkillTime = 0;

    private long attack2Cooldown = 9000;
    private long groundCooldown = 6500;

    private boolean isAoeTeleport = false;
    private boolean effectTriggered = false;

    // Ground attack visual tracking
    private long groundStrikeTime = 0;
    private float groundStrikeX, groundStrikeY;
    private static final long GROUND_VISUAL_DURATION = 600;

    public KingBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 55, (int) ((1300 + (surviveTimeSeconds * 8)) * 1.8f), 1.1f, Color.DARK_GRAY);
        this.isBoss = true;
        this.deathFadeDuration = 2000;
        this.currentSurviveTime = surviveTimeSeconds;
        initAnimations();
    }

    private void initAnimations() {
        ImageManager.loadAnimation(bossKey + "_move", "app/res/" + bossKey + "_move.png", 8);
        ImageManager.loadAnimation(bossKey + "_attack1", "app/res/" + bossKey + "_attack1.png", 17);
        ImageManager.loadAnimation(bossKey + "_attack2", "app/res/" + bossKey + "_attack2.png", 17);
        ImageManager.loadAnimation(bossKey + "_ground", "app/res/" + bossKey + "_ground.png", 20);
        ImageManager.loadAnimation(bossKey + "_teleport", "app/res/" + bossKey + "_teleport.png", 14);
        ImageManager.loadAnimation(bossKey + "_death", "app/res/" + bossKey + "_death.png", 10);

        moveAnim = new Animation(6);
        moveAnim.setFrames(ImageManager.getAnimation(bossKey + "_move"));

        attack1Anim = new Animation(3);
        attack1Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack1"));
        attack1Anim.setLooping(false);

        attack2Anim = new Animation(3);
        attack2Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack2"));
        attack2Anim.setLooping(false);

        groundAnim = new Animation(4);
        groundAnim.setFrames(ImageManager.getAnimation(bossKey + "_ground"));
        groundAnim.setLooping(false);

        teleportAnim = new Animation(3);
        teleportAnim.setFrames(ImageManager.getAnimation(bossKey + "_teleport"));
        teleportAnim.setLooping(false);

        deathAnim = new Animation(8);
        deathAnim.setFrames(ImageManager.getAnimation(bossKey + "_death"));
        deathAnim.setLooping(false);
    }

    @Override
    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {

        long currentTime = GamePanel.getTickTime();

        if (isDying) {
            if (currentState != State.DYING) {
                currentState = State.DYING;
                deathAnim.reset();
            }
            deathAnim.update();
            return;
        }

        checkThresholds(panel, currentTime);

        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float pCenterX = playerX + Player.SIZE / 2;
        float pCenterY = playerY + Player.SIZE / 2;

        float dx = pCenterX - centerX;
        float dy = pCenterY - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (currentState == State.MOVING || currentState == State.TELEPORTING) {
            if (dx > 5)
                facingRight = true;
            else if (dx < -5)
                facingRight = false;
        }

        switch (currentState) {
            case MOVING:
                moveAnim.update();
                EnemyController.moveEnemy(this, panel, speedMultiplier);

                if (actionTimer > 0)
                    actionTimer--;

                if (actionTimer <= 0) {
                    if (dist > 700) {
                        currentState = State.TELEPORTING;
                        teleportAnim.reset();
                        float angle = (float) Math.atan2(dy, dx);
                        targetX = x + (float) Math.cos(angle) * 350;
                        targetY = y + (float) Math.sin(angle) * 350;
                        isAoeTeleport = false;
                        effectTriggered = false;
                    } else if (dist > 350 && dist < 750
                            && currentTime - lastGroundTime > groundCooldown
                            && currentTime - lastSkillTime > globalSkillCooldown) {
                        currentState = State.ATTACKING_GROUND;
                        groundAnim.reset();
                        targetX = playerX;
                        targetY = playerY;
                        lastSkillTime = currentTime;
                        effectTriggered = false;
                    } else if (dist > 450 && currentTime - lastAttack2Time > attack2Cooldown
                            && currentTime - lastSkillTime > globalSkillCooldown) {
                        currentState = State.TELEPORTING;
                        teleportAnim.reset();
                        targetX = playerX;
                        targetY = playerY;
                        isAoeTeleport = true;
                        lastSkillTime = currentTime;
                        effectTriggered = false;
                    } else if (dist <= 250) {
                        // CHỈ DASH NẾU XUNG QUANH THOÁNG (75px)
                        boolean nearObstacle = !panel.mapManager.getObstaclesInRadius(centerX, centerY, 75).isEmpty();
                        if (!nearObstacle) {
                            currentState = State.ATTACKING_1;
                            attack1Anim.reset();
                            effectTriggered = false;
                            float adist = (float) Math.sqrt(dx * dx + dy * dy);
                            targetX = (dx / adist);
                            targetY = (dy / adist);
                        } else {
                            actionTimer = 10;
                        }
                    }
                }
                break;

            case TELEPORTING:
                teleportAnim.update();
                if (teleportAnim.getFrameIndex() == 7 && !effectTriggered) {
                    if (isAoeTeleport) {
                        float offset = 120;
                        this.x = pCenterX - (facingRight ? offset : -offset) - size / 2;
                        this.y = pCenterY - size / 2;
                    } else {
                        this.x = targetX;
                        this.y = targetY;
                    }
                    panel.vfxManager.spawnComboSparkles(x, y, currentTime, Color.YELLOW, 5);
                    effectTriggered = true;
                }
                if (teleportAnim.hasFinishedCycle()) {
                    if (isAoeTeleport) {
                        currentState = State.ATTACKING_2;
                        attack2Anim.reset();
                        effectTriggered = false;
                        lastAttack2Time = currentTime;
                    } else {
                        currentState = State.MOVING;
                        actionTimer = 25;
                    }
                }
                break;

            case ATTACKING_1:
                attack1Anim.update();
                int idx1 = attack1Anim.getFrameIndex();

                if (idx1 <= 10) {
                    if (dist > 60) {
                        float lungeSpeed = 9.0f;
                        x += targetX * lungeSpeed;
                        y += targetY * lungeSpeed;
                        EnemyController.resolveHybridCollision(this, panel.mapManager);
                    }
                } else if (idx1 <= 13) {
                    float trackingSpeed = 1.0f;
                    x += targetX * trackingSpeed;
                    y += targetY * trackingSpeed;
                    EnemyController.resolveHybridCollision(this, panel.mapManager);
                }

                if (idx1 == 11 && !effectTriggered) {
                    checkHit(panel, 100, 1);
                    panel.vfxManager.triggerScreenShake(8);
                    effectTriggered = true;
                }
                if (attack1Anim.hasFinishedCycle()) {
                    currentState = State.MOVING;
                    actionTimer = 35;
                }
                break;

            case ATTACKING_2:
                attack2Anim.update();
                int idx2 = attack2Anim.getFrameIndex();
                if (idx2 == 14 && !effectTriggered) {
                    checkHit(panel, 260, 1);
                    panel.vfxManager.triggerScreenShake(18);
                    panel.vfxManager.addExplosion(centerX, centerY, 520, currentTime);
                    SoundManager.play("explosion");
                    effectTriggered = true;
                }
                if (attack2Anim.hasFinishedCycle()) {
                    currentState = State.MOVING;
                    actionTimer = 50;
                }
                break;

            case ATTACKING_GROUND:
                groundAnim.update();
                if (groundAnim.getFrameIndex() == 14 && !effectTriggered) {
                    triggerGroundEffect(panel, currentTime);
                    lastGroundTime = currentTime;
                    effectTriggered = true;
                }
                if (groundAnim.hasFinishedCycle()) {
                    currentState = State.MOVING;
                    actionTimer = 60;
                }
                break;

            case DYING:
                break;
        }
    }

    private void triggerGroundEffect(GamePanel panel, long currentTime) {
        groundStrikeTime = currentTime;
        groundStrikeX = targetX;
        groundStrikeY = targetY;

        panel.vfxManager.addExplosion(targetX, targetY, 280, currentTime);
        panel.vfxManager.triggerScreenShake(12);
        SoundManager.play("explosion");
        float distToExplosion = (float) Math.sqrt(Math.pow((panel.player.getX() + Player.SIZE / 2) - targetX, 2)
                + Math.pow((panel.player.getY() + Player.SIZE / 2) - targetY, 2));
        if (distToExplosion < 140)
            panel.player.takeDamage(1);
    }

    private void checkThresholds(GamePanel panel, long currentTime) {
        float hpPercent = (float) hp / maxHp;
        if (hpPercent <= 0.80f && !thresholds[0]) {
            triggerSummon(panel, currentTime);
            thresholds[0] = true;
        } else if (hpPercent <= 0.60f && !thresholds[1]) {
            triggerSummon(panel, currentTime);
            thresholds[1] = true;
        } else if (hpPercent <= 0.40f && !thresholds[2]) {
            triggerSummon(panel, currentTime);
            thresholds[2] = true;
        } else if (hpPercent <= 0.20f && !thresholds[3]) {
            triggerSummon(panel, currentTime);
            thresholds[3] = true;
        }
    }

    private void triggerSummon(GamePanel panel, long currentTime) {
        // TÍNH TOÁN TIER THEO THỜI GIAN: Mỗi 3 phút (180s) tăng 1 Tier
        int tier = Math.min(5, 1 + (currentSurviveTime / 60));

        // Tăng cường máu "Hoàng gia" (Sử dụng 1.5x thời gian chơi thực tế để buff HP)
        int royalTimeBonus = (int) (currentSurviveTime * 1.5f);

        // Triệu hồi 4 cận vệ bao quanh Boss với Tier đã tính toán
        spawnedEnemies.add(new NormalEnemy(x - 80, y - 80, tier, royalTimeBonus));
        spawnedEnemies.add(new NormalEnemy(x + 80, y - 80, tier, royalTimeBonus));
        spawnedEnemies.add(new ShooterEnemy(x - 80, y + 80, tier, royalTimeBonus));
        spawnedEnemies.add(new ShooterEnemy(x + 80, y + 80, tier, royalTimeBonus));

        // Hiệu ứng Visual & Sound
        panel.vfxManager.triggerScreenShake(10);
        panel.vfxManager.addExplosion(x + size / 2, y + size / 2, 150, currentTime);
        SoundManager.play("shield");
    }

    @Override
    public List<Enemy> summon() {
        if (spawnedEnemies.isEmpty())
            return null;
        List<Enemy> result = new ArrayList<>(spawnedEnemies);
        spawnedEnemies.clear();
        return result;
    }

    private void checkHit(GamePanel panel, float range, int damage) {
        float pCenterX = panel.player.getX() + Player.SIZE / 2;
        float pCenterY = panel.player.getY() + Player.SIZE / 2;
        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float dist = (float) Math.sqrt(Math.pow(pCenterX - centerX, 2) + Math.pow(pCenterY - centerY, 2));
        if (dist < range)
            panel.player.takeDamage(damage);
    }

    @Override
    public void draw(Graphics g) {
        BufferedImage img = null;
        switch (currentState) {
            case MOVING:
                img = (moveAnim != null) ? moveAnim.getCurrentFrame() : null;
                break;
            case ATTACKING_1:
                img = (attack1Anim != null) ? attack1Anim.getCurrentFrame() : null;
                break;
            case ATTACKING_2:
                img = (attack2Anim != null) ? attack2Anim.getCurrentFrame() : null;
                break;
            case ATTACKING_GROUND:
                img = (groundAnim != null) ? groundAnim.getCurrentFrame() : null;
                break;
            case TELEPORTING:
                img = (teleportAnim != null) ? teleportAnim.getCurrentFrame() : null;
                break;
            case DYING:
                img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
                break;
        }

        if (img != null) {
            drawBossNative(g, img);
        } else {
            g.setColor(color);
            g.fillRect((int) x, (int) y, size, size);
        }

        // --- HIỂN THỊ HITBOX ĐỂ DEBUG ---
        if (GamePanel.showHitboxes) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.GREEN);
            g2d.drawRect((int) x, (int) y, size, size);

            if (currentState == State.ATTACKING_1) {
                g2d.setColor(Color.RED);
                g2d.drawOval((int) (x + size / 2 - 100), (int) (y + size / 2 - 100), 200, 200);
            }

            if (currentState == State.ATTACKING_2) {
                g2d.setColor(Color.RED);
                g2d.drawOval((int) (x + size / 2 - 260), (int) (y + size / 2 - 260), 520, 520);
            }

            if (currentState == State.ATTACKING_GROUND) {
                g2d.setColor(Color.RED);
                g2d.drawOval((int) (targetX - 140), (int) (targetY - 140), 280, 280);
            }
            g2d.dispose();
        }

        if (currentState == State.ATTACKING_GROUND && groundAnim.getFrameIndex() < 14) {
            float progress = Math.min(1.0f, (float) groundAnim.getFrameIndex() / 11);
            drawTelegraph(g, targetX, targetY, progress, 280);
        }
        // Hiệu ứng cột lửa sau khi ground attack kích hoạt
        if (groundStrikeTime > 0) {
            long elapsed = GamePanel.getTickTime() - groundStrikeTime;
            if (elapsed < GROUND_VISUAL_DURATION) {
                float fadeAlpha = 1.0f - (float) elapsed / GROUND_VISUAL_DURATION;
                drawGroundStrike(g, groundStrikeX, groundStrikeY, fadeAlpha, elapsed);
            } else {
                groundStrikeTime = 0;
            }
        }
        if (currentState == State.ATTACKING_2 && attack2Anim.getFrameIndex() < 14) {
            float progress = Math.min(1.0f, (float) attack2Anim.getFrameIndex() / 11);
            drawTelegraph(g, x + size / 2, y + size / 2, progress, 520);
        }
    }

    private void drawTelegraph(Graphics g, float tx, float ty, float progress, int maxSize) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(new Color(255, 0, 0, 100));
        g2d.drawOval((int) tx - maxSize / 2, (int) ty - maxSize / 2, maxSize, maxSize);
        g2d.setColor(new Color(255, 0, 0, 60));
        int tSize = (int) (maxSize * progress);
        g2d.fillOval((int) tx - tSize / 2, (int) ty - tSize / 2, tSize, tSize);
        g2d.dispose();
    }

    private void drawGroundStrike(Graphics g, float tx, float ty, float alpha, long elapsed) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        float flicker = 0.8f + 0.2f * (float) Math.sin(elapsed / 15.0);
        float finalAlpha = Math.max(0, Math.min(1, alpha * flicker));
        int cx = (int) tx;
        int cy = (int) ty;

        java.util.Random rand = new java.util.Random(Double.doubleToLongBits(tx * 123 + ty));

        float crackProgress = Math.min(1.0f, (float) elapsed / 150); // Nứt rất nhanh
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, finalAlpha));

        // 1. Vết xém/Crater trung tâm
        g2d.setColor(new Color(20, 5, 0, 150));
        g2d.fillOval(cx - 35, cy - 25, 70, 50);

        // 2. Vết nứt (Tự nhiên: to ở trong, nhỏ ở ngoài, uốn lượn)
        int numCracks = 3 + rand.nextInt(2); // 3-4 đường nứt chính
        double angleOffset = rand.nextDouble() * Math.PI * 2;

        for (int i = 0; i < numCracks; i++) {
            // Phân bổ góc đều nhau để tránh các vết nứt dính sát nhau
            double angle = angleOffset + (i * (Math.PI * 2 / numCracks)) + (rand.nextDouble() - 0.5) * 0.5;
            int segments = 4 + rand.nextInt(3); // 4-6 phân đoạn
            float totalLen = 110 + rand.nextFloat() * 35; // Dài 110-145 px (Tràn ra tới viền đỏ 140px)

            float[] ptsX = new float[segments + 1];
            float[] ptsY = new float[segments + 1];
            ptsX[0] = cx;
            ptsY[0] = cy;

            double currentAngle = angle;
            for (int s = 1; s <= segments; s++) {
                float segLen = totalLen / segments;
                currentAngle += (rand.nextFloat() - 0.5f) * 1.2f; // Chệch hướng ngẫu nhiên
                ptsX[s] = ptsX[s - 1] + (float) Math.cos(currentAngle) * segLen;
                ptsY[s] = ptsY[s - 1] + (float) Math.sin(currentAngle) * segLen;
            }

            // Vẽ từng phân đoạn dựa trên crackProgress
            int maxSeg = (int) (segments * crackProgress);
            for (int s = 0; s < maxSeg; s++) {
                float thickness = Math.max(1f, 7f - (s * 7f / segments));

                // Viền magma rực rỡ bên dưới (Nền to)
                g2d.setStroke(new java.awt.BasicStroke(thickness + 2.5f, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                g2d.setColor(new Color(255, 100 + (int) (80 * (1f - (float) s / segments)), 0, 200));
                g2d.drawLine((int) ptsX[s], (int) ptsY[s], (int) ptsX[s + 1], (int) ptsY[s + 1]);

                // Lõi đen/nứt nẻ bên trên (Nét nhỏ)
                g2d.setStroke(new java.awt.BasicStroke(Math.max(1f, thickness - 1.5f), java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                g2d.setColor(new Color(15, 5, 0, 230));
                g2d.drawLine((int) ptsX[s], (int) ptsY[s], (int) ptsX[s + 1], (int) ptsY[s + 1]);

                // Nứt nhánh nhỏ
                if (s > 0 && rand.nextFloat() < 0.3f) { // Xác suất nhánh ít hơn
                    float branchAngle = (float) currentAngle + (rand.nextBoolean() ? 0.9f : -0.9f);
                    float branchLen = 20 + rand.nextFloat() * 25;
                    int bx = (int) (ptsX[s] + Math.cos(branchAngle) * branchLen);
                    int by = (int) (ptsY[s] + Math.sin(branchAngle) * branchLen);

                    // Viền magma nhánh
                    g2d.setStroke(new java.awt.BasicStroke(thickness + 1f, java.awt.BasicStroke.CAP_ROUND,
                            java.awt.BasicStroke.JOIN_ROUND));
                    g2d.setColor(new Color(255, 60, 0, 200));
                    g2d.drawLine((int) ptsX[s], (int) ptsY[s], bx, by);

                    // Lõi đen nhánh
                    g2d.setStroke(new java.awt.BasicStroke(Math.max(1f, thickness - 1f), java.awt.BasicStroke.CAP_ROUND,
                            java.awt.BasicStroke.JOIN_ROUND));
                    g2d.setColor(new Color(15, 5, 0, 230));
                    g2d.drawLine((int) ptsX[s], (int) ptsY[s], bx, by);
                }
            }
        }

        // 3. Tâm vụ nổ magma
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, finalAlpha));
        g2d.setColor(new Color(255, 100, 0, 200));
        g2d.fillOval(cx - 20, cy - 15, 40, 30);
        g2d.setColor(new Color(255, 200, 50, 200));
        g2d.fillOval(cx - 10, cy - 7, 20, 14);

        // 4. Mảnh vỡ magma bay lên (thay vì hình tròn đều đặn)
        float eruptProgress = Math.min(1.0f, (float) elapsed / GROUND_VISUAL_DURATION);
        if (eruptProgress > 0 && eruptProgress < 1) {
            java.util.Random particleRand = new java.util.Random(Double.doubleToLongBits(tx * 321 + ty));
            int pCount = 12;
            for (int i = 0; i < pCount; i++) {
                double a = particleRand.nextDouble() * Math.PI * 2;
                float dist = 10 + particleRand.nextFloat() * 100 * eruptProgress; // Văng ra
                float heightOffset = (float) Math.sin(eruptProgress * Math.PI) * (20 + particleRand.nextFloat() * 40); // Đường
                                                                                                                       // parabol
                                                                                                                       // bay
                                                                                                                       // lên
                                                                                                                       // rồi
                                                                                                                       // rớt

                int px = cx + (int) (Math.cos(a) * dist);
                int py = cy + (int) (Math.sin(a) * dist) - (int) heightOffset;

                int size = 4 + particleRand.nextInt(6);

                // Chuyển màu từ vàng -> cam -> đỏ thẫm dần theo thời gian
                Color pColor;
                if (eruptProgress < 0.3f)
                    pColor = new Color(255, 200, 50);
                else if (eruptProgress < 0.6f)
                    pColor = new Color(255, 80, 0);
                else
                    pColor = new Color(80, 20, 10);

                g2d.setColor(pColor);
                g2d.fillRect(px - size / 2, py - size / 2, size, size); // Vẽ mảng vuông/mảnh vỡ
            }
        }

        // 5. Vòng bụi lan tỏa (Shockwave đất)
        float waveProgress = Math.min(1.0f, (float) elapsed / (GROUND_VISUAL_DURATION * 0.8f));
        if (waveProgress < 1.0f) {
            int ringSize = (int) (280 * waveProgress);
            float ringAlpha = finalAlpha * (1.0f - waveProgress) * 0.6f;
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, ringAlpha));

            // Vòng ngoài màu bụi đất
            g2d.setColor(new Color(200, 150, 100));
            g2d.setStroke(new java.awt.BasicStroke(10f + 10f * (1 - waveProgress)));
            g2d.drawOval(cx - ringSize / 2, cy - ringSize / 2, ringSize, ringSize);

            // Vòng trong màu lửa
            g2d.setColor(new Color(255, 100, 0));
            g2d.setStroke(new java.awt.BasicStroke(4f + 4f * (1 - waveProgress)));
            g2d.drawOval(cx - (ringSize - 10) / 2, cy - (ringSize - 10) / 2, ringSize - 10, ringSize - 10);
        }

        g2d.dispose();
    }

    private void drawBossNative(Graphics g, BufferedImage img) {
        Graphics2D g2d = (Graphics2D) g.create();
        long now = GamePanel.getTickTime();
        float scale = 2.0f;
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);
        int drawX = (int) x - drawW / 2 + size / 2;
        int drawY = (int) y - drawH + size;

        float alpha = 1.0f;
        if (currentState == State.TELEPORTING) {
            int frame = teleportAnim.getFrameIndex();
            if (frame <= 6)
                alpha = 1.0f - (float) frame / 7.0f;
            else
                alpha = (float) (frame - 7) / 7.0f;
        } else if (isDying) {
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        }
        g2d.setComposite(
                java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha))));
        if (!facingRight)
            g2d.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null);
        else
            g2d.drawImage(img, drawX, drawY, drawW, drawH, null);
        if (!isDying && now < hitFlashEndTime && currentState != State.TELEPORTING) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f * alpha));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(drawX, drawY, drawW, drawH);
        }
        g2d.dispose();
    }

    public String getName() {
        return "THE KING";
    }
}
