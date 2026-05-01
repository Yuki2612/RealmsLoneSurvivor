package gameproject;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;

import gameproject.entity.Enemy;
import gameproject.meta.PlayerData;

public class VFXManager {

    // ── Inner classes ──────────────────────────────────────────────
    public static class FireZone {
        public float x, y;
        public long expireTime;
        public boolean isExplosion, isAcid;
        public int radius;

        public FireZone(float x, float y, long expireTime, boolean isExplosion, boolean isAcid, int radius) {
            this.x = x; this.y = y; this.expireTime = expireTime;
            this.isExplosion = isExplosion; this.isAcid = isAcid; this.radius = radius;
        }
    }

    public static class DamageText {
        public float x, y;
        public int damage;
        public long expireTime;
        public Color color;
        public boolean isCrit;

        public DamageText(float x, float y, int damage, long currentTime, Color color, boolean isCrit) {
            this.x = x; this.y = y; this.damage = damage;
            this.expireTime = currentTime + (isCrit ? 900 : 700);
            this.color = color; this.isCrit = isCrit;
        }
    }

    public static class LaserVFX {
        public float x1, y1, x2, y2;
        public long expireTime;
        public LaserVFX(float x1, float y1, float x2, float y2, long currentTime) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.expireTime = currentTime + 400;
        }
    }

    public static class Particle {
        public float x, y, vx, vy;
        public long expireTime;
        public Color color;
        public int size;

        public Particle(float x, float y, float vx, float vy, long currentTime, Color color, int size, int lifespanMs) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.expireTime = currentTime + lifespanMs;
            this.color = color; this.size = size;
        }
    }

    public static class DashAfterimage {
        public float x, y;
        public long expireTime;

        public DashAfterimage(float x, float y, long currentTime) {
            this.x = x; this.y = y;
            this.expireTime = currentTime + 150;
        }
    }

    public static class WaveBanner {
        public String text;
        public Color color;
        public long expireTime;

        public WaveBanner(String text, Color color, long currentTime) {
            this.text = text; this.color = color;
            this.expireTime = currentTime + 2500;
        }
    }

    // ── State variables ────────────────────────────────────────────
    public ArrayList<FireZone>       fireZones      = new ArrayList<>();
    public ArrayList<DamageText>     damageTexts    = new ArrayList<>();
    public ArrayList<LaserVFX>       lasers         = new ArrayList<>();
    public ArrayList<Particle>       particles      = new ArrayList<>();
    public ArrayList<DashAfterimage> afterimages    = new ArrayList<>();
    private ArrayList<WaveBanner>    waveBanners    = new ArrayList<>();
    
    public void clearAll() {
        synchronized(fireZones) { fireZones.clear(); }
        synchronized(damageTexts) { damageTexts.clear(); }
        synchronized(lasers) { lasers.clear(); }
        synchronized(particles) { particles.clear(); }
        synchronized(afterimages) { afterimages.clear(); }
        synchronized(waveBanners) { waveBanners.clear(); }
        shakeTimer = 0;
    }

    public boolean showDamageText = true;
    private int shakeTimer = 0;
    private int currentDx = 0, currentDy = 0;

    private long flashEndTime = 0;
    private int flashDuration = 1000;
    private long playerDamageFlashEndTime = 0;

    private RadialGradientPaint cachedComboPaint = null;
    private float cachedComboRadius = -1;
    
    private java.awt.image.BufferedImage darknessVignette = null;

    // ── Public API ─────────────────────────────────────────────────

    public void triggerScreenShake(int durationFrames) { this.shakeTimer = durationFrames; }

    public void triggerPlayerDamageFlash(long currentTime) {
        playerDamageFlashEndTime = currentTime + 350;
    }

    public void addExplosion(float x, float y, float radius, long currentTime) {
        fireZones.add(new FireZone(x - radius / 2, y - radius / 2, currentTime + 200, true, false, (int) radius));
        spawnParticleBurst(x, y, 12, currentTime, new Color(255, 100, 0), new Color(255, 220, 50), 5, 8, 500);
    }

    public void addFireTrail(float x, float y, long currentTime) {
        fireZones.add(new FireZone(x, y, currentTime + 3000, false, false, 20));
    }

    public void addAcidZone(float x, float y, float radius, long currentTime) {
        fireZones.add(new FireZone(x - radius / 2, y - radius / 2, currentTime + 5000, false, true, (int) radius));
    }

    public void addLaser(float x1, float y1, float x2, float y2, long currentTime) {
        lasers.add(new LaserVFX(x1, y1, x2, y2, currentTime));
    }

    public void addDamageText(float x, float y, int damage, long currentTime, Color color) {
        if (!showDamageText) return;
        float ox = (float) (Math.random() * 20 - 10);
        float oy = (float) (Math.random() * 10 - 5);
        damageTexts.add(new DamageText(x + ox, y + oy, damage, currentTime, color, false));
    }

    public void addDamageText(float x, float y, int damage, long currentTime) {
        addDamageText(x, y, damage, currentTime, Color.WHITE);
    }

    public void addCritDamageText(float x, float y, int damage, long currentTime, Color color) {
        if (!showDamageText) return;
        float ox = (float) (Math.random() * 20 - 10);
        float oy = (float) (Math.random() * 10 - 5);
        damageTexts.add(new DamageText(x + ox, y + oy, damage, currentTime, color, true));
    }

    public void addCritDamageText(float x, float y, int damage, long currentTime) {
        addCritDamageText(x, y, damage, currentTime, new Color(255, 220, 0));
    }

    public void spawnDeathParticles(float x, float y, long currentTime, Color baseColor) {
        spawnParticleBurst(x, y, 8, currentTime, baseColor, Color.WHITE, 2, 4, 200);
    }

    public void addDashAfterimage(float playerX, float playerY, long currentTime) {
        afterimages.add(new DashAfterimage(playerX, playerY, currentTime));
    }

    public void showWaveBanner(String text, Color color, long currentTime) {
        waveBanners.clear();
        waveBanners.add(new WaveBanner(text, color, currentTime));
    }

    public void spawnComboSparkles(float cx, float cy, long currentTime, Color color, int tier) {
        if (Math.random() < 0.25) {
            double angle = Math.random() * 2 * Math.PI;
            float speed = 0.5f + (float) (Math.random() * 1.5f);
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed - 1.2f;
            
            Color pColor = color;
            int sz = 3;
            int life = 600;
            if (tier == 1) {
                pColor = new Color(200, 240, 255, 180);
                sz = 2; life = 800;
            }
            particles.add(new Particle(cx, cy, vx, vy, currentTime, pColor, sz, life));
        }
    }

    private void spawnParticleBurst(float cx, float cy, int count, long currentTime,
                                    Color color1, Color color2, int minSize, int maxSize, int lifespanMs) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            float speed = 1.5f + (float) (Math.random() * 4.0f);
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            Color c = Math.random() < 0.5 ? color1 : color2;
            int sz = minSize + (int) (Math.random() * (maxSize - minSize));
            particles.add(new Particle(cx, cy, vx, vy, currentTime, c, sz, lifespanMs));
        }
    }

    public void update(long currentTime) {
        synchronized (fireZones) { fireZones.removeIf(fz -> currentTime > fz.expireTime); }
        synchronized (lasers) { lasers.removeIf(l -> currentTime > l.expireTime); }
        synchronized (afterimages) { afterimages.removeIf(a -> currentTime > a.expireTime); }
        synchronized (waveBanners) { waveBanners.removeIf(b -> currentTime > b.expireTime); }

        synchronized (damageTexts) {
            Iterator<DamageText> dIt = damageTexts.iterator();
            while (dIt.hasNext()) {
                DamageText dt = dIt.next();
                if (currentTime > dt.expireTime) dIt.remove();
                else dt.y -= (dt.isCrit ? 0.9f : 0.6f);
            }
        }

        synchronized (particles) {
            Iterator<Particle> pIt = particles.iterator();
            while (pIt.hasNext()) {
                Particle p = pIt.next();
                if (currentTime > p.expireTime) { pIt.remove(); continue; }
                p.x += p.vx; p.y += p.vy; p.vy += 0.12f; p.vx *= 0.92f;
            }
        }
    }

    public void applyScreenShake(Graphics2D g2d) {
        if (shakeTimer > 0) {
            currentDx = (int) (Math.random() * 10 - 5);
            currentDy = (int) (Math.random() * 10 - 5);
            g2d.translate(currentDx, currentDy);
            shakeTimer--;
        } else {
            currentDx = currentDy = 0;
        }
    }

    public void resetScreenShake(Graphics2D g2d) {
        if (currentDx != 0 || currentDy != 0) g2d.translate(-currentDx, -currentDy);
    }

    public void draw(Graphics g, Player player) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        synchronized (afterimages) {
            for (DashAfterimage a : afterimages) {
                java.awt.image.BufferedImage img = ImageManager.get(PlayerData.getPlayerImageKey());
                if (img == null) img = ImageManager.get("player");
                if (img != null) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                    g2d.drawImage(img, (int) a.x - 10, (int) a.y - 20, 45, 45, null);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }
        }

        synchronized (lasers) {
            for (LaserVFX l : lasers) {
                g2d.setColor(new Color(0, 255, 255, 220));
                g2d.setStroke(new BasicStroke(24));
                g2d.drawLine((int) l.x1, (int) l.y1, (int) l.x2, (int) l.y2);
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(10));
                g2d.drawLine((int) l.x1, (int) l.y1, (int) l.x2, (int) l.y2);
                g2d.setStroke(new BasicStroke(1));
            }
        }

        synchronized (fireZones) {
            for (FireZone fz : fireZones) {
                if (fz.isExplosion) {
                    g.setColor(new Color(255, 50, 50, 150));
                    g.fillOval((int) fz.x, (int) fz.y, fz.radius, fz.radius);
                } else if (fz.isAcid) {
                    g.setColor(new Color(50, 255, 50, 80));
                    g.fillOval((int) fz.x, (int) fz.y, fz.radius, fz.radius);
                } else {
                    g.setColor(new Color(255, 100, 0, 150));
                    g.fillRect((int) fz.x, (int) fz.y, fz.radius, fz.radius);
                }
            }
        }

        synchronized (particles) {
            for (Particle p : particles) {
                g2d.setColor(p.color);
                g2d.fillRect((int) p.x, (int) p.y, p.size, p.size);
            }
        }

        if (showDamageText) {
            synchronized (damageTexts) {
                for (DamageText dt : damageTexts) {
                    g2d.setFont(FontManager.getFont(dt.isCrit ? 22f : 15f));
                    String text = (dt.isCrit ? "! " : "") + dt.damage;
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(text, (int) dt.x + 1, (int) dt.y + 1);
                    g2d.setColor(dt.color);
                    g2d.drawString(text, (int) dt.x, (int) dt.y);
                }
            }
        }
    }

    private void initDarknessVignette(int screenW, int screenH) {
        int vSize = (int) (Math.sqrt(screenW * screenW + screenH * screenH) * 1.5f);
        darknessVignette = new java.awt.image.BufferedImage(vSize, vSize, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = darknessVignette.createGraphics();

        float visionRadius = 110f;
        float blurRadius = 260f;
        float center = vSize / 2f;

        float[] fractions = { 0.0f, (visionRadius * 0.7f) / center, visionRadius / center, blurRadius / center, 1.0f };
        Color[] colors = { new Color(0,0,0,0), new Color(0,0,0,0), new Color(0,0,0,120), Color.BLACK, Color.BLACK };

        RadialGradientPaint paint = new RadialGradientPaint(new Point2D.Float(center, center), center, fractions, colors);
        g2.setPaint(paint);
        g2.fillRect(0, 0, vSize, vSize);
        g2.dispose();
    }

    public void drawOverlays(Graphics g, int screenW, int screenH, long currentTime, gameproject.GamePanel game) {
        Graphics2D g2d = (Graphics2D) g;

        // 1. Flash
        if (currentTime < playerDamageFlashEndTime) {
            float alpha = 0.35f * (float) (playerDamageFlashEndTime - currentTime) / 350f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.35f, alpha)));
            g2d.setColor(Color.RED);
            g2d.fillRect(0, 0, screenW, screenH);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 2. Combo Tier 3
        if (game.player.getComboManager().getTier() >= 3) {
            float pulse = (float) (Math.sin(currentTime / 150.0) * 0.2f + 0.4f);
            float radius = (float) Math.sqrt(screenW * screenW + screenH * screenH) / 2.0f;
            if (radius != cachedComboRadius || cachedComboPaint == null) {
                cachedComboRadius = radius;
                float[] fr = { 0.0f, 0.6f, 1.0f };
                Color[] cl = { new Color(0,0,0,0), new Color(0,0,0,0), new Color(255, 140, 0, 255) };
                cachedComboPaint = new RadialGradientPaint(new Point2D.Float(screenW/2f, screenH/2f), radius, fr, cl);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
            g2d.setPaint(cachedComboPaint);
            g2d.fillRect(0, 0, screenW, screenH);
            g2d.setPaint(null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3. Acid Rain
        if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.ACID_RAIN &&
            gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
            g2d.setColor(new Color(150, 255, 0, 40));
            g2d.fillRect(0, 0, screenW, screenH);
            g2d.setColor(new Color(100, 255, 50, 100));
            for (int i = 0; i < 30; i++) {
                int rx = (int) ((currentTime / 20 + i * 137) % screenW);
                int ry = (int) ((currentTime / 2 + i * 89) % screenH);
                g2d.drawLine(rx, ry, rx - 2, ry + 10);
            }
        }

        // 4. Darkness
        if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.DARKNESS &&
            gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
            if (darknessVignette == null) initDarknessVignette(screenW, screenH);
            int screenX = (int) Math.round(game.player.getX() + game.player.getBounds().width / 2f) - game.camIntX;
            int screenY = (int) Math.round(game.player.getY() + game.player.getBounds().height / 2f) - game.camIntY;
            g2d.drawImage(darknessVignette, screenX - darknessVignette.getWidth()/2, screenY - darknessVignette.getHeight()/2, null);
        } else if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.DARKNESS &&
                   gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.WARNING) {
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillRect(0, 0, screenW, screenH);
        } else if (gameproject.state.PlayingState.activeEvent == gameproject.state.PlayingState.EventType.BLOOD_MOON &&
                gameproject.state.PlayingState.eventPhase == gameproject.state.PlayingState.EventPhase.ACTIVE) {
            
            boolean highTier = game.player.getComboManager().getTier() >= 3;
            
            // 1. Tông màu đỏ toàn màn hình (Giảm độ đậm nếu đang có Combo Tier 3 để tránh lóa)
            g2d.setColor(new Color(180, 0, 0, highTier ? 12 : 25));
            g2d.fillRect(0, 0, screenW, screenH);
            
            // 2. Viền đỏ pulsing (Blood Vignette) - Crimson Deep Red
            float basePulse = (float) (Math.sin(currentTime / 200.0) * 0.12f + 0.3f);
            float pulse = highTier ? basePulse * 0.6f : basePulse; // Giảm rung lắc nếu đang combo cao
            
            // Bán kính lớn hơn Combo Tier 3 một chút để tạo lớp phủ bên ngoài
            float radius = (float) Math.sqrt(screenW * screenW + screenH * screenH) / 1.3f;
            
            float[] fr = { 0.0f, 0.65f, 1.0f };
            // Dùng màu Crimson đậm để phân biệt với màu Cam của Combo
            Color[] cl = { new Color(0,0,0,0), new Color(100, 0, 0, 0), new Color(139, 0, 0, highTier ? 140 : 200) };
            RadialGradientPaint bloodPaint = new RadialGradientPaint(new Point2D.Float(screenW/2f, screenH/2f), radius, fr, cl);
            
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
            g2d.setPaint(bloodPaint);
            g2d.fillRect(0, 0, screenW, screenH);
            g2d.setPaint(null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 5. Wave banners
        synchronized (waveBanners) {
            for (WaveBanner b : waveBanners) {
                float life = (float) (b.expireTime - currentTime) / 2500f;
                float alpha = Math.min(1f, life * 3f);
                if (alpha <= 0) continue;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setFont(FontManager.getFont(b.text.length() > 20 ? 32f : 52f));
                int tw = g2d.getFontMetrics().stringWidth(b.text);
                int bx = screenW / 2 - tw / 2;
                int by = screenH / 3;
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRoundRect(bx - 30, by - 55, tw + 60, 75, 16, 16);
                g2d.setColor(Color.BLACK);
                g2d.drawString(b.text, bx + 3, by + 3);
                g2d.setColor(b.color);
                g2d.drawString(b.text, bx, by);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        }
    }
}