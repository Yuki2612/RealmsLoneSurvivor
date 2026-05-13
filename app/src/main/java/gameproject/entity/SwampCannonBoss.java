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

public class SwampCannonBoss extends Enemy {
    public enum State {
        MOVING, DASHING, ATTACKING_1, ATTACKING_2, ATTACKING_3, DYING
    }

    private State currentState = State.MOVING;
    private String bossKey = "boss7";
    private boolean facingRight = true;
    private Random rand = new Random();

    // Animations
    private Animation moveAnim;
    private Animation dashAnim;
    private Animation deathAnim;
    private Animation attack1Anim;
    private Animation attack2Anim;
    private Animation attack3Anim;

    // Timers & Cooldowns
    private int actionTimer = 60;
    private long lastSkillTime = 0;
    private long globalSkillCooldown = 1500;
    
    private long lastAttack1Time = 0;
    private long attack1Cooldown = 4000;
    
    private long lastAttack2Time = 0;
    private long attack2Cooldown = 6000;
    
    private long lastAttack3Time = 0;
    private long attack3Cooldown = 5000;

    private List<Projectile> pendingProjectiles = new ArrayList<>();
    private float targetX, targetY;
    private boolean effectTriggered = false;
    private int spawnedMarkers = 0;
    private long lastMarkerTime = 0;
    private int attackCycle = 0;

    // Artillery markers
    private static class ArtilleryMarker {
        float x, y;
        long startTime;
        long triggerTime;
        public ArtilleryMarker(float x, float y, long startTime, long triggerTime) {
            this.x = x; this.y = y; this.startTime = startTime; this.triggerTime = triggerTime;
        }
    }
    private List<ArtilleryMarker> markers = new ArrayList<>();

    public SwampCannonBoss(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 100, (int) ((1000 + (surviveTimeSeconds * 8)) * 1.5f), 1.5f, new Color(46, 139, 87));
        this.isBoss = true;
        this.deathFadeDuration = 2000;
        initAnimations();
    }

    private void initAnimations() {
        ImageManager.loadAnimation(bossKey + "_move", "app/res/" + bossKey + "_move_f4.png", 4);
        ImageManager.loadAnimation(bossKey + "_dash", "app/res/" + bossKey + "_dash_f2.png", 2);
        ImageManager.loadAnimation(bossKey + "_death", "app/res/" + bossKey + "_death_f4.png", 4);
        ImageManager.loadAnimation(bossKey + "_attack1", "app/res/" + bossKey + "_attack1_f6.png", 6);
        ImageManager.loadAnimation(bossKey + "_attack2", "app/res/" + bossKey + "_attack2_f4.png", 4);
        ImageManager.loadAnimation(bossKey + "_attack3", "app/res/" + bossKey + "_attack3_f4.png", 4);

        moveAnim = new Animation(8);
        moveAnim.setFrames(ImageManager.getAnimation(bossKey + "_move"));

        dashAnim = new Animation(6);
        dashAnim.setFrames(ImageManager.getAnimation(bossKey + "_dash"));
        dashAnim.setLooping(false);

        deathAnim = new Animation(10);
        deathAnim.setFrames(ImageManager.getAnimation(bossKey + "_death"));
        deathAnim.setLooping(false);

        attack1Anim = new Animation(6);
        attack1Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack1"));
        attack1Anim.setLooping(false);

        attack2Anim = new Animation(8);
        attack2Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack2"));
        attack2Anim.setLooping(false);

        attack3Anim = new Animation(7);
        attack3Anim.setFrames(ImageManager.getAnimation(bossKey + "_attack3"));
        attack3Anim.setLooping(false);
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, GamePanel panel) {

        long currentTime = GamePanel.getTickTime();

        if (isDying) {
            if (currentState != State.DYING) {
                currentState = State.DYING;
                deathAnim.reset();
                deathFadeStartTime = currentTime;
            }
            deathAnim.update();
            return;
        }

        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float dx = playerX - centerX;
        float dy = playerY - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dx > 5) facingRight = true;
        else if (dx < -5) facingRight = false;

        // Process markers
        var it = markers.iterator();
        while (it.hasNext()) {
            ArtilleryMarker m = it.next();
            if (currentTime >= m.triggerTime) {
                panel.vfxManager.addExplosion(m.x, m.y, 160, currentTime);
                SoundManager.play("explosion");
                panel.vfxManager.triggerScreenShake(10);
                // Damage player if inside
                float pdx = playerX - m.x;
                float pdy = playerY - m.y;
                if (Math.sqrt(pdx*pdx + pdy*pdy) < 80) {
                    panel.player.takeHit();
                }
                it.remove();
            }
        }

        switch (currentState) {
            case MOVING:
                moveAnim.update();
                EnemyController.moveEnemy(this, panel, speedMultiplier);

                if (actionTimer > 0) actionTimer--;

                if (actionTimer <= 0 && currentTime - lastSkillTime > globalSkillCooldown) {
                    if (dist > 600) {
                        currentState = State.DASHING;
                        dashAnim.reset();
                        float angle = (float) Math.atan2(dy, dx);
                        targetX = playerX - (float) Math.cos(angle) * 150;
                        targetY = playerY - (float) Math.sin(angle) * 150;
                        effectTriggered = false;
                        lastSkillTime = currentTime;
                    } 
                    else if (dist < 200 && currentTime - lastAttack1Time > attack1Cooldown) {
                        currentState = State.ATTACKING_1;
                        attack1Anim.reset();
                        effectTriggered = false;
                        targetX = playerX;
                        targetY = playerY;
                        lastAttack1Time = currentTime;
                        lastSkillTime = currentTime;
                    }
                    else if (currentTime - lastAttack2Time > attack2Cooldown) {
                        currentState = State.ATTACKING_2;
                        attack2Anim.reset();
                        effectTriggered = false;
                        lastAttack2Time = currentTime;
                        lastSkillTime = currentTime;
                    }
                    else if (dist < 500 && currentTime - lastAttack3Time > attack3Cooldown) {
                        currentState = State.ATTACKING_3;
                        attack3Anim.reset();
                        effectTriggered = false;
                        lastAttack3Time = currentTime;
                        lastSkillTime = currentTime;
                    }
                }
                break;

            case DASHING:
                dashAnim.update();
                float ddx = targetX - centerX;
                float ddy = targetY - centerY;
                float ddist = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                if (ddist > 20) {
                    float dashSpeed = 15.0f;
                    x += (ddx / ddist) * dashSpeed;
                    y += (ddy / ddist) * dashSpeed;
                    EnemyController.resolveHybridCollision(this, panel.mapManager);
                }
                if (dashAnim.hasFinishedCycle()) {
                    currentState = State.MOVING;
                    actionTimer = 30;
                }
                break;

            case ATTACKING_1: // Melee Lunge
                attack1Anim.update();
                int idx1 = attack1Anim.getFrameIndex();
                if (idx1 >= 2 && idx1 <= 4) {
                    float ldx = targetX - centerX;
                    float ldy = targetY - centerY;
                    float ldist = (float) Math.sqrt(ldx * ldx + ldy * ldy);
                    if (ldist > 10) {
                        float lungeSpeed = 12.0f;
                        x += (ldx / ldist) * lungeSpeed;
                        y += (ldy / ldist) * lungeSpeed;
                        EnemyController.resolveHybridCollision(this, panel.mapManager);
                    }
                    if (idx1 == 3 && !effectTriggered) {
                        checkMeleeHit(panel, 100);
                        panel.vfxManager.triggerScreenShake(8);
                        effectTriggered = true;
                    }
                }
                if (attack1Anim.hasFinishedCycle()) {
                    currentState = State.MOVING;
                    actionTimer = 40;
                }
                break;

            case ATTACKING_2: // Artillery
                attack2Anim.update();
                // Spawn 5 markers sequentially
                if (spawnedMarkers < 5 && currentTime - lastMarkerTime > 500) {
                    float tx = playerX + rand.nextInt(60) - 30;
                    float ty = playerY + rand.nextInt(60) - 30;
                    markers.add(new ArtilleryMarker(tx, ty, currentTime, currentTime + 700));
                    SoundManager.play("laser");
                    lastMarkerTime = currentTime;
                    spawnedMarkers++;
                }

                if (attack2Anim.hasFinishedCycle() && spawnedMarkers >= 5) {
                    currentState = State.MOVING;
                    actionTimer = 50;
                    spawnedMarkers = 0; // Reset for next time
                }
                break;

            case ATTACKING_3: // Spread Explosive
                attack3Anim.update();
                if (attack3Anim.getFrameIndex() == 2 && !effectTriggered) {
                    fireExplosiveSpread(playerX, playerY, 5, 45);
                    SoundManager.play("laser");
                    effectTriggered = true;
                }
                if (attack3Anim.hasFinishedCycle()) {
                    currentState = State.MOVING;
                    actionTimer = 45;
                }
                break;

            case DYING:
                break;
        }
    }

    private void fireExplosiveSpread(float px, float py, int count, float spreadAngle) {
        float cx = x + size / 2;
        float cy = y + size / 2;
        float baseAngle = (float) Math.toDegrees(Math.atan2(py - cy, px - cx));
        float startAngle = baseAngle - spreadAngle / 2f;
        float step = spreadAngle / (count - 1);

        for (int i = 0; i < count; i++) {
            float angle = (float) Math.toRadians(startAngle + i * step);
            float tx = cx + (float) Math.cos(angle) * 500;
            float ty = cy + (float) Math.sin(angle) * 500;

            Projectile p = new Projectile(cx, cy, tx, ty, 0.4f, 600f);
            p.isEnemyBullet = true;
            p.isExplosive = true;
            p.explosionRadius = 80;
            p.damage = 1;
            p.spriteKey = "projectile";
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
            panel.player.takeHit();
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
    public void draw(Graphics g) {
        BufferedImage img = null;
        long now = GamePanel.getTickTime();
        if (isDying) {
            img = deathAnim.getCurrentFrame();
        } else {
            switch (currentState) {
                case MOVING: img = moveAnim.getCurrentFrame(); break;
                case DASHING: img = dashAnim.getCurrentFrame(); break;
                case ATTACKING_1: img = attack1Anim.getCurrentFrame(); break;
                case ATTACKING_2: img = attack2Anim.getCurrentFrame(); break;
                case ATTACKING_3: img = attack3Anim.getCurrentFrame(); break;
            }
        }

        if (img != null) {
            drawBoss(g, img);
        }

        // Draw markers
        Graphics2D g2d = (Graphics2D) g;
        for (ArtilleryMarker m : markers) {
            float progress = Math.min(1.0f, (float)(now - m.startTime) / (m.triggerTime - m.startTime));
            int currentD = (int)(160 * progress);
            
            // Border
            g2d.setColor(new Color(255, 0, 0, 150));
            g2d.setStroke(new java.awt.BasicStroke(2f));
            g2d.drawOval((int) m.x - 80, (int) m.y - 80, 160, 160);
            
            // Faint expanding fill
            g2d.setColor(new Color(255, 50, 0, 40)); 
            g2d.fillOval((int) m.x - currentD / 2, (int) m.y - currentD / 2, currentD, currentD);
        }
    }

    private void drawBoss(Graphics g, BufferedImage img) {
        Graphics2D g2d = (Graphics2D) g.create();
        long now = GamePanel.getTickTime();
        float scale = 2.8f;
        int drawW = (int) (img.getWidth() * scale);
        int drawH = (int) (img.getHeight() * scale);
        int drawX = (int) x - drawW / 2 + size / 2;
        int drawY = (int) y - drawH + size;

        float alpha = 1.0f;
        if (isDying) {
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        }

        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha))));
        
        if (facingRight) {
            g2d.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null);
        } else {
            g2d.drawImage(img, drawX, drawY, drawW, drawH, null);
        }

        if (!isDying && now < hitFlashEndTime) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f * alpha));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(drawX, drawY, drawW, drawH);
        }
        g2d.dispose();
    }

    @Override
    public String getName() {
        return "CANNON TURTLE";
    }

    @Override
    public boolean shouldRemove() {
        return isDying && (GamePanel.getTickTime() - deathFadeStartTime >= deathFadeDuration);
    }
}
