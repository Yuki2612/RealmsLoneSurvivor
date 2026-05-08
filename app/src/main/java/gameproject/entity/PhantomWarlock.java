package gameproject.entity;

import gameproject.*;
import gameproject.weapon.Projectile;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import java.util.Collections;

public class PhantomWarlock extends Enemy {
    public enum State {
        MOVING, ATTACK1, ATTACK2, SUMMONING, TELEPORTING, DEATH
    }

    private State currentState = State.MOVING;
    private String bossKey = "boss5";
    private boolean facingRight = true;
    private Random rand = new Random();

    // Phase tracking
    private boolean isPhase2 = false;
    private boolean transformTriggered = false;

    // Animations
    private Animation moveAnim;
    private Animation attack1Anim;
    private Animation attack2Anim;
    private Animation deathAnim;

    // Timers & Cooldowns
    private int actionTimer = 60;
    private long lastAttack1Time = 0;
    private long lastAttack2Time = 0;
    private long lastTeleportTime = 0;
    private long lastSkillTime = 0;

    private long attack1Cooldown = 5000;
    private long attack2Cooldown = 15000;
    private long summonCooldown = 15000;
    private long cloneCooldown = 15000;
    private long lastSummonTime = 0;
    private long globalSkillCooldown = 1500;
    private boolean isClonePhase = false;

    // Invisibility & Teleport
    private float currentAlpha = 1.0f;

    // Shooting
    private List<Projectile> pendingProjectiles = new ArrayList<>();
    private List<Enemy> spawnedEnemies = new ArrayList<>(); // Cho ảo ảnh (Clones) hoặc tường lửa

    // Phase 2 Mechanics
    private boolean hasReflectorShield = false;
    private long reflectorShieldStartTime = 0;
    private long reflectorShieldDuration = 4000; // 4 giây phản đòn

    // Battleground
    private boolean battlegroundActive = false;
    private float bgCenterX, bgCenterY;
    private float bgRadius = 750f;
    private long lastAcidRainDamageTime = 0;

    // Lồng giam khói
    private Rectangle[] cageWalls = null;
    private long cageStartTime = 0;
    private long cageDuration = 7000;
    private long lastCageDamageTime = 0;
    private static BufferedImage smokeParticle = null; // Cache ảnh hạt khói để tối ưu hiệu năng

    private boolean effectTriggered = false;

    private int currentSurviveTime = 0;

    public PhantomWarlock(float x, float y, int surviveTimeSeconds) {
        super(x, y, 100, (int) ((1200 + (surviveTimeSeconds * 8)) * 2f), 1.3f, new Color(50, 0, 80));
        this.isBoss = true;
        this.currentSurviveTime = surviveTimeSeconds;
        this.deathFadeDuration = 2000;

        initAnimations();
    }

    private void initAnimations() {
        // Load sprite sheets (ImageManager tự đọc số frame từ hậu tố _fN trong tên file)
        ImageManager.loadAnimation(bossKey + "_move", "app/res/" + bossKey + "_move.png");
        ImageManager.loadAnimation(bossKey + "_attack1", "app/res/" + bossKey + "_attack1.png");
        ImageManager.loadAnimation(bossKey + "_attack2", "app/res/" + bossKey + "_attack2.png");
        ImageManager.loadAnimation(bossKey + "_death", "app/res/" + bossKey + "_death.png");

        moveAnim = new Animation(8);
        moveAnim.setFrames(ImageManager.getAnimation(bossKey + "_move"));

        attack1Anim = new Animation(8);
        attack1Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack1"));

        attack2Anim = new Animation(8);
        attack2Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack2"));

        deathAnim = new Animation(7);
        deathAnim.setFrames(ImageManager.getAnimation(bossKey + "_death"));
        deathAnim.setLooping(false);
    }

    public String getName() {
        return "PHANTOM WARLOCK";
    }

    @Override
    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
        // Boss không bị đẩy lùi
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {
        long currentTime = GamePanel.getTickTime();

        if (isDying) {
            if (currentState != State.DEATH) {
                currentState = State.DEATH;
                if (deathAnim != null)
                    deathAnim.reset();
            }
            if (deathAnim != null) {
                deathAnim.update();
                if (deathAnim.hasFinishedCycle() && !effectTriggered) {
                    effectTriggered = true; // Đánh dấu đã phát nổ
                    panel.vfxManager.spawnDeathParticles(x + size / 2, y + size / 2, currentTime,
                            new Color(150, 0, 255));
                    SoundManager.play("explosion");
                }
            }
            return;
        }

        // Chuyển Phase 2
        if (!transformTriggered && hp < maxHp / 2) {
            transformTriggered = true;
            isPhase2 = true;
            speed = 1.5f;
            attack1Cooldown = 3500;
            attack2Cooldown = 12000;
            panel.vfxManager.showWaveBanner("THE PHANTOM WARLOCK AWAKENS!", new Color(150, 0, 255), currentTime);
            panel.vfxManager.triggerScreenShake(15);
            SoundManager.play("explosion");

            // Kích hoạt Battleground lấy người chơi làm trung tâm
            battlegroundActive = true;
            bgCenterX = playerX + Player.SIZE / 2f;
            bgCenterY = playerY + Player.SIZE / 2f;

            // Dịch chuyển boss cách xa người chơi một đoạn an toàn trong vòng bo
            float angle = (float) (Math.random() * Math.PI * 2);
            this.x = bgCenterX + (float) Math.cos(angle) * 250f - size / 2f;
            this.y = bgCenterY + (float) Math.sin(angle) * 250f - size / 2f;
            this.x = Math.max(0, Math.min(this.x, GamePanel.WORLD_WIDTH - size));
            this.y = Math.max(0, Math.min(this.y, GamePanel.WORLD_HEIGHT - size));
        }

        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float dx = playerX - centerX;
        float dy = playerY - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (currentState != State.TELEPORTING) {
            if (dx > 5)
                facingRight = true;
            else if (dx < -5)
                facingRight = false;
        }

        // --- Gọi hàm Phản Đòn ---
        if (hasReflectorShield) {
            handleReflectorShield(panel, currentTime);
            if (currentTime - reflectorShieldStartTime > reflectorShieldDuration) {
                hasReflectorShield = false;
            }
        }

        // --- Xử lý Mưa Acid ngoài Battleground (Phase 2) ---
        if (battlegroundActive) {
            handleBattlegroundDamage(panel, currentTime);
        }

        // --- Xử lý Lồng Giam Khói Độc ---
        if (cageWalls != null) {
            if (currentTime - cageStartTime > cageDuration) {
                cageWalls = null;
            } else {
                Rectangle playerRect = panel.player.getBounds();
                boolean isInSmoke = false;
                for (Rectangle wall : cageWalls) {
                    if (wall.intersects(playerRect)) {
                        isInSmoke = true;
                        break;
                    }
                }
                if (isInSmoke) {
                    if (currentTime - lastCageDamageTime > 500) {
                        panel.player.takeDamage(1);
                        panel.vfxManager.spawnDeathParticles(panel.player.getX() + Player.SIZE / 2,
                                panel.player.getY() + Player.SIZE / 2, currentTime, new Color(150, 0, 255));
                        lastCageDamageTime = currentTime;
                    }
                }
            }
        }

        switch (currentState) {
            case MOVING:
                if (moveAnim != null)
                    moveAnim.update();

                // Di chuyển mượt mà tới mục tiêu nhưng giữ khoảng cách an toàn
                float tDx = playerX - x;
                float tDy = playerY - y;
                float tDist = (float) Math.sqrt(tDx * tDx + tDy * tDy);

                if (dist > 250) {
                    // Tiến lại gần nếu quá xa
                    x += (tDx / tDist) * speed * speedMultiplier;
                    y += (tDy / tDist) * speed * speedMultiplier;
                } else if (dist < 150) {
                    // Lùi ra xa nếu quá gần
                    x -= (tDx / tDist) * speed * speedMultiplier;
                    y -= (tDy / tDist) * speed * speedMultiplier;
                }
                // Nếu ở khoảng cách 150 - 250, boss đứng xả skill (giữ nguyên vị trí)

                currentAlpha = 1.0f; // Luôn hiển thị ở trạng thái bình thường

                if (actionTimer > 0)
                    actionTimer--;

                if (actionTimer <= 0 && currentTime - lastSkillTime > globalSkillCooldown && currentAlpha >= 0.8f) {

                    // Attack 2 (Lồng giam hoặc Khiên phản đòn)
                    if (currentTime - lastAttack2Time > attack2Cooldown) {
                        currentState = State.ATTACK2;
                        if (attack2Anim != null)
                            attack2Anim.reset();
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                    // Attack 1 (Đạn rượt đuổi)
                    else if (currentTime - lastAttack1Time > attack1Cooldown) {
                        currentState = State.ATTACK1;
                        if (attack1Anim != null)
                            attack1Anim.reset();
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                    // Summon (Triệu hồi đệ tử)
                    else if (currentTime - lastSummonTime > summonCooldown) {
                        currentState = State.SUMMONING;
                        if (attack2Anim != null)
                            attack2Anim.reset(); // Dùng chung animation gõ gậy
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                    // Teleport (Phase 2): Gọi Phân thân theo thời gian
                    else if (isPhase2 && currentTime - lastTeleportTime > cloneCooldown) {
                        currentState = State.TELEPORTING;
                        currentAlpha = 1.0f;
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    }
                }
                break;

            case ATTACK1:
                if (attack1Anim != null) {
                    attack1Anim.update();
                    int triggerFrame = 3; // Tùy chọn frame xả đạn
                    if (attack1Anim.getFrameIndex() >= triggerFrame && !effectTriggered) {
                        fireCursedOrbs(panel.player);
                        lastAttack1Time = currentTime;
                        effectTriggered = true;
                        SoundManager.play("laser");
                    }

                    if (attack1Anim.hasFinishedCycle()) {
                        currentState = State.MOVING;
                        actionTimer = 40;
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case ATTACK2:
                if (attack2Anim != null) {
                    attack2Anim.update();
                    int triggerFrame = 4;
                    if (attack2Anim.getFrameIndex() >= triggerFrame && !effectTriggered) {
                        if (isPhase2 && rand.nextBoolean()) {
                            // 50% tạo khiên phản đòn ở Phase 2
                            hasReflectorShield = true;
                            reflectorShieldStartTime = currentTime;
                            panel.vfxManager.showWaveBanner("REFLECTOR SHIELD!", Color.MAGENTA, currentTime);
                            SoundManager.play("shield");
                        } else {
                            // Tạo tường lồng giam
                            createPhantomCage(playerX, playerY);
                            SoundManager.play("explosion");
                            panel.vfxManager.triggerScreenShake(8);
                        }

                        lastAttack2Time = currentTime;
                        effectTriggered = true;
                    }

                    if (attack2Anim.hasFinishedCycle()) {
                        currentState = State.MOVING;
                        actionTimer = 50;
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case SUMMONING:
                if (attack2Anim != null) {
                    attack2Anim.update();
                    int triggerFrame = 4;
                    if (attack2Anim.getFrameIndex() >= triggerFrame && !effectTriggered) {
                        summonGhostMobs();
                        lastSummonTime = currentTime;
                        effectTriggered = true;
                        panel.vfxManager.showWaveBanner("NECROMANCY!", new Color(150, 255, 50), currentTime);
                        SoundManager.play("explosion");
                    }

                    if (attack2Anim.hasFinishedCycle()) {
                        currentState = State.MOVING;
                        actionTimer = 40;
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case TELEPORTING:
                // Biến mất nhanh
                currentAlpha -= 0.05f;
                if (currentAlpha <= 0 && !effectTriggered) {
                    summonClones(panel);

                    panel.vfxManager.spawnDeathParticles(x + size / 2, y + size / 2, currentTime,
                            new Color(150, 50, 255));
                    SoundManager.play("explosion");
                    effectTriggered = true;
                }

                if (effectTriggered) {
                    currentAlpha += 0.05f;
                    if (currentAlpha >= 1.0f) {
                        currentAlpha = 1.0f;
                        currentState = State.MOVING;
                        lastTeleportTime = currentTime;
                        actionTimer = 30;
                    }
                }
                break;

            case DEATH:
                break;
        }
    }

    // --- Kỹ năng: Đạn Lời Nguyền (Bám đuổi) ---
    private void fireCursedOrbs(Player player) {
        float cx = x + size / 2;
        float cy = y + size / 2;

        int orbCount = isPhase2 ? 6 : 4;
        float spreadAngle = 2.0f; // Góc tỏa rộng hơn để đạn tản ra tứ phía
        float baseAngle = (float) Math.atan2((player.getY() + Player.SIZE / 2) - cy,
                (player.getX() + Player.SIZE / 2) - cx);

        for (int i = 0; i < orbCount; i++) {
            float angle = baseAngle + (i - orbCount / 2f + 0.5f) * (spreadAngle / (orbCount - 1));
            float targetX = cx + (float) Math.cos(angle) * 500;
            float targetY = cy + (float) Math.sin(angle) * 500;

            Projectile p = new Projectile(cx, cy, targetX, targetY, 0.2f, 1500f);
            p.isEnemyBullet = true;
            p.isHoming = true;
            p.targetPlayer = player;

            // Random hóa tốc độ và khả năng bẻ lái để chúng tách rời nhau ra, bay lả lướt
            float randomSpeedVariance = (float) Math.random() * 1.5f;
            p.speedX = p.speedX * (1.0f + randomSpeedVariance);
            p.speedY = p.speedY * (1.0f + randomSpeedVariance);
            p.homingTurnSpeed = 0.01f + (float) Math.random() * 0.02f; // Bẻ lái ngẫu nhiên
            p.size = 28; // Kích thước bóng ma to hơn
            p.damage = 1;
            p.isPurpleGhost = true;

            pendingProjectiles.add(p);
        }
    }

    // --- Kỹ năng: Lồng Giam Linh Hồn ---
    private void createPhantomCage(float playerX, float playerY) {
        float px = playerX + Player.SIZE / 2;
        float py = playerY + Player.SIZE / 2;
        float cageRadius = 320f; // Mở rộng lồng ra to hơn
        float wallThickness = 45f; // Giữ độ mỏng
        // Để 4 bức tường ghép mí với nhau vừa khít và không lòi góc ra ngoài (tránh vẽ
        // trùng ở 4 góc)
        float wallLength = cageRadius * 2 + wallThickness;

        cageWalls = new Rectangle[4];
        // Tường trên (Kéo dài hết chiều ngang)
        cageWalls[0] = new Rectangle((int) (px - wallLength / 2), (int) (py - cageRadius - wallThickness / 2),
                (int) wallLength, (int) wallThickness);
        // Tường dưới (Kéo dài hết chiều ngang)
        cageWalls[1] = new Rectangle((int) (px - wallLength / 2), (int) (py + cageRadius - wallThickness / 2),
                (int) wallLength, (int) wallThickness);
        // Tường trái (Nằm lọt lòng giữa tường trên và dưới)
        float sideLength = cageRadius * 2 - wallThickness;
        cageWalls[2] = new Rectangle((int) (px - cageRadius - wallThickness / 2), (int) (py - sideLength / 2),
                (int) wallThickness, (int) sideLength);
        // Tường phải (Nằm lọt lòng giữa tường trên và dưới)
        cageWalls[3] = new Rectangle((int) (px + cageRadius - wallThickness / 2), (int) (py - sideLength / 2),
                (int) wallThickness, (int) sideLength);

        cageStartTime = GamePanel.getTickTime();
    }

    // Khởi tạo trước một mảng khói ảo (Pre-rendered Soft Particle)
    // Giúp tăng FPS cực mạnh vì không phải dùng fillOval vẽ hàng nghìn hình tròn
    // mỗi frame
    private void initSmokeParticle() {
        if (smokeParticle != null)
            return;
        int size = 60;
        smokeParticle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smokeParticle.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int r1 = size / 2;
        g.setColor(new Color(150, 20, 255, 30));
        g.fillOval(size / 2 - r1, size / 2 - r1, r1 * 2, r1 * 2);

        int r2 = (int) (r1 * 0.7);
        g.setColor(new Color(120, 0, 200, 60));
        g.fillOval(size / 2 - r2, size / 2 - r2, r2 * 2, r2 * 2);

        int r3 = (int) (r1 * 0.4);
        g.setColor(new Color(80, 0, 150, 120));
        g.fillOval(size / 2 - r3, size / 2 - r3, r3 * 2, r3 * 2);

        g.dispose();
    }

    // --- Xử lý Khiên Phản Đòn ---
    private void handleReflectorShield(GamePanel panel, long currentTime) {
        Rectangle bossRect = getBounds();
        for (Projectile p : panel.entityManager.projectiles) {
            // Đạn của người chơi bay vào boss
            if (!p.isEnemyBullet && p.getBounds().intersects(bossRect)) {
                // Đảo ngược hướng đạn, chuyển thành đạn địch
                p.isEnemyBullet = true;
                p.speedX = -p.speedX;
                p.speedY = -p.speedY;

                // Mất Homing nếu có
                p.isHoming = false;

                // Thêm hiệu ứng phản đòn
                panel.vfxManager.spawnComboSparkles(p.startX, p.startY, currentTime, Color.MAGENTA, 3);
            }
        }
    }

    // --- Xử lý Battleground Mưa Acid ---
    private void handleBattlegroundDamage(GamePanel panel, long currentTime) {
        float px = panel.player.getX() + Player.SIZE / 2;
        float py = panel.player.getY() + Player.SIZE / 2;
        float distToCenter = (float) Math.sqrt(Math.pow(px - bgCenterX, 2) + Math.pow(py - bgCenterY, 2));

        if (distToCenter > bgRadius) {
            // Đứng ngoài vùng an toàn -> Nhận sát thương mỗi giây
            if (currentTime - lastAcidRainDamageTime > 1000) {
                panel.player.takeDamage(1);
                panel.vfxManager.spawnDeathParticles(px, py, currentTime, Color.GREEN);
                lastAcidRainDamageTime = currentTime;
            }
        }
    }

    // --- Kỹ năng: Triệu hồi Quái (Necromancy) ---
    private void summonGhostMobs() {
        int mobCount = isPhase2 ? 6 : 4;
        for (int i = 0; i < mobCount; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float cx = x + (float) Math.cos(angle) * 150;
            float cy = y + (float) Math.sin(angle) * 150;

            // Ép tọa độ trong bản đồ
            cx = Math.max(0, Math.min(cx, GamePanel.WORLD_WIDTH - 50));
            cy = Math.max(0, Math.min(cy, GamePanel.WORLD_HEIGHT - 50));

            // Dùng AssassinEnemy làm đệ tử vì chúng chạy nhanh (Sát thủ bóng ma)
            // HP được tự tính trong constructor của AssassinEnemy dựa trên surviveTimeSeconds
            Enemy minion = new AssassinEnemy(cx, cy, 5, currentSurviveTime);
            minion.speed = 1.5f; // Chạy rất nhanh
            spawnedEnemies.add(minion);
        }
    }

    // --- Triệu hồi Ảo ảnh (Clones) ---
    private void summonClones(GamePanel panel) {
        isClonePhase = true;

        float px = panel.player.getX();
        float py = panel.player.getY();

        float[][] corners = {
                { px - 400, py - 400 },
                { px + 400, py - 400 },
                { px - 400, py + 400 },
                { px + 400, py + 400 }
        };

        for (int i = 0; i < 4; i++) {
            corners[i][0] = Math.max(0, Math.min(corners[i][0], GamePanel.WORLD_WIDTH - size));
            corners[i][1] = Math.max(0, Math.min(corners[i][1], GamePanel.WORLD_HEIGHT - size));
        }

        List<float[]> cornerList = Arrays.asList(corners);
        Collections.shuffle(cornerList);

        this.x = cornerList.get(0)[0];
        this.y = cornerList.get(0)[1];

        for (int i = 1; i < 4; i++) {
            PhantomClone clone = new PhantomClone(cornerList.get(i)[0], cornerList.get(i)[1], this, panel);
            spawnedEnemies.add(clone);
        }
    }

    @Override
    public List<Projectile> shoot() {
        if (!pendingProjectiles.isEmpty()) {
            List<Projectile> result = new ArrayList<>(pendingProjectiles);
            pendingProjectiles.clear();
            return result;
        }
        return null;
    }

    @Override
    public List<Enemy> summon() {
        if (!spawnedEnemies.isEmpty()) {
            List<Enemy> result = new ArrayList<>(spawnedEnemies);
            spawnedEnemies.clear();
            return result;
        }
        return null;
    }

    @Override
    public void takeDamage(int damage, boolean isCrit, VFXManager vfxManager, long currentTime) {
        if (currentState == State.TELEPORTING)
            return; // Né đạn khi đang dịch chuyển
        if (hasReflectorShield)
            return; // Không nhận sát thương khi đang có khiên phản đòn
        if (isClonePhase) {
            isClonePhase = false;
        }
        super.takeDamage(damage, isCrit, vfxManager, currentTime);
    }

    @Override
    public void takeDamage(int damage, VFXManager vfxManager, long currentTime) {
        if (currentState == State.TELEPORTING)
            return; // Né đạn khi đang dịch chuyển
        if (hasReflectorShield)
            return;
        if (isClonePhase) {
            isClonePhase = false;
        }
        super.takeDamage(damage, vfxManager, currentTime);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // --- Vẽ Battleground ---
        if (battlegroundActive) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

            // Vẽ toàn màn hình tối đi
            Area outer = new Area(new Rectangle(0, 0, GamePanel.WORLD_WIDTH, GamePanel.WORLD_HEIGHT));
            // Trừ đi hình tròn an toàn ở giữa
            Area inner = new Area(new java.awt.geom.Ellipse2D.Float(bgCenterX - bgRadius, bgCenterY - bgRadius,
                    bgRadius * 2, bgRadius * 2));
            outer.subtract(inner);

            g2d.setColor(new Color(20, 0, 40));
            g2d.fill(outer);

            // Viền ranh giới
            g2d.setColor(Color.MAGENTA);
            g2d.drawOval((int) (bgCenterX - bgRadius), (int) (bgCenterY - bgRadius), (int) bgRadius * 2,
                    (int) bgRadius * 2);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        // --- Vẽ Khiên Phản Đòn ---
        if (hasReflectorShield) {
            g2d.setColor(new Color(255, 0, 255, 100));
            g2d.fillOval((int) x - 20, (int) y - 20, size + 40, size + 40);
            g2d.setColor(Color.MAGENTA);
            g2d.drawOval((int) x - 20, (int) y - 20, size + 40, size + 40);
        }

        // --- Vẽ Lồng Giam Khói Tím ---
        if (cageWalls != null) {
            if (smokeParticle == null)
                initSmokeParticle();

            long elapsed = GamePanel.getTickTime() - cageStartTime;
            float alpha = 1.0f;
            if (elapsed > cageDuration - 1000) {
                alpha = Math.max(0f, 1.0f - (elapsed - (cageDuration - 1000)) / 1000f);
            } else if (elapsed < 500) {
                alpha = elapsed / 500f;
            }

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int pSize = smokeParticle.getWidth();
            for (Rectangle wall : cageWalls) {
                boolean isHorizontal = wall.width > wall.height;
                int length = isHorizontal ? wall.width : wall.height;
                int spacing = 20; // Khoảng cách giữa các cụm khói
                int numParticles = length / spacing + 1;

                // Dùng Pseudo-random cố định vị trí ban đầu
                Random fogRand = new Random(wall.x * 1000L + wall.y);

                for (int i = 0; i < numParticles; i++) {
                    // Trải dọc theo chiều dài tường
                    int baseX = isHorizontal ? wall.x + i * spacing : wall.x + wall.width / 2 - pSize / 2;
                    int baseY = isHorizontal ? wall.y + wall.height / 2 - pSize / 2 : wall.y + i * spacing;

                    // Thêm dao động xê dịch vị trí cố định để trông lởm chởm tự nhiên
                    int offsetX = fogRand.nextInt(20) - 10;
                    int offsetY = fogRand.nextInt(20) - 10;

                    // Dao động lượn lờ bồng bềnh theo nhịp sóng thời gian
                    float shiftX = (float) Math.sin((elapsed + i * 60) / 400.0) * 10f;
                    float shiftY = (float) Math.cos((elapsed + i * 60) / 400.0) * 10f;

                    // Vẽ hình ảnh khói pre-rendered (Cực kỳ nhẹ so với fillOval)
                    g2d.drawImage(smokeParticle, baseX + offsetX + (int) shiftX, baseY + offsetY + (int) shiftY, null);
                }
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        // --- Thiết lập độ trong suốt (Alpha) cho tàng hình ---
        if (currentAlpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, currentAlpha)));
        }

        BufferedImage img = null;
        if (isDying) {
            img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
        } else {
            switch (currentState) {
                case MOVING:
                case TELEPORTING:
                    img = (moveAnim != null) ? moveAnim.getCurrentFrame() : null;
                    break;
                case ATTACK1:
                    img = (attack1Anim != null) ? attack1Anim.getCurrentFrame() : null;
                    break;
                case ATTACK2:
                case SUMMONING:
                    img = (attack2Anim != null) ? attack2Anim.getCurrentFrame() : null;
                    break;
                case DEATH:
                    img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
                    break;
            }
        }

        if (img != null) {
            // 1. Chỉ dùng ĐÚNG MỘT hệ số Scale cho toàn bộ các trạng thái
            float scale = 2.0f; // Đã giảm độ to chung của nhân vật xuống 1 chút

            // 2. Lấy kích thước THẬT của khung hình đang chạy
            int imgW = img.getWidth();
            int imgH = img.getHeight();

            // 3. Tính toán kích thước vẽ ra màn hình
            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);

            // 4. CHỐT TỌA ĐỘ
            // Căn tâm X vào giữa cơ thể:
            int drawX = (int) (x + size / 2) - drawW / 2;
            // Ép tọa độ Y chạm đúng gót chân (Trừ đi toàn bộ chiều cao của ảnh):
            int drawY = (int) (y + size) - drawH;

            // 5. Vẽ và lật mặt (Flip)
            if (!facingRight) {
                g2d.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null);
            } else {
                g2d.drawImage(img, drawX, drawY, drawW, drawH, null);
            }
        } else {
            // Fallback
            g2d.setColor(new Color(150, 50, 200));
            g2d.fillRect((int) x, (int) y, size, size);
        }

        g2d.dispose();
    }

    // Lớp nội (Inner Class) đại diện cho phân thân của Boss
    public class PhantomClone extends Enemy {
        private PhantomWarlock parent;
        private GamePanel panel;
        private long lastShootTime;

        public PhantomClone(float x, float y, PhantomWarlock parent, GamePanel panel) {
            super(x, y, parent.size, 10, 0, new Color(50, 0, 80));
            this.parent = parent;
            this.panel = panel;
            this.hp = 10;
            this.maxHp = 10;
            this.lastShootTime = GamePanel.getTickTime();
        }

        @Override
        public void update(float px, float py, float speedMulti, ArrayList<Enemy> enemies, int w, int h,
                GamePanel panel) {
            if (!parent.isClonePhase) {
                this.hp = 0; // Tự hủy nếu bản thể bị đánh trúng
                return;
            }

            // Đứng im, bắn đạn truy đuổi mỗi 3 giây
            if (GamePanel.getTickTime() - lastShootTime > 3000) {
                float angle = (float) Math.atan2((py + Player.SIZE / 2) - (y + size / 2),
                        (px + Player.SIZE / 2) - (x + size / 2));
                float targetX = x + size / 2 + (float) Math.cos(angle) * 500;
                float targetY = y + size / 2 + (float) Math.sin(angle) * 500;

                Projectile p = new Projectile(x + size / 2, y + size / 2, targetX, targetY, 0.2f, 1500f);
                p.isEnemyBullet = true;
                p.isHoming = true;
                p.targetPlayer = panel.player;
                p.speedX *= 1.5f;
                p.speedY *= 1.5f;
                p.homingTurnSpeed = 0.015f;
                p.size = 28;
                p.damage = 1;
                p.isPurpleGhost = true;

                parent.pendingProjectiles.add(p);
                lastShootTime = GamePanel.getTickTime();
            }
        }

        private boolean isDeathTriggered = false;

        @Override
        public boolean shouldRemove() {
            boolean removed = super.shouldRemove();
            if (removed && !isDeathTriggered) {
                isDeathTriggered = true;
                if (parent.isClonePhase) {
                    // Nếu bị người chơi bắn chết: Phá hủy tạo ra quái vật và bắn 3 đạn truy đuổi
                    for (int i = 0; i < 2; i++) {
                        Enemy e = new AssassinEnemy(x, y, 5, parent.currentSurviveTime);
                        parent.spawnedEnemies.add(e);
                    }

                    for (int i = 0; i < 3; i++) {
                        float angle = (float) (Math.random() * Math.PI * 2);
                        float targetX = x + size / 2 + (float) Math.cos(angle) * 500;
                        float targetY = y + size / 2 + (float) Math.sin(angle) * 500;
                        Projectile p = new Projectile(x + size / 2, y + size / 2, targetX, targetY, 0.2f, 1500f);
                        p.isEnemyBullet = true;
                        p.isHoming = true;
                        p.targetPlayer = panel.player;
                        p.speedX *= 1.5f;
                        p.speedY *= 1.5f;
                        p.homingTurnSpeed = 0.015f;
                        p.size = 28;
                        p.damage = 1;
                        p.isPurpleGhost = true;
                        parent.pendingProjectiles.add(p);
                    }
                }
            }
            return removed;
        }

        @Override
        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            BufferedImage img = (parent.moveAnim != null) ? parent.moveAnim.getCurrentFrame() : null;
            if (img != null) {
                float scale = 2.0f;
                int imgW = img.getWidth();
                int imgH = img.getHeight();
                int drawW = (int) (imgW * scale);
                int drawH = (int) (imgH * scale);
                int drawX = (int) (x + size / 2) - drawW / 2;
                int drawY = (int) (y + size) - drawH;

                // Mờ ảo hơn bản thể một chút
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));

                // Ln quay mặt theo parent hoặc lật ngẫu nhiên
                if (!parent.facingRight) {
                    g2d.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null);
                } else {
                    g2d.drawImage(img, drawX, drawY, drawW, drawH, null);
                }
            }
            g2d.dispose();
        }

        @Override
        public String getName() {
            return "PHANTOM CLONE";
        }
    }
}
