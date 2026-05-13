package gameproject.entity;

import gameproject.*;
import gameproject.weapon.Projectile;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PriestBoss extends Enemy {
    public enum State {
        WALKING, ATTACKING_1, TELEPORTING, TRANSFORMING, ATTACKING_2, ATTACKING_3, ATTACKING_4, DYING
    }

    private State currentState = State.WALKING;
    private String bossKey = "boss6";
    private boolean facingRight = true;
    private Random rand = new Random();

    // Phase tracking
    private boolean isPhase2 = false;
    private boolean transformTriggered = false;
    private int transformStep = 0; // 0: transform1, 1: transform2

    // Animations
    private Animation walk1Anim;
    private Animation walk2Anim;
    private Animation teleport1Anim;
    private Animation teleport2Anim;
    private Animation attack1Anim;
    private Animation attack2Anim; // Dash
    private Animation attack3Anim; // Bullet Hell
    private Animation transform1Anim;
    private Animation transform2Anim;

    // Timers & Cooldowns
    private int actionTimer = 60;
    private long lastSkillTime = 0;
    private long globalSkillCooldown = 1500;
    
    private long lastAttack1Time = 0;
    private long attack1Cooldown = 2500;
    
    private long lastAttack2Time = 0;
    private long attack2Cooldown = 4000;
    
    private long lastAttack3Time = 0;
    private long attack3Cooldown = 3500;
    
    private long lastAttack4Time = 0;
    private long attack4Cooldown = 6000;

    // Logic variables
    private List<Projectile> pendingProjectiles = new ArrayList<>();
    private float targetX, targetY;
    private boolean effectTriggered = false;
    private int attackCycle = 0;
    private long lastParticleTime = 0;

    public PriestBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 70, (int) ((1200 + (surviveTimeSeconds * 10)) * 1.7f), 1.2f, new Color(255, 215, 0));
        this.isBoss = true;
        this.deathFadeDuration = 3000;
        initAnimations();
    }

    private void initAnimations() {
        ImageManager.loadAnimation(bossKey + "_walk1", "app/res/" + bossKey + "_walk1_f6.png", 6);
        ImageManager.loadAnimation(bossKey + "_walk2", "app/res/" + bossKey + "_walk2_f6.png", 6);
        ImageManager.loadAnimation(bossKey + "_teleport1", "app/res/" + bossKey + "_teleport1_f4.png", 4);
        ImageManager.loadAnimation(bossKey + "_teleport2", "app/res/" + bossKey + "_teleport2_f4.png", 4);
        ImageManager.loadAnimation(bossKey + "_attack1", "app/res/" + bossKey + "_attack1_f6.png", 6);
        ImageManager.loadAnimation(bossKey + "_attack2", "app/res/" + bossKey + "_attack2_f6.png", 6);
        ImageManager.loadAnimation(bossKey + "_attack3", "app/res/" + bossKey + "_attack3_f6.png", 6);
        ImageManager.loadAnimation(bossKey + "_transform1", "app/res/" + bossKey + "_transform1_f6.png", 6);
        ImageManager.loadAnimation(bossKey + "_transform2", "app/res/" + bossKey + "_transform2_f2.png", 2);

        walk1Anim = new Animation(6);
        walk1Anim.setFrames(ImageManager.getAnimation(bossKey + "_walk1"));

        walk2Anim = new Animation(6);
        walk2Anim.setFrames(ImageManager.getAnimation(bossKey + "_walk2"));

        teleport1Anim = new Animation(5);
        teleport1Anim.setFrames(ImageManager.getAnimation(bossKey + "_teleport1"));
        teleport1Anim.setLooping(false);

        teleport2Anim = new Animation(5);
        teleport2Anim.setFrames(ImageManager.getAnimation(bossKey + "_teleport2"));
        teleport2Anim.setLooping(false);

        attack1Anim = new Animation(5);
        attack1Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack1"));
        attack1Anim.setLooping(false);

        attack2Anim = new Animation(5);
        attack2Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack2"));
        attack2Anim.setLooping(false);

        attack3Anim = new Animation(4);
        attack3Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack3"));
        attack3Anim.setLooping(false);

        transform1Anim = new Animation(8);
        transform1Anim.setFrames(ImageManager.getAnimation(bossKey + "_transform1"));
        transform1Anim.setLooping(false);

        transform2Anim = new Animation(10);
        transform2Anim.setFrames(ImageManager.getAnimation(bossKey + "_transform2"));
        transform2Anim.setLooping(false);
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {

        long currentTime = GamePanel.getTickTime();

        if (isDying) {
            if (currentState != State.DYING) {
                currentState = State.DYING;
                deathFadeStartTime = currentTime;
            }
            // Rising particles on death
            if (currentTime - lastParticleTime > 100) {
                panel.vfxManager.spawnToxicSmoke(x + rand.nextInt(size), y + rand.nextInt(size), currentTime);
                lastParticleTime = currentTime;
            }
            return;
        }

        // Phase Transition Check
        if (!transformTriggered && hp < maxHp * 0.75f) {
            transformTriggered = true;
            currentState = State.TRANSFORMING;
            transformStep = 0;
            transform1Anim.reset();
            panel.vfxManager.showWaveBanner("THE PRIEST SUMMONS HIS SERVANT!", new Color(255, 200, 0), currentTime);
            panel.vfxManager.triggerScreenShake(20);
            SoundManager.play("shield");
        }

        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float dx = playerX - centerX;
        float dy = playerY - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (currentState != State.TRANSFORMING) {
            if (dx > 5) facingRight = true;
            else if (dx < -5) facingRight = false;
        }

        switch (currentState) {
            case WALKING:
                if (isPhase2) walk2Anim.update();
                else walk1Anim.update();

                EnemyController.moveEnemy(this, panel, speedMultiplier);

                if (actionTimer > 0) actionTimer--;

                if (actionTimer <= 0 && currentTime - lastSkillTime > globalSkillCooldown) {
                    // Teleport if too far
                    if (dist > 700) {
                        currentState = State.TELEPORTING;
                        if (isPhase2) teleport2Anim.reset();
                        else teleport1Anim.reset();
                        
                        float angle = (float) (Math.random() * Math.PI * 2);
                        float offset = 200f;
                        targetX = playerX + (float) Math.cos(angle) * offset;
                        targetY = playerY + (float) Math.sin(angle) * offset;
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    } 
                    else if (!isPhase2) {
                        // Phase 1 Attacks
                        if (dist < 500 && currentTime - lastAttack1Time > attack1Cooldown) {
                            currentState = State.ATTACKING_1;
                            attack1Anim.reset();
                            effectTriggered = false;
                            lastAttack1Time = currentTime;
                            lastSkillTime = currentTime;
                        }
                    } else {
                        // Phase 2 Attacks
                        // Priority: Attack 4 (Toxic) -> Attack 2 (Dash) -> Attack 3 (Bullet)
                        if (currentTime - lastAttack4Time > attack4Cooldown) {
                            currentState = State.ATTACKING_4;
                            attack3Anim.reset(); // Uses attack3 animation
                            effectTriggered = false;
                            attackCycle = 0;
                            lastAttack4Time = currentTime;
                            lastSkillTime = currentTime;
                        }
                        else if (dist > 300 && dist < 600 && currentTime - lastAttack2Time > attack2Cooldown) {
                            currentState = State.ATTACKING_2;
                            attack2Anim.reset();
                            targetX = playerX;
                            targetY = playerY;
                            effectTriggered = false;
                            lastAttack2Time = currentTime;
                            lastSkillTime = currentTime;
                        }
                        else if (dist < 550 && currentTime - lastAttack3Time > attack3Cooldown) {
                            currentState = State.ATTACKING_3;
                            attack3Anim.reset();
                            effectTriggered = false;
                            attackCycle = 0;
                            lastAttack3Time = currentTime;
                            lastSkillTime = currentTime;
                        }
                    }
                }
                break;

            case TELEPORTING:
                Animation tpAnim = isPhase2 ? teleport2Anim : teleport1Anim;
                tpAnim.update();
                if (tpAnim.getFrameIndex() == tpAnim.getTotalFrames() / 2 && !effectTriggered) {
                    this.x = Math.max(0, Math.min(targetX - size/2, GamePanel.WORLD_WIDTH - size));
                    this.y = Math.max(0, Math.min(targetY - size/2, GamePanel.WORLD_HEIGHT - size));
                    panel.vfxManager.spawnComboSparkles(x + size/2, y + size/2, currentTime, Color.YELLOW, 5);
                    SoundManager.play("shield");
                    effectTriggered = true;
                }
                if (tpAnim.hasFinishedCycle()) {
                    currentState = State.WALKING;
                    actionTimer = 20;
                }
                break;

            case ATTACKING_1:
                attack1Anim.update();
                if (attack1Anim.getFrameIndex() == 3 && !effectTriggered) {
                    fireYellowFan(playerX, playerY, 6, 60);
                    SoundManager.play("laser");
                    effectTriggered = true;
                }
                if (attack1Anim.hasFinishedCycle()) {
                    currentState = State.WALKING;
                    actionTimer = 30;
                }
                break;

            case ATTACKING_2: // Dash (Soul Reaper style)
                attack2Anim.update();
                int idx2 = attack2Anim.getFrameIndex();
                if (idx2 >= 2 && idx2 <= 4) {
                    float adx = targetX - centerX;
                    float ady = targetY - centerY;
                    float adist = (float) Math.sqrt(adx * adx + ady * ady);
                    if (adist > 20) {
                        float dashSpeed = 14.0f;
                        x += (adx / adist) * dashSpeed;
                        y += (ady / adist) * dashSpeed;
                        EnemyController.resolveHybridCollision(this, panel.mapManager);
                    }
                    if (idx2 == 3 && !effectTriggered) {
                        checkMeleeHit(panel, 110);
                        panel.vfxManager.triggerScreenShake(10);
                        effectTriggered = true;
                    }
                }
                if (attack2Anim.hasFinishedCycle()) {
                    currentState = State.WALKING;
                    actionTimer = 40;
                }
                break;

            case ATTACKING_3: // Advanced Bullet Hell
                attack3Anim.update();
                if (attack3Anim.getFrameIndex() == 2 && !effectTriggered) {
                    if (attackCycle == 0) {
                        fireYellowFan(playerX, playerY, 10, 80);
                        attackCycle = 1;
                        attack3Anim.reset();
                    } else if (attackCycle == 1) {
                        fireYellowFan(playerX, playerY, 10, 80, 15); // Offset 15 degrees
                        effectTriggered = true;
                    }
                    SoundManager.play("laser");
                }
                if (attack3Anim.hasFinishedCycle() && effectTriggered) {
                    currentState = State.WALKING;
                    actionTimer = 35;
                }
                break;

            case ATTACKING_4: // Toxic Pools
                attack3Anim.update();
                if (attack3Anim.getFrameIndex() == 2 && !effectTriggered) {
                    // Summon toxic pools at random locations near player
                    for (int i = 0; i < 3; i++) {
                        float tx = playerX + rand.nextInt(300) - 150;
                        float ty = playerY + rand.nextInt(300) - 150;
                        panel.vfxManager.fireZones.add(new VFXManager.FireZone(tx - 60, ty - 60, currentTime + 5000, false, false, 120));
                        // Add a marker that it's toxic
                        VFXManager.FireZone fz = panel.vfxManager.fireZones.get(panel.vfxManager.fireZones.size() - 1);
                        fz.isToxic = true;
                    }
                    SoundManager.play("shield");
                    effectTriggered = true;
                }
                if (attack3Anim.hasFinishedCycle()) {
                    currentState = State.WALKING;
                    actionTimer = 50;
                }
                break;

            case TRANSFORMING:
                if (transformStep == 0) {
                    transform1Anim.update();
                    if (transform1Anim.hasFinishedCycle()) {
                        transformStep = 1;
                        transform2Anim.reset();
                    }
                } else {
                    transform2Anim.update();
                    if (transform2Anim.hasFinishedCycle()) {
                        isPhase2 = true;
                        speed = 1.5f;
                        currentState = State.WALKING;
                        actionTimer = 30;
                    }
                }
                break;

            case DYING:
                break;
        }
    }

    private void fireYellowFan(float px, float py, int count, float spreadAngle) {
        fireYellowFan(px, py, count, spreadAngle, 0);
    }

    private void fireYellowFan(float px, float py, int count, float spreadAngle, float offsetAngle) {
        float cx = x + size / 2;
        float cy = y + size / 2;
        float baseAngle = (float) Math.toDegrees(Math.atan2(py - cy, px - cx)) + offsetAngle;
        float startAngle = baseAngle - spreadAngle / 2f;
        float step = spreadAngle / (count - 1);

        for (int i = 0; i < count; i++) {
            float angle = (float) Math.toRadians(startAngle + i * step);
            float tx = cx + (float) Math.cos(angle) * 500;
            float ty = cy + (float) Math.sin(angle) * 500;

            Projectile p = new Projectile(cx, cy, tx, ty, 0.5f, 650f);
            p.isEnemyBullet = true;
            p.isPriestBullet = true;
            p.damage = 1;
            pendingProjectiles.add(p);
        }
    }

    private void checkMeleeHit(GamePanel panel, float range) {
        float pCenterX = panel.player.getX() + Player.SIZE / 2;
        float pCenterY = panel.player.getY() + Player.SIZE / 2;
        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float d = (float) Math.sqrt(Math.pow(pCenterX - centerX, 2) + Math.pow(pCenterY - centerY, 2));
        if (d < range) {
            panel.player.takeDamage(1);
        }
    }

    @Override
    public List<Projectile> shoot() {
        if (pendingProjectiles.isEmpty()) return null;
        List<Projectile> res = new ArrayList<>(pendingProjectiles);
        pendingProjectiles.clear();
        return res;
    }

    @Override
    public void takeDamage(int damage, boolean isCrit, VFXManager vfxManager, long currentTime) {
        if (currentState == State.TRANSFORMING) return;
        super.takeDamage(damage, isCrit, vfxManager, currentTime);
    }

    @Override
    public void draw(Graphics g) {
        BufferedImage img = null;
        if (isDying) {
            img = isPhase2 ? walk2Anim.getCurrentFrame() : walk1Anim.getCurrentFrame();
        } else {
            switch (currentState) {
                case WALKING:
                    img = isPhase2 ? walk2Anim.getCurrentFrame() : walk1Anim.getCurrentFrame();
                    break;
                case ATTACKING_1:
                    img = attack1Anim.getCurrentFrame();
                    break;
                case ATTACKING_2:
                    img = attack2Anim.getCurrentFrame();
                    break;
                case ATTACKING_3:
                case ATTACKING_4:
                    img = attack3Anim.getCurrentFrame();
                    break;
                case TELEPORTING:
                    img = isPhase2 ? teleport2Anim.getCurrentFrame() : teleport1Anim.getCurrentFrame();
                    break;
                case TRANSFORMING:
                    img = (transformStep == 0) ? transform1Anim.getCurrentFrame() : transform2Anim.getCurrentFrame();
                    break;
            }
        }

        if (img != null) {
            drawPriest(g, img);
        }

        if (GamePanel.showHitboxes) {
            g.setColor(Color.RED);
            Rectangle r = getBounds();
            g.drawRect(r.x, r.y, r.width, r.height);
        }
    }

    private void drawPriest(Graphics g, BufferedImage img) {
        Graphics2D g2d = (Graphics2D) g.create();
        long now = GamePanel.getTickTime();
        float scale = isPhase2 ? 2.8f : 2.5f;
        int drawW = (int) (img.getWidth() * scale);
        int drawH = (int) (img.getHeight() * scale);
        int drawX = (int) x - drawW / 2 + size / 2;
        int drawY = (int) y - drawH + size;

        float alpha = 1.0f;
        if (isDying) {
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        } else if (currentState == State.TELEPORTING) {
            Animation tp = isPhase2 ? teleport2Anim : teleport1Anim;
            int f = tp.getFrameIndex();
            int t = tp.getTotalFrames();
            if (f < t / 2) alpha = 1.0f - (float) f / (t / 2f);
            else alpha = (float) (f - t / 2f) / (t / 2f);
        }

        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha))));
        
        // Reverse direction logic
        if (facingRight) {
            g2d.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null);
        } else {
            g2d.drawImage(img, drawX, drawY, drawW, drawH, null);
        }

        if (!isDying && now < hitFlashEndTime && currentState != State.TELEPORTING) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f * alpha));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(drawX, drawY, drawW, drawH);
        }
        g2d.dispose();
    }

    @Override
    public String getName() {
        return "SWAMP PRIEST";
    }

    @Override
    public boolean shouldRemove() {
        return isDying && (GamePanel.getTickTime() - deathFadeStartTime >= deathFadeDuration);
    }
}
