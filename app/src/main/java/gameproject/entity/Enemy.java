package gameproject.entity;

import gameproject.*;
import gameproject.weapon.Projectile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

public abstract class Enemy {
    protected float x, y;
    protected float speed;
    protected int size;
    protected int hp, maxHp;
    protected Color color;
    protected float kbX = 0, kbY = 0;
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

    public abstract void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies,
            int screenW, int screenH);

    public abstract void draw(Graphics g);

    public void updateStatusEffects(long currentTime, VFXManager vfxManager) {
        if (burnEndTime > currentTime && currentTime - lastBurnTick >= 500) {
            // Burn DoT: 33% playerDamage/tick (~0.66x/s) – hiệu ứng phụ, không quá mạnh
            takeDamageBase(Math.max(3, playerDamageCache / 3), vfxManager, currentTime, Color.ORANGE);
            lastBurnTick = currentTime;
        }
        if (plasmaEndTime > currentTime && currentTime - lastPlasmaTick >= 500) {
            // Plasma DoT: 50% playerDamage/tick (~1x/s) – phản ứng cấp cao, mạnh hơn Burn
            takeDamageBase(Math.max(5, playerDamageCache / 2), vfxManager, currentTime, Color.YELLOW);
            lastPlasmaTick = currentTime;
        }
    }

    public void applyBurn(long duration, VFXManager vfxManager) {
        if (chillEndTime > System.currentTimeMillis() && System.currentTimeMillis() > thermalShockCooldown) {
            chillEndTime = 0;
            burnEndTime = 0;
            thermalShockCooldown = System.currentTimeMillis() + 3000;
            // Thermal Shock: 5x playerDamage – combo mạnh, scale cùng player
            takeDamageBase(playerDamageCache * 3, vfxManager, System.currentTimeMillis(), Color.WHITE);
            freezeEndTime = System.currentTimeMillis() + 1500;
        } else {
            burnEndTime = Math.max(burnEndTime, System.currentTimeMillis() + duration);
        }
    }

    public void applyChill(long duration, VFXManager vfxManager) {
        if (burnEndTime > System.currentTimeMillis() && System.currentTimeMillis() > thermalShockCooldown) {
            chillEndTime = 0;
            burnEndTime = 0;
            thermalShockCooldown = System.currentTimeMillis() + 3000;
            // Thermal Shock: 5x playerDamage – combo mạnh, scale cùng player
            takeDamageBase(playerDamageCache * 3, vfxManager, System.currentTimeMillis(), Color.WHITE);
            freezeEndTime = System.currentTimeMillis() + 1500;
        } else {
            chillEndTime = Math.max(chillEndTime, System.currentTimeMillis() + duration);
        }
    }

    public void applyPoison(long duration) {
        poisonEndTime = Math.max(poisonEndTime, System.currentTimeMillis() + duration);
    }

    public void applyShock(long duration, VFXManager vfxManager, ArrayList<Enemy> enemies) {
        if (poisonEndTime > System.currentTimeMillis() && System.currentTimeMillis() > plasmaCooldown) {
            plasmaCooldown = System.currentTimeMillis() + 4000;
            plasmaEndTime = System.currentTimeMillis() + 2000;
            int count = 0;
            for (Enemy e : enemies) {
                if (e != this && !e.isDead() && count < 5) {
                    float dist = (float) Math.sqrt(Math.pow(e.x - x, 2) + Math.pow(e.y - y, 2));
                    if (dist < 150) {
                        e.applyPoison(2000);
                        e.plasmaEndTime = System.currentTimeMillis() + 2000;
                        count++;
                    }
                }
            }
        } else {
            shockEndTime = Math.max(shockEndTime, System.currentTimeMillis() + duration);
        }
    }

    // ĐÂY LÀ HÀM BẠN ĐÃ LÀM THIẾU
    public java.util.List<gameproject.weapon.Projectile> shoot() {
        return null;
    }

    public java.util.List<Enemy> summon() {
        return null;
    }

    protected void applyPhysicsAndBounds(float moveX, float moveY, int screenW, int screenH) {
        x += moveX;
        y += moveY;
        x += kbX;
        y += kbY;
        kbX *= 0.85f;
        kbY *= 0.85f;
        x = Math.max(0, Math.min(x, screenW - size));
        y = Math.max(0, Math.min(y, screenH - size));
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

    public void takeDamage(int damage, VFXManager vfxManager, long currentTime) {
        if (poisonEndTime > currentTime) {
            damage = (int) (damage * 1.3f);
        }
        takeDamageBase(damage, vfxManager, currentTime, poisonEndTime > currentTime ? Color.GREEN : Color.WHITE);
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
            deathFadeStartTime = System.currentTimeMillis();
        }
        return isDying && (System.currentTimeMillis() - deathFadeStartTime >= deathFadeDuration);
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
        long now = System.currentTimeMillis();
        java.awt.image.BufferedImage img = ImageManager.get(imageKey);
        Graphics2D g2d = (Graphics2D) g;

        // Alpha: fade out khi đang chết
        float alpha = 1.0f;
        if (isDying && deathFadeStartTime >= 0) {
            alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
        }
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));

        if (img != null) {
            int drawX = (int) x - 10;
            int drawY = (int) y - 20;
            int drawW = size + 20;
            int drawH = size + 20;
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
    }

    public int getHp() {
        return hp;
    }
}