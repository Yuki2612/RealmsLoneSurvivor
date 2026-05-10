package gameproject.entity;

import gameproject.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ShadowBoss extends Enemy {
    public enum State {
        MOVING, DASHING, ATTACKING, NIGHTMARE, DYING
    }

    private State currentState = State.MOVING;

    private Animation moveAnim;
    private Animation dashAnim;
    private Animation attackAnim;
    private Animation deathAnim;

    private int actionTimer = 60;
    private float baseSpeed = 1.8f;
    private int attackFrameCounter = 0;
    private int nightmareCounter = 0;
    private long lastNightmareActionTime = 0;
    private boolean isHidden = false;
    private boolean isTeleporting = false;

    private String bossKey = "boss2";
    private float targetX, targetY;
    private boolean facingRight = true;
    private boolean nightmareTriggered = false;

    public ShadowBoss(float startX, float startY, int surviveTimeSeconds) {
        // Tăng hitbox lên 55 để khớp với sprite hơn
        super(startX, startY, 55, (int) ((900 + (surviveTimeSeconds * 8)) * 1.4), 1.8f, new Color(100, 0, 150));
        this.isBoss = true;
        this.deathFadeDuration = 1500;

        initAnimations(bossKey);
    }

    public String getName() {
        return "THE SHADOW";
    }

    private void initAnimations(String key) {
        ImageManager.loadAnimation(key + "_move", "app/res/" + key + "_move.png");
        ImageManager.loadAnimation(key + "_dash", "app/res/" + key + "_dash.png");
        ImageManager.loadAnimation(key + "_attack", "app/res/" + key + "_attack.png");
        ImageManager.loadAnimation(key + "_death", "app/res/" + key + "_death.png");

        moveAnim = new Animation(7);
        moveAnim.setFrames(ImageManager.getAnimation(key + "_move"));

        dashAnim = new Animation(5);
        dashAnim.setFrames(ImageManager.getAnimation(key + "_dash"));

        attackAnim = new Animation(6);
        attackAnim.setFrames(ImageManager.getAnimation(key + "_attack"));

        deathAnim = new Animation(6);
        deathAnim.setLooping(false);
        deathAnim.setFrames(ImageManager.getAnimation(key + "_death"));
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies, int screenW,
            int screenH, gameproject.GamePanel panel) {

        long currentTime = GamePanel.getTickTime();

        if (isDying) {
            if (currentState != State.DYING) {
                currentState = State.DYING;
                if (deathAnim != null)
                    deathAnim.reset();
                panel.vfxManager.isNightmareActive = false;
            }
            if (deathAnim != null)
                deathAnim.update();
            return;
        }

        // Kích hoạt Nightmare Domain khi dưới 50% máu
        if (!nightmareTriggered && hp < maxHp / 2) {
            nightmareTriggered = true;
            currentState = State.NIGHTMARE;
            nightmareCounter = 0;
            lastNightmareActionTime = currentTime;
            isHidden = true; // Chỉ tàng hình lúc bắt đầu vào phase
            panel.vfxManager.isNightmareActive = true;
            panel.vfxManager.nightmareAlpha = 1.0f;
            panel.vfxManager.showWaveBanner("THE SHADOW ENVELOPS THE WORLD!", new Color(150, 0, 255), currentTime);
        }

        float dx = playerX - x;
        float dy = playerY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dx > 5)
            facingRight = true;
        else if (dx < -5)
            facingRight = false;

        switch (currentState) {
            case MOVING:
                speed = baseSpeed;
                if (moveAnim != null)
                    moveAnim.update();

                if (actionTimer > 0)
                    actionTimer--;

                // Nếu đang trong chuỗi nightmare, sau khi tấn công xong và chờ một chút thì
                // dịch chuyển tiếp
                if (nightmareTriggered && nightmareCounter > 0 && nightmareCounter < 5) {
                    if (actionTimer <= 0) {
                        isHidden = true;
                        currentState = State.NIGHTMARE;
                        lastNightmareActionTime = currentTime;
                    }
                } else {
                    // Kiểm tra vật cản xung quanh boss (75px)
                    boolean hasObstacleNearby = !panel.mapManager.getObstaclesInRadius(x + size / 2f, y + size / 2f, 75)
                            .isEmpty();

                    if (actionTimer <= 0 && dist > 400 && !hasObstacleNearby) {
                        currentState = State.DASHING;
                        if (dashAnim != null)
                            dashAnim.reset();
                        float angle = (float) Math.atan2(dy, dx);
                        targetX = playerX - (float) Math.cos(angle) * 120;
                        targetY = playerY - (float) Math.sin(angle) * 120;
                        actionTimer = 35;
                    } else if (actionTimer <= 0 && dist < 280 && !hasObstacleNearby) {
                        currentState = State.ATTACKING;
                        if (attackAnim != null)
                            attackAnim.reset();
                        attackFrameCounter = 0;
                    }
                }
                break;

            case DASHING:
                float tdx = targetX - x;
                float tdy = targetY - y;
                float tdist = (float) Math.sqrt(tdx * tdx + tdy * tdy);

                if (tdist > 10) {
                    x += (tdx / tdist) * baseSpeed * 10.0f;
                    y += (tdy / tdist) * baseSpeed * 10.0f;
                    EnemyController.resolveHybridCollision(this, panel.mapManager);
                }

                if (dashAnim != null)
                    dashAnim.update();

                if (actionTimer > 0)
                    actionTimer--;

                if ((actionTimer <= 0 || tdist <= 10) && (dashAnim == null || dashAnim.hasFinishedCycle())) {
                    currentState = State.MOVING;
                    actionTimer = 40;
                }
                break;

            case ATTACKING:
                if (attackAnim != null) {
                    attackAnim.update();
                    int idx = attackAnim.getFrameIndex();

                    if (idx < 2) {
                        speed = 0;
                    } else {
                        float lungeMulti = (nightmareTriggered && nightmareCounter > 0) ? 5.75f : 5f;
                        float angle = (float) Math.atan2(playerY - y, playerX - x);
                        x += (float) Math.cos(angle) * baseSpeed * lungeMulti;
                        y += (float) Math.sin(angle) * baseSpeed * lungeMulti;
                        EnemyController.resolveHybridCollision(this, panel.mapManager);

                    }

                    if (idx >= 3 && attackFrameCounter == 0) {
                        checkHit(panel, 75); // Tầm chém rộng hơn tí
                        attackFrameCounter = 1;
                        panel.vfxManager.triggerScreenShake(8);
                    }

                    if (attackAnim.hasFinishedCycle()) {
                        currentState = State.MOVING;
                        // Sau khi tấn công xong, để boss hiện hình và di chuyển trong 1 giây để người
                        // chơi phản công
                        actionTimer = (nightmareTriggered && nightmareCounter > 0) ? 30 : 60;

                        // Nếu đã đủ 5 lần tấn công, kết thúc bóng tối
                        if (nightmareCounter >= 5) {
                            panel.vfxManager.isNightmareActive = false;
                            nightmareCounter = 0; // Reset
                        }
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case NIGHTMARE:
                if (nightmareCounter < 5) {
                    if (isHidden && currentTime - lastNightmareActionTime > 1000) {
                        double angle = Math.random() * Math.PI * 2;
                        float offset = 220f;
                        this.x = playerX + (float) Math.cos(angle) * offset;
                        this.y = playerY + (float) Math.sin(angle) * offset;

                        isHidden = false;
                        isTeleporting = true;
                        lastNightmareActionTime = currentTime;

                        panel.vfxManager.spawnComboSparkles(x, y, currentTime, new Color(150, 0, 255), 3);
                        SoundManager.play("shield");
                    } else if (isTeleporting && currentTime - lastNightmareActionTime > 400) {
                        isTeleporting = false;
                        nightmareCounter++;
                        currentState = State.ATTACKING;
                        if (attackAnim != null)
                            attackAnim.reset();
                        attackFrameCounter = 0;
                    }
                } else {
                    double retreatAngle = Math.random() * Math.PI * 2;
                    this.x = playerX + (float) Math.cos(retreatAngle) * 450;
                    this.y = playerY + (float) Math.sin(retreatAngle) * 450;

                    isHidden = false;
                    isTeleporting = false;
                    currentState = State.MOVING;
                    actionTimer = 150;
                    nightmareCounter = 0;
                    panel.vfxManager.isNightmareActive = false;
                }
                break;

            case DYING:
                panel.vfxManager.isNightmareActive = false;
                break;
        }

        if (currentState == State.MOVING && !isHidden) {
            EnemyController.moveEnemy(this, panel, speedMultiplier);
        }
    }

    private void checkHit(GamePanel panel, float range) {
        float playerDist = (float) Math
                .sqrt(Math.pow(panel.player.getX() - x, 2) + Math.pow(panel.player.getY() - y, 2));
        if (playerDist < range) {
            if (panel.player.takeHit()) {
                panel.triggerGameOver();
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        if (isHidden)
            return;

        BufferedImage img = null;
        if (isDying) {
            img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
        } else {
            switch (currentState) {
                case MOVING:
                    img = (moveAnim != null) ? moveAnim.getCurrentFrame() : null;
                    break;
                case DASHING:
                    img = (dashAnim != null) ? dashAnim.getCurrentFrame() : null;
                    break;
                case ATTACKING:
                    img = (attackAnim != null) ? attackAnim.getCurrentFrame() : null;
                    break;
                case NIGHTMARE:
                    img = (moveAnim != null) ? moveAnim.getCurrentFrame() : null;
                    break;
            }
        }

        if (img != null) {
            drawShadow(g, img);
        }
    }

    private void drawShadow(Graphics g, BufferedImage img) {
        Graphics2D g2d = (Graphics2D) g.create();
        long now = GamePanel.getTickTime();

        float alpha = 1.0f;
        if (isDying) {
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        } else if (nightmareTriggered && nightmareCounter > 0) {
            alpha = 0.85f; // Hiện rõ hơn tí
        }
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, alpha)));

        float scale = 1.3f;
        int drawW = (int) (img.getWidth() * scale);
        int drawH = (int) (img.getHeight() * scale);
        int drawX = (int) x - drawW / 2;
        int drawY = (int) y - drawH / 2;

        if (!facingRight) {
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

        if (GamePanel.showHitboxes) {
            g.setColor(Color.RED);
            Rectangle rect = getBounds();
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
            g.setColor(new Color(255, 0, 0, 100));
            g.drawOval((int) x - 85, (int) y - 85, 170, 170);
        }
    }
}
