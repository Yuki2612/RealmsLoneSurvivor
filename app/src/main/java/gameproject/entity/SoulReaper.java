package gameproject.entity;

import gameproject.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class SoulReaper extends Enemy {
    public enum State {
        MOVING, DASHING, ATTACKING, DYING
    }

    private State currentState = State.MOVING;

    private Animation idleAnim;
    private Animation dashAnim;
    private Animation attackAnim;
    private Animation deathAnim;

    private int actionTimer = 60;
    private float baseSpeed = 1.2f; // Giảm tốc độ di chuyển cơ bản
    private int attackFrameCounter = 0;

    private String bossKey = "boss1";
    private float targetX, targetY;
    private boolean facingRight = true; // Hướng mặt của Boss

    public SoulReaper(float startX, float startY, int surviveTimeSeconds) {
        super(startX, startY, 90, (int) ((700 + (surviveTimeSeconds * 8)) * 1.2), 2.2f, Color.RED);
        this.isBoss = true;
        this.deathFadeDuration = 1500;

        initAnimations(bossKey);
    }

    public String getName() {
        return "SOUL REAPER";
    }

    private void initAnimations(String key) {
        // Đảm bảo các hoạt ảnh được nạp vào ImageManager trước khi lấy ra
        ImageManager.loadAnimation(key + "_idle", "app/res/" + key + "_idle.png");
        ImageManager.loadAnimation(key + "_dash", "app/res/" + key + "_dash.png");
        ImageManager.loadAnimation(key + "_attack", "app/res/" + key + "_attack.png");
        ImageManager.loadAnimation(key + "_death", "app/res/" + key + "_death.png");

        idleAnim = new Animation(7);
        idleAnim.setFrames(ImageManager.getAnimation(key + "_idle"));

        dashAnim = new Animation(4);
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
            }
            if (deathAnim != null)
                deathAnim.update();
            return;
        }

        float dx = playerX - x;
        float dy = playerY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Cập nhật hướng mặt dựa trên vị trí người chơi
        if (dx > 5)
            facingRight = true;
        else if (dx < -5)
            facingRight = false;

        switch (currentState) {
            case MOVING:
                speed = baseSpeed;
                if (idleAnim != null)
                    idleAnim.update();

                // KHẮC PHỤC 2: Đảm bảo timer không tụt xuống âm
                if (actionTimer > 0)
                    actionTimer--;

                // 1. Kiểm tra vật cản xung quanh boss (75px)
                boolean hasObstacleNearby = !panel.mapManager.getObstaclesInRadius(x + size / 2f, y + size / 2f, 75)
                        .isEmpty();

                // 1. Dash áp sát (Charge) nếu ở quá xa
                if (actionTimer <= 0 && dist > 450 && !hasObstacleNearby) {
                    currentState = State.DASHING;
                    if (dashAnim != null)
                        dashAnim.reset(); // Bắt đầu lướt từ frame 0
                    float angle = (float) Math.atan2(dy, dx);
                    targetX = playerX - (float) Math.cos(angle) * 150;
                    targetY = playerY - (float) Math.sin(angle) * 150;
                    actionTimer = 40;
                }
                // 2. Tấn công nếu ở gần
                else if (actionTimer <= 0 && dist < 320 && !hasObstacleNearby) {
                    currentState = State.ATTACKING;
                    if (attackAnim != null)
                        attackAnim.reset(); // Reset để chém từ đầu
                    attackFrameCounter = 0;
                }
                break;

            case DASHING:
                // Cơ chế Charge giống boss cũ
                float tdx = targetX - x;
                float tdy = targetY - y;
                float tdist = (float) Math.sqrt(tdx * tdx + tdy * tdy);

                if (tdist > 10) {
                    x += (tdx / tdist) * baseSpeed * 8.5f;
                    y += (tdy / tdist) * baseSpeed * 8.5f;
                    EnemyController.resolveHybridCollision(this, panel.mapManager);
                }

                if (dashAnim != null)
                    dashAnim.update();
                if (currentTime % 2 == 0) { // Tăng mật độ bóng mờ để mượt hơn
                    int bw = (int) (64 * 2.8f);
                    int bh = (int) (64 * 2.8f);
                    java.awt.image.BufferedImage currentFrame = (dashAnim != null) ? dashAnim.getCurrentFrame() : null;
                    if (currentFrame != null) {
                        panel.vfxManager.addDashAfterimage(x - bw / 2 + size / 2, y - bh + size, bw, bh, currentTime,
                                currentFrame, facingRight);
                    }
                }

                if (actionTimer > 0)
                    actionTimer--;

                // Chờ lướt xong VÀ animation chạy xong (hoặc gần xong)
                if ((actionTimer <= 0 || tdist <= 10) && (dashAnim == null || dashAnim.hasFinishedCycle())) {
                    currentState = State.MOVING;
                    if (idleAnim != null)
                        idleAnim.reset();
                    actionTimer = 60; // Hồi sức sau khi lướt
                }
                break;

            case ATTACKING:
                if (attackAnim != null) {
                    attackAnim.update();
                    int idx = attackAnim.getFrameIndex();

                    // GIIAI ĐOẠN 1: Đứng yên chuẩn bị (Wind-up) - Khung hình 0-3
                    if (idx < 3) {
                        speed = 0; // Đứng yên để người chơi nhận biết và né
                    }
                    // GIAI ĐOẠN 2: Lao vào chém - Khung hình 3-5
                    else if (idx <= 5) {
                        float angle = (float) Math.atan2(playerY - y, playerX - x);
                        x += (float) Math.cos(angle) * baseSpeed * 11.0f;
                        y += (float) Math.sin(angle) * baseSpeed * 11.0f;
                        EnemyController.resolveHybridCollision(this, panel.mapManager);

                        if (currentTime % 2 == 0) {
                            int bw = (int) (64 * 2.8f);
                            int bh = (int) (64 * 2.8f);
                            java.awt.image.BufferedImage currentFrame = (attackAnim != null)
                                    ? attackAnim.getCurrentFrame()
                                    : null;
                            if (currentFrame != null) {
                                panel.vfxManager.addDashAfterimage(x - bw / 2 + size / 2, y - bh + size, bw, bh,
                                        currentTime, currentFrame, facingRight);
                            }
                        }
                    }

                    // Gây sát thương tại khung hình 4-5
                    if ((idx == 4 || idx == 5) && attackFrameCounter == 0) {
                        checkScytheHit(panel);
                        attackFrameCounter = 1;
                        panel.vfxManager.triggerScreenShake(12);
                    }

                    // KHẮC PHỤC 1: Đợi animation chạy xong 100% vòng đời khung hình cuối
                    if (attackAnim.hasFinishedCycle()) {
                        currentState = State.MOVING;
                        if (idleAnim != null)
                            idleAnim.reset();
                        actionTimer = 100 + (int) (Math.random() * 50);
                    }
                } else {
                    currentState = State.MOVING;
                }
                break;

            case DYING:
                break;
        }

        // Bám đuổi liên tục khi đang ở trạng thái MOVING
        if (currentState == State.MOVING) {
            EnemyController.moveEnemy(this, panel, speedMultiplier);
        }
    }

    public void checkScytheHit(GamePanel panel) {
        float playerDist = (float) Math
                .sqrt(Math.pow(panel.player.getX() - x, 2) + Math.pow(panel.player.getY() - y, 2));
        if (playerDist < 120) { // Tầm chém của lưỡi hái
            if (panel.player.takeHit()) {
                panel.triggerGameOver();
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        BufferedImage img = null;

        // ƯU TIÊN TUYỆT ĐỐI: Nếu đang chết thì bắt buộc dùng deathAnim
        if (isDying) {
            img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
        } else {
            switch (currentState) {
                case MOVING:
                    img = (idleAnim != null) ? idleAnim.getCurrentFrame() : null;
                    break;
                case DASHING:
                    img = (dashAnim != null) ? dashAnim.getCurrentFrame() : null;
                    break;
                case ATTACKING:
                    img = (attackAnim != null) ? attackAnim.getCurrentFrame() : null;
                    break;
                case DYING:
                    img = (deathAnim != null) ? deathAnim.getCurrentFrame() : null;
                    break;
            }
        }

        if (img != null) {
            drawSoulReaper(g, img);
        }
    }

    private void drawSoulReaper(Graphics g, BufferedImage img) {
        Graphics2D g2d = (Graphics2D) g.create();
        long now = GamePanel.getTickTime();

        float alpha = 1.0f;
        if (isDying) {
            // Đảm bảo alpha giảm dần trong 1.5 giây
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        }
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, alpha)));

        // GIẢM SCALE: Chỉnh về 2.8f để cân đối hơn với nhân vật
        float scale = 2.8f;
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);

        // CĂN GIỮA TUYỆT ĐỐI
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

        // HIỂN THỊ HITBOX (Dành cho chế độ Debug)
        if (GamePanel.showHitboxes) {
            g.setColor(Color.RED);
            Rectangle rect = getBounds(); // Đã đổi từ physics sang bounds gây sát thương
            g.drawRect(rect.x, rect.y, rect.width, rect.height);

            // Tầm chém của Boss
            g.setColor(new Color(255, 0, 0, 100));
            g.drawOval((int) x - 120, (int) y - 120, 240, 240);
        }
    }

    @Override
    public Rectangle getBounds() {
        // TĂNG HITBOX & CĂN GIỮA TUYỆT ĐỐI cho Soul Reaper
        // Boss này được vẽ tâm tại (x, y), nên hitbox phải bao quanh tâm đó
        int hbW = (int) (size * 1.6f);
        int hbH = (int) (size * 1.9f);
        return new Rectangle((int) x - hbW / 2, (int) y - hbH / 2, hbW, hbH);
    }

    @Override
    public boolean shouldRemove() {
        return isDying && (GamePanel.getTickTime() - deathFadeStartTime >= deathFadeDuration);
    }
}
