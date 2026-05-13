package gameproject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import gameproject.skill.Upgrade;
import gameproject.meta.CharacterClass;
import gameproject.meta.PlayerData;
import gameproject.environment.MapManager;

public class Player implements Renderable {
    private float x, y;
    public static final int SIZE = 25;
    private CharacterClass charClass;
    private boolean isInWater = false;
    private long lastWaterSplashTime = 0;
    private float lastWaterSplashX, lastWaterSplashY;

    @Override
    public void render(Graphics2D g) {
        draw(g);
    }

    @Override
    public float getBottomY() {
        return y + SIZE;
    }

    private float speed = 3.5f;
    private long dashCooldown = 2000;

    private int hearts = 3;
    private int maxHearts = 5;
    private long invulnerableUntil = 0;

    // Evolution Buffs
    private long adrenalineEndTime = 0;
    private boolean shouldTriggerPhantomExplosion = false;

    // Frenzy Evolution
    private long frenzyEndTime = 0;
    private long frenzyLastTriggerTime = 0;
    private final long FRENZY_DURATION = 2000;
    private final long FRENZY_COOLDOWN = 4000;

    private Map<Upgrade, Integer> upgradeLevels = new ConcurrentHashMap<>();

    private boolean up, down, left, right;
    private float lastDirX = 1, lastDirY = 0;
    private boolean isDashing = false;
    private long lastDashTime = 0;
    private long dashStartTime = 0;
    private float dashDirX = 0, dashDirY = 0;
    private final long DASH_DURATION = 150;
    private final float DASH_SPEED = 18.0f;

    public enum PlayerState {
        IDLE, RUN
    }

    private PlayerState currentState = PlayerState.IDLE;
    private String animDir = "down";
    private boolean facingRight = true;

    private Map<String, Animation> animations = new HashMap<>();
    private Animation activeAnim;

    // Combo System integration
    private ComboManager comboManager;

    // Slow system
    private float slowMultiplier = 1.0f;
    private long slowEndTime = 0;

    private boolean isCarryingRune = false;

    public Player(float startX, float startY, CharacterClass charClass) {
        this.x = startX;
        this.y = startY;
        this.dashCooldown = (long) (2000 * (1.0f - PlayerData.statDashLevel * 0.02f));
        this.lastDashTime = -dashCooldown;
        this.maxHearts = charClass.baseHp + PlayerData.statHealthLevel;
        this.hearts = this.maxHearts;
        this.speed = (4.0f * charClass.speedMulti) * (1.0f + PlayerData.statSpeedLevel * 0.02f);
        this.comboManager = new ComboManager();
        this.charClass = charClass;

        initAnimations(charClass);
    }

    private void initAnimations(CharacterClass charClass) {
        String pKey = PlayerData.getPlayerImageKey();
        String fallback = "player1";
        String[] states = { "idle", "run" };
        String[] dirs = { "side", "down", "up" };

        for (String s : states) {
            for (String d : dirs) {
                String key = s + "_" + d;
                int delay = s.equals("run") ? 6 : 8;
                Animation anim = new Animation(delay);
                BufferedImage[] frames = ImageManager.getAnimation(pKey + "_" + key);
                if (frames == null)
                    frames = ImageManager.getAnimation(fallback + "_" + key);
                if (frames == null && s.equals("idle") && d.equals("up")) {
                    frames = ImageManager.getAnimation(pKey + "_run_up");
                    if (frames == null)
                        frames = ImageManager.getAnimation(fallback + "_run_up");
                }
                if (frames != null) {
                    anim.setFrames(frames);
                    animations.put(key, anim);
                }
            }
        }
        activeAnim = animations.get("idle_down");
        if (activeAnim == null)
            activeAnim = animations.get("idle_side");
    }

    private void setState(PlayerState newState, String newDir) {
        String stateKey = (newState == PlayerState.RUN ? "run" : "idle") + "_" + newDir;
        Animation nextAnim = animations.get(stateKey);
        if (nextAnim != null && nextAnim != activeAnim) {
            activeAnim = nextAnim;
        }
        currentState = newState;
        animDir = newDir;
    }

    public void update(GamePanel game) {
        comboManager.update();
        boolean isMoving = false;
        String nextDir = animDir;

        // --- MÔI TRƯỜNG: LÀM CHẬM KHI ĐI TRONG NƯỚC ---
        float pCenterX = x + SIZE / 2f;
        float pCenterY = y + SIZE / 2f;
        String tileType = game.mapManager.getTileTypeAtWorld(pCenterX, pCenterY);
        isInWater = tileType.equals("water");
        if (isInWater) {
            applySlow(0.7f, 100); // Giảm 30% tốc độ (còn 70%)
        }

        if (isDashing) {
            if (gameproject.GamePanel.getTickTime() - dashStartTime >= DASH_DURATION) {
                isDashing = false;
            } else {
                float nextX = x + dashDirX * DASH_SPEED;
                float nextY = y + dashDirY * DASH_SPEED;

                // Sử dụng hitbox chân (Footprint) để va chạm mượt hơn
                int footW = 16;
                int footH = 10;
                float fx = nextX + (SIZE - footW) / 2f;
                float fy = nextY + SIZE - footH;

                if (!game.mapManager.isColliding(fx, fy, footW, footH)) {
                    x = nextX;
                    y = nextY;
                }
                x = Math.max(0, Math.min(x, GamePanel.WORLD_WIDTH - SIZE));
                y = Math.max(0, Math.min(y, GamePanel.WORLD_HEIGHT - SIZE));
                isMoving = true;

                // HANDLE PHANTOM DASH EXPLOSION (Moved to update where 'game' is available)
                if (shouldTriggerPhantomExplosion) {
                    shouldTriggerPhantomExplosion = false;
                    float radius = 100 + (PlayerData.evoPhantomDash * 30);
                    int dmg = (int) (game.upgradeManager.playerDamage * (0.8f + PlayerData.evoPhantomDash * 0.2f));
                    game.vfxManager.addExplosion(x, y, radius, dashStartTime);
                    game.vfxManager.triggerScreenShake(8);

                    synchronized (game.entityManager.enemies) {
                        for (gameproject.entity.Enemy en : new java.util.ArrayList<>(game.entityManager.enemies)) {
                            float edist = (float) Math.sqrt(Math.pow(en.getX() - x, 2) + Math.pow(en.getY() - y, 2));
                            if (edist < radius) {
                                en.takeDamageDirect(dmg, false, game.vfxManager, dashStartTime);
                            }
                        }
                    }
                }
            }
        } else {
            float currentDirX = 0, currentDirY = 0;

            // Tính toán tốc độ hiện tại (Speed * Combo Bonus * Slow Multiplier * Adrenaline
            // Bonus)
            float effectiveSlow = 1.0f;
            if (gameproject.GamePanel.getTickTime() < slowEndTime) {
                effectiveSlow = slowMultiplier;
            }
            float adrelBonus = 0f;
            if (gameproject.GamePanel.getTickTime() < adrenalineEndTime) {
                adrelBonus = (PlayerData.evoBerserker * 0.07f); // +7% per level
            }
            float currentSpeed = speed * (1.0f + comboManager.getMoveSpeedBonus() + adrelBonus) * effectiveSlow;

            int footW = 16;
            int footH = 10;

            if (up && y > MapManager.TILE_SIZE) {
                float nextY = y - currentSpeed;
                float fx = x + (SIZE - footW) / 2f;
                float fy = nextY + SIZE - footH;
                if (!game.mapManager.isColliding(fx, fy, footW, footH)) {
                    y = nextY;
                    currentDirY = -1;
                    isMoving = true;
                    nextDir = "up";
                }
            }
            if (down && y < GamePanel.WORLD_HEIGHT - SIZE - MapManager.TILE_SIZE) {
                float nextY = y + currentSpeed;
                float fx = x + (SIZE - footW) / 2f;
                float fy = nextY + SIZE - footH;
                if (!game.mapManager.isColliding(fx, fy, footW, footH)) {
                    y = nextY;
                    currentDirY = 1;
                    isMoving = true;
                    nextDir = "down";
                }
            }
            if (left && x > MapManager.TILE_SIZE) {
                float nextX = x - currentSpeed;
                float fx = nextX + (SIZE - footW) / 2f;
                float fy = y + SIZE - footH;
                if (!game.mapManager.isColliding(fx, fy, footW, footH)) {
                    x = nextX;
                    currentDirX = -1;
                    isMoving = true;
                    nextDir = "side";
                    facingRight = false;
                }
            }
            if (right && x < GamePanel.WORLD_WIDTH - SIZE - MapManager.TILE_SIZE) {
                float nextX = x + currentSpeed;
                float fx = nextX + (SIZE - footW) / 2f;
                float fy = y + SIZE - footH;
                if (!game.mapManager.isColliding(fx, fy, footW, footH)) {
                    x = nextX;
                    currentDirX = 1;
                    isMoving = true;
                    nextDir = "side";
                    facingRight = true;
                }
            }
            if (isMoving) {
                lastDirX = currentDirX;
                lastDirY = currentDirY;
            } else {
                if (lastDirY < 0)
                    nextDir = "up";
                else if (lastDirY > 0)
                    nextDir = "down";
                else
                    nextDir = "side";
            }
        }
        setState(isMoving ? PlayerState.RUN : PlayerState.IDLE, nextDir);
        if (activeAnim != null)
            activeAnim.update();

        // 6. KIỂM TRA THẢ RUNE TẠI BÀN THỜ
        if (isCarryingRune) {
            for (gameproject.environment.Obstacle obs : game.mapManager.getAllObstacles()) {
                if (obs instanceof gameproject.environment.Altar) {
                    gameproject.environment.Altar altar = (gameproject.environment.Altar) obs;
                    float distSq = (float) ((x + SIZE / 2 - (altar.x + 32)) * (x + SIZE / 2 - (altar.x + 32)) +
                            (y + SIZE / 2 - (altar.y + 32)) * (y + SIZE / 2 - (altar.y + 32)));
                    if (distSq < 360 * 360) { // Khoảng cách nộp Rune (Tăng gấp đôi từ 180)
                        altar.addRune();
                        isCarryingRune = false;
                        String msg = "";
                        switch (altar.getRuneCount()) {
                            case 1:
                                msg = "THE ALTAR PULSES... THE ANCIENT GUARDIAN'S SKIN BEGINS TO CRACK.";
                                break;
                            case 2:
                                msg = "ANCIENT ENERGY FLOWS... THE GUARDIAN'S DEFENSES ARE WEAKENING.";
                                break;
                            case 3:
                                msg = "THE SWAMP TREMBLES... THE GUARDIAN IS VULNERABLE!";
                                break;
                            case 4:
                                msg = "THE SEALS ARE BROKEN! THE ANCIENT GUARDIAN IS EXPOSED!";
                                break;
                        }
                        game.vfxManager.showWaveBanner(msg, Color.YELLOW, gameproject.GamePanel.getTickTime());
                        SoundManager.play("powerup");
                    }
                }
            }
        }

        // Phát âm thanh lội nước định kỳ và sinh particle bắn nước
        if (isInWater && isMoving && !isDashing) {
            long now = gameproject.GamePanel.getTickTime();

            // Sinh particle bắn nước lùi về sau hướng di chuyển một chút cho chân thực
            float offX = -lastDirX * 12;
            float offY = -lastDirY * 8;
            game.vfxManager.spawnWaterSplash(x + SIZE / 2 + offX, y + SIZE - 22 + offY, now);

            float dx = x - lastWaterSplashX;
            float dy = y - lastWaterSplashY;
            float distSq = dx * dx + dy * dy;

            // Phát âm thanh nếu di chuyển > 70px và đã qua > 900ms
            if (distSq >= 4900 && now - lastWaterSplashTime >= 900) {
                SoundManager.play("water_splash", 0.4f);
                lastWaterSplashTime = now;
                lastWaterSplashX = x;
                lastWaterSplashY = y;
            }
        }
    }

    public void draw(Graphics g) {
        if (isInvulnerable() && gameproject.GamePanel.getTickTime() % 200 < 100)
            return;
        BufferedImage img = (activeAnim != null) ? activeAnim.getCurrentFrame() : null;
        if (img != null) {
            int drawX = (int) Math.round(x) - 10;
            int drawY = (int) Math.round(y) - 20;
            int drawSize = SIZE + 20;
            Graphics2D g2d = (Graphics2D) g.create();
            if (isDashing) {
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
            }
            if (isInWater) {
                // Hiệu ứng vòng nước/bùn dưới chân: Điều chỉnh vị trí khớp với điểm bị cắt
                // (drawSize - 18)
                Graphics2D rippleG = (Graphics2D) g2d.create();
                rippleG.setColor(new Color(20, 35, 15, 150)); // Màu xanh đen đậm của đầm lầy
                rippleG.fillOval(drawX + 4, drawY + drawSize - 22, drawSize - 8, 12);
                rippleG.dispose();

                // Hiệu ứng lún xuống nước/bùn: Cắt bỏ phần dưới (từ bắp chân xuống)
                g2d.setClip(drawX, drawY, drawSize, drawSize - 18);
            }
            if (animDir.equals("side") && !facingRight) {
                g2d.drawImage(img, drawX + drawSize, drawY, -drawSize, drawSize, null);
            } else {
                g2d.drawImage(img, drawX, drawY, drawSize, drawSize, null);
            }
            g2d.dispose();

            // Vẽ Rune trên đầu nếu đang mang
            if (isCarryingRune) {
                BufferedImage runeImg = ImageManager.get("rune");
                if (runeImg != null) {
                    // Tăng kích cỡ lên 32x32 và đẩy cao lên y - 45
                    g.drawImage(runeImg, (int) x + SIZE / 2 - 16, (int) y - 45, 32, 32, null);
                }
            }
        } else {
            g.setColor(isDashing ? Color.CYAN : Color.RED);
            g.fillRect((int) Math.round(x), (int) Math.round(y), SIZE, SIZE);
        }
        if (GamePanel.showHitboxes) {
            g.setColor(Color.GREEN);
            Rectangle b = getBounds();
            g.drawRect((int) Math.round(b.x), (int) Math.round(b.y), b.width, b.height);
        }
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> up = true;
            case KeyEvent.VK_S -> down = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SHIFT -> {
                if (!isDashing && gameproject.GamePanel.getTickTime() - lastDashTime >= dashCooldown) {
                    isDashing = true;
                    dashStartTime = gameproject.GamePanel.getTickTime();
                    lastDashTime = dashStartTime;
                    float length = (float) Math.sqrt(lastDirX * lastDirX + lastDirY * lastDirY);
                    if (length == 0) {
                        dashDirX = 1;
                        dashDirY = 0;
                    } else {
                        dashDirX = lastDirX / length;
                        dashDirY = lastDirY / length;
                    }

                    // Trigger Phantom Dash flag
                    if (PlayerData.evoPhantomDash > 0) {
                        shouldTriggerPhantomExplosion = true;
                    }
                }
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> up = false;
            case KeyEvent.VK_S -> down = false;
            case KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_D -> right = false;
        }
    }

    public void resetMovement() {
        up = down = left = right = false;
    }

    public void upgradeSpeed(float amount) {
        this.speed += amount;
    }

    public void upgradeDashCooldown(long reduction) {
        this.dashCooldown = Math.max(500, this.dashCooldown - reduction);
    }

    public void addHeart() {
        if (hearts < maxHearts)
            hearts++;
    }

    public void addMaxHeart() {
        maxHearts++;
        addHeart(); // Heal 1 HP when gaining a max heart
    }

    public int getHearts() {
        return hearts;
    }

    public int getMaxHearts() {
        return maxHearts;
    }

    public float getSpeed() {
        return speed;
    }

    public boolean hasShield = false;

    public boolean takeHit() {
        if (isInvulnerable())
            return false;

        if (hasShield) {
            hasShield = false;
            addInvulnerability(1000); // 1s grace period
            gameproject.SoundManager.play("shield");
            return false;
        }

        hearts--;

        // ADRENALINE EVOLUTION: Speed boost on damage
        if (PlayerData.evoBerserker > 0) {
            adrenalineEndTime = gameproject.GamePanel.getTickTime() + 3000; // 3 seconds
        }

        // Phát âm thanh bị thương ngẫu nhiên (1-3)
        int randHurt = 1 + (int) (Math.random() * 3);
        SoundManager.play("playerhurt" + randHurt);

        invulnerableUntil = gameproject.GamePanel.getTickTime() + 1000;
        return hearts <= 0;
    }

    public boolean takeDamage(int amount) {
        // Boss hoặc môi trường gây sát thương trực tiếp
        if (isInvulnerable())
            return false;

        // Hiện tại mỗi lần dính chiêu Boss trừ 1 tim (theo yêu cầu mới)
        return takeHit();
    }

    public void applySlow(float multiplier, long duration) {
        this.slowMultiplier = multiplier;
        this.slowEndTime = gameproject.GamePanel.getTickTime() + duration;
    }

    public void levelUpUpgrade(Upgrade u) {
        int current = upgradeLevels.getOrDefault(u, 0);
        if (current < 10)
            upgradeLevels.put(u, current + 1);
    }

    public int getUpgradeLevel(Upgrade u) {
        return upgradeLevels.getOrDefault(u, 0);
    }

    public boolean isCarryingRune() {
        return isCarryingRune;
    }

    public void setCarryingRune(boolean b) {
        this.isCarryingRune = b;
    }

    public void collectRune(gameproject.entity.RuneItem ri) {
        this.isCarryingRune = true;
        SoundManager.play("powerup");
    }

    public int getBreakthroughLevel(Upgrade u) {
        return getUpgradeLevel(u);
    }

    public List<Upgrade> getOwnedBreakthroughs() {
        List<Upgrade> list = new ArrayList<>();
        synchronized (upgradeLevels) {
            for (Upgrade u : upgradeLevels.keySet()) {
                if (u.isBreakthrough)
                    list.add(u);
            }
        }
        return list;
    }

    public boolean isInvulnerable() {
        return gameproject.GamePanel.getTickTime() < invulnerableUntil;
    }

    public void addInvulnerability(long duration) {
        long now = gameproject.GamePanel.getTickTime();
        if (invulnerableUntil < now)
            invulnerableUntil = now;
        invulnerableUntil += duration;
    }

    public void triggerFrenzy() {
        if (PlayerData.evoFrenzy <= 0)
            return;
        long now = gameproject.GamePanel.getTickTime();
        if (now - frenzyLastTriggerTime >= FRENZY_COOLDOWN) {
            frenzyEndTime = now + FRENZY_DURATION;
            frenzyLastTriggerTime = now;
        }
    }

    public float getFrenzyFireRateBonus() {
        if (gameproject.GamePanel.getTickTime() < frenzyEndTime) {
            return PlayerData.evoFrenzy * 0.05f; // +5% per level (Max 25%)
        }
        return 0f;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public Animation getActiveAnim() {
        return activeAnim;
    }

    public boolean isDashing() {
        return isDashing;
    }

    public boolean isMoving() {
        return currentState == PlayerState.RUN || isDashing;
    }

    public long getLastDashTime() {
        return lastDashTime;
    }

    public long getDashCooldown() {
        return dashCooldown;
    }

    public ComboManager getComboManager() {
        return comboManager;
    }

    public CharacterClass getCharClass() {
        return charClass;
    }
}