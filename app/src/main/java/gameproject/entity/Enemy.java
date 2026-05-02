package gameproject.entity;

import gameproject.*;
import gameproject.weapon.Projectile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

public abstract class Enemy implements gameproject.Renderable {
    protected float x, y;
    protected float speed;
    protected int size;

    @Override
    public void render(Graphics2D g) {
        draw(g);
    }

    @Override
    public float getBottomY() {
        return y + size; // Chân của quái
    }

    protected int hp, maxHp;
    protected Color color;

    // THÊM: Các biến vật lý quán tính
    public float velX = 0;
    public float velY = 0;
    public float kbX = 0, kbY = 0;

    public boolean isBoss = false;

    public long burnEndTime = 0;
    public long chillEndTime = 0;
    public long poisonEndTime = 0;
    public long shockEndTime = 0;
    public long freezeEndTime = 0;
    public long plasmaEndTime = 0;
    public boolean inAcidZone = false;

    // Cache chỉ số sát thương của player – được EntityManager cập nhật mỗi frame
    // Dùng để phản ứng nguyên tố scale cùng progression của player
    public int playerDamageCache = 10;

    private long lastBurnTick = 0;
    private long lastPlasmaTick = 0;
    private long lastPoisonTick = 0;
    public boolean triggerCorrosiveMelt = false;

    public long thermalShockCooldown = 0;
    public long plasmaCooldown = 0;

    // ── Phase-1 VFX ──────────────────────────────────────────────
    private long hitFlashEndTime = 0; // White flash khi nhận damage
    private long deathFadeStartTime = -1; // -1 = chưa bắt đầu chết
    public long deathFadeDuration = 300;
    public boolean isDying = false; // đang trong animation chết

    public Enemy(float x, float y, int size, int maxHp, float speed, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.speed = speed;
        this.color = color;
    }

    // THÊM: Forgiving Hitbox SIÊU MỎNG để lách qua khe hẹp
    public Rectangle getPhysicsHitbox() {
        // Thu hẹp chiều ngang đáng kể (40% size) và hạ thấp xuống chân
        int w = (int) (this.size * 0.4);
        int h = (int) (this.size * 0.4);
        int offsetX = (this.size - w) / 2;
        int offsetY = this.size - h;
        return new Rectangle((int) this.x + offsetX, (int) this.y + offsetY, w, h);
    }

    public abstract void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies,
            int screenW, int screenH, GamePanel panel);

    public abstract void draw(Graphics g);

    public void updateStatusEffects(long currentTime, VFXManager vfxManager) {
        if (burnEndTime > currentTime && currentTime - lastBurnTick >= 500) {
            // Burn DoT: 33% playerDamage/tick
            takeDamage(Math.max(3, playerDamageCache / 3), vfxManager, currentTime);
            lastBurnTick = currentTime;
        }
        if (plasmaEndTime > currentTime && currentTime - lastPlasmaTick >= 500) {
            // Plasma DoT: 50% playerDamage/tick
            takeDamage(Math.max(5, playerDamageCache / 2), vfxManager, currentTime);
            lastPlasmaTick = currentTime;
        }
        if (poisonEndTime > currentTime && currentTime - lastPoisonTick >= 500) {
            // Poison DoT: 20% playerDamage/tick
            takeDamage(Math.max(2, playerDamageCache / 5), vfxManager, currentTime);
            lastPoisonTick = currentTime;
        }
    }

    public void applyBurn(long duration, VFXManager vfxManager) {
        long now = gameproject.GamePanel.getTickTime();
        if (chillEndTime > now && now > thermalShockCooldown) {
            chillEndTime = 0;
            burnEndTime = 0;
            thermalShockCooldown = now + 3000;
            // Thermal Shock: 3x playerDamage
            takeDamage(playerDamageCache * 3, vfxManager, now);
            freezeEndTime = now + 1500;
        } else {
            burnEndTime = Math.max(burnEndTime, now + duration);
        }
    }

    public void applyChill(long duration, VFXManager vfxManager) {
        long now = gameproject.GamePanel.getTickTime();
        if (burnEndTime > now && now > thermalShockCooldown) {
            chillEndTime = 0;
            burnEndTime = 0;
            thermalShockCooldown = now + 3000;
            // Thermal Shock: 3x playerDamage
            takeDamage(playerDamageCache * 3, vfxManager, now);
            freezeEndTime = now + 1500;
        } else {
            chillEndTime = Math.max(chillEndTime, now + duration);
        }
    }

    public void applyPoison(long duration) {
        poisonEndTime = Math.max(poisonEndTime, gameproject.GamePanel.getTickTime() + duration);
    }

    public void applyShock(long duration, VFXManager vfxManager, ArrayList<Enemy> enemies) {
        long now = gameproject.GamePanel.getTickTime();
        if (poisonEndTime > now && now > plasmaCooldown) {
            plasmaCooldown = now + 4000;
            plasmaEndTime = now + 2000;
            int count = 0;
            for (Enemy e : enemies) {
                if (e != this && !e.isDead() && count < 5) {
                    float dist = (float) Math.sqrt(Math.pow(e.x - x, 2) + Math.pow(e.y - y, 2));
                    if (dist < 150) {
                        e.applyPoison(2000);
                        e.plasmaEndTime = gameproject.GamePanel.getTickTime() + 2000;
                        count++;
                    }
                }
            }
        } else {
            shockEndTime = Math.max(shockEndTime, gameproject.GamePanel.getTickTime() + duration);
        }
    }

    // ĐÂY LÀ HÀM BẠN ĐÃ LÀM THIẾU
    public java.util.List<gameproject.weapon.Projectile> shoot() {
        return null;
    }

    public java.util.List<Enemy> summon() {
        return null;
    }

    public void applyKnockback(float sourceX, float sourceY, float pushForce) {
        float dx = this.x - sourceX;
        float dy = this.y - sourceY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            this.kbX = (dx / dist) * (isBoss ? pushForce * 0.2f : pushForce);
            this.kbY = (dy / dist) * (isBoss ? pushForce * 0.2f : pushForce);
        }
    }

    public void takeDamage(int damage, boolean isCrit, VFXManager vfxManager, long currentTime) {
        // 1. Áp dụng hệ số Chí mạng (x1.5)
        if (isCrit) {
            damage = (int) (damage * 1.5f);
        }

        // 2. Áp dụng hệ số Độc (x1.3) - Có thể cộng dồn với Chí mạng
        if (poisonEndTime > currentTime) {
            damage = (int) (damage * 1.3f); // Stack: (Base * 1.5) * 1.3
        }

        if (isCrit) {
            // Hiển thị màu đặc biệt khi Crit + Poison (Vàng chanh)
            Color critColor = (poisonEndTime > currentTime) ? new Color(180, 255, 0) : new Color(255, 220, 0);
            if (vfxManager != null) {
                vfxManager.addCritDamageText(this.x + 15, this.y - 10, damage, currentTime, critColor);
            }
            takeDamageBase(damage, null, currentTime, critColor);
        } else {
            // Priority: Burn (Orange) > Poison (Green) > Normal (White)
            Color textColor = Color.WHITE;
            if (burnEndTime > currentTime)
                textColor = new Color(255, 100, 0); // Orange-Red for Fire
            else if (poisonEndTime > currentTime)
                textColor = Color.GREEN;

            takeDamageBase(damage, vfxManager, currentTime, textColor);
        }
    }

    public void takeDamage(int damage, VFXManager vfxManager, long currentTime) {
        takeDamage(damage, false, vfxManager, currentTime);
    }

    public void takeDamageBase(int damage, VFXManager vfxManager, long currentTime, Color textColor) {
        this.hp -= damage;
        hitFlashEndTime = currentTime + 80; // Hit flash 80ms
        if (vfxManager != null) {
            vfxManager.addDamageText(this.x + 15, this.y, damage, currentTime, textColor);
        }
        if (hp <= 0) {
            // Kích hoạt death fade animation ngay khi dưới 0 HP lần đầu
            if (!isDying) {
                isDying = true;
                deathFadeStartTime = currentTime;
            }
            if (poisonEndTime > currentTime && burnEndTime > currentTime) {
                triggerCorrosiveMelt = true;
            }
        }
    }

    /** Kiểm tra quái đã cạn kiệt HP (dùng bởi passive skills, va chạm, etc.) */
    public boolean isDead() {
        return this.hp <= 0;
    }

    /** Kiểm tra đã xong animation fade và có thể xóa khỏi danh sách */
    public boolean shouldRemove() {
        if (hp <= 0 && !isDying) {
            isDying = true;
            deathFadeStartTime = gameproject.GamePanel.getTickTime();
        }
        return isDying && (gameproject.GamePanel.getTickTime() - deathFadeStartTime >= deathFadeDuration);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, size, size);
    }

    protected void drawSprite(Graphics g, String imageKey) {
        long now = gameproject.GamePanel.getTickTime();
        java.awt.image.BufferedImage img = ImageManager.get(imageKey);
        Graphics2D g2d = (Graphics2D) g;

        // Alpha: fade out khi đang chết
        float alpha = 1.0f;
        if (isDying && deathFadeStartTime >= 0) {
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        }
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));

        if (img != null) {
            // QUY CHUẨN PIXEL-PERFECT: round(World) phối hợp với Snapped Translation
            int drawX = (int) Math.round(x) - 10;
            int drawY = (int) Math.round(y) - 20;
            int drawW = size + 20;
            int drawH = size + 20;

            // 1. Hiệu ứng Blood Moon Aura (Vòng tròn đỏ dưới chân)
            if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON &&
                    gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE
                    && !isBoss) {
                g2d.setColor(new Color(255, 0, 0, (int) (70 * alpha)));
                g2d.fillOval((int) x - 5, (int) y + size - 10, size + 10, 15);
            }

            g2d.drawImage(img, drawX, drawY, drawW, drawH, null);

            // Hit flash: vẽ lớp trắng bán trong suốt lên trên sprite
            if (now < hitFlashEndTime) {
                float flashAlpha = 0.65f * (float) (hitFlashEndTime - now) / 80f;
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                        Math.max(0, Math.min(0.65f, flashAlpha))));
                g2d.setColor(Color.WHITE);
                g2d.fillRect(drawX, drawY, drawW, drawH);
            }
        } else {
            g2d.setColor(color);
            g2d.fillRect((int) x, (int) y, size, size);
        }

        // Reset composite
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f));

        // HP bar (chỉ hiện khi còn sống)
        if (!isDying) {
            g.setColor(Color.RED);
            g.fillRect((int) x, (int) y + size, size, 4);
            g.setColor(Color.GREEN);
            int hpWidth = (int) ((float) hp / maxHp * size);
            g.fillRect((int) x, (int) y + size, hpWidth, 4);
        }

        // Draw Hitbox for debugging
        if (gameproject.GamePanel.showHitboxes && !isDying) {
            g.setColor(Color.RED);
            Rectangle b = getPhysicsHitbox();
            g.drawRect(b.x, b.y, b.width, b.height);
        }
    }

    public int getHp() {
        return hp;
    }
}