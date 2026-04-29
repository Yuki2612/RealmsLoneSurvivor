package gameproject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import gameproject.skill.Upgrade;
import gameproject.meta.CharacterClass;
import gameproject.meta.PlayerData;

public class Player {
    private float x, y;
    private final int SIZE = 25;

    private float speed = 5.0f;
    private long dashCooldown = 2000;

    private int hearts = 3;
    private final int MAX_HEARTS = 15;
    private long invulnerableUntil = 0;

    // GOM CHUNG TOÀN BỘ NÂNG CẤP VÀO ĐÂY ĐỂ ĐẾM LEVEL
    private Map<Upgrade, Integer> upgradeLevels = new HashMap<>();

    private boolean up, down, left, right;
    private float lastDirX = 1, lastDirY = 0;
    private boolean isDashing = false;
    private long lastDashTime = 0;
    private long dashStartTime = 0;
    private float dashDirX = 0, dashDirY = 0;
    private final long DASH_DURATION = 150;
    private final float DASH_SPEED = 18.0f;

    // FSM & Animation System (PRO Version)
    public enum PlayerState {
        IDLE, RUN
    }
    private PlayerState currentState = PlayerState.IDLE;
    private String animDir = "down";
    private boolean facingRight = true;
    
    private Map<String, Animation> animations = new HashMap<>();
    private Animation activeAnim;

    public Player(float startX, float startY, CharacterClass charClass) {
        this.x = startX;
        this.y = startY;
        this.dashCooldown = (long)(2000 * (1.0f - PlayerData.statDashLevel * 0.02f));
        this.lastDashTime = -dashCooldown;
        this.hearts = charClass.baseHp + (PlayerData.statHealthLevel / 10);
        this.speed = (5.0f * charClass.speedMulti) * (1.0f + PlayerData.statSpeedLevel * 0.02f);
        
        initAnimations(charClass);
    }

    private void initAnimations(CharacterClass charClass) {
        String pKey = PlayerData.getPlayerImageKey();
        String fallback = "player1";

        // Định nghĩa các cặp State_Direction
        String[] states = {"idle", "run"};
        String[] dirs = {"side", "down", "up"};

        for (String s : states) {
            for (String d : dirs) {
                String key = s + "_" + d;
                int delay = s.equals("run") ? 6 : 8; // Chạy nhanh hơn đứng yên
                Animation anim = new Animation(delay);
                
                // Caching: Lấy từ ImageManager
                BufferedImage[] frames = ImageManager.getAnimation(pKey + "_" + key);
                if (frames == null) frames = ImageManager.getAnimation(fallback + "_" + key);
                
                // Fallback đặc biệt cho idle_up
                if (frames == null && s.equals("idle") && d.equals("up")) {
                    frames = ImageManager.getAnimation(pKey + "_run_up");
                    if (frames == null) frames = ImageManager.getAnimation(fallback + "_run_up");
                }

                if (frames != null) {
                    anim.setFrames(frames);
                    animations.put(key, anim);
                }
            }
        }
        
        // Mặc định
        activeAnim = animations.get("idle_down");
        if (activeAnim == null) activeAnim = animations.get("idle_side");
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

    public void update(int screenWidth, int screenHeight) {
        boolean isMoving = false;
        String nextDir = animDir;

        if (isDashing) {
            if (System.currentTimeMillis() - dashStartTime >= DASH_DURATION) {
                isDashing = false;
            } else {
                x += dashDirX * DASH_SPEED;
                y += dashDirY * DASH_SPEED;
                x = Math.max(0, Math.min(x, GamePanel.WORLD_WIDTH - SIZE));
                y = Math.max(0, Math.min(y, GamePanel.WORLD_HEIGHT - SIZE));
                isMoving = true;
            }
        } else {
            float currentDirX = 0, currentDirY = 0;
            if (up && y > 0) {
                y -= speed;
                currentDirY = -1;
                isMoving = true;
                nextDir = "up";
            }
            if (down && y < GamePanel.WORLD_HEIGHT - SIZE) {
                y += speed;
                currentDirY = 1;
                isMoving = true;
                nextDir = "down";
            }
            if (left && x > 0) {
                x -= speed;
                currentDirX = -1;
                isMoving = true;
                nextDir = "side";
                facingRight = false;
            }
            if (right && x < GamePanel.WORLD_WIDTH - SIZE) {
                x += speed;
                currentDirX = 1;
                isMoving = true;
                nextDir = "side";
                facingRight = true;
            }

            if (isMoving) {
                lastDirX = currentDirX;
                lastDirY = currentDirY;
            } else {
                // Hướng khi đứng yên dựa trên hướng cuối cùng
                if (lastDirY < 0) nextDir = "up";
                else if (lastDirY > 0) nextDir = "down";
                else nextDir = "side";
            }
        }

        // Chuyển đổi State & Animation thông qua FSM
        setState(isMoving ? PlayerState.RUN : PlayerState.IDLE, nextDir);
        
        if (activeAnim != null) {
            activeAnim.update();
        }
    }

    public void draw(Graphics g) {
        if (isInvulnerable() && System.currentTimeMillis() % 200 < 100)
            return;

        BufferedImage img = (activeAnim != null) ? activeAnim.getCurrentFrame() : null;

        if (img != null) {
            int drawX = (int) x - 10;
            int drawY = (int) y - 20;
            int drawSize = SIZE + 20;
            
            Graphics2D g2d = (Graphics2D) g.create();
            if (isDashing) {
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
            }

            if (animDir.equals("side") && !facingRight) {
                g2d.drawImage(img, drawX + drawSize, drawY, -drawSize, drawSize, null);
            } else {
                g2d.drawImage(img, drawX, drawY, drawSize, drawSize, null);
            }
            g2d.dispose();
        } else {
            // Fallback cuối: Khối vuông màu đỏ
            g.setColor(isDashing ? Color.CYAN : Color.RED);
            g.fillRect((int) x, (int) y, SIZE, SIZE);
        }
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> up = true;
            case KeyEvent.VK_S -> down = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SHIFT -> {
                if (!isDashing && System.currentTimeMillis() - lastDashTime >= dashCooldown) {
                    isDashing = true;
                    dashStartTime = System.currentTimeMillis();
                    lastDashTime = dashStartTime;
                    float length = (float) Math.sqrt(lastDirX * lastDirX + lastDirY * lastDirY);
                    if (length == 0) {
                        dashDirX = 1;
                        dashDirY = 0;
                    } else {
                        dashDirX = lastDirX / length;
                        dashDirY = lastDirY / length;
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
        if (hearts < MAX_HEARTS)
            hearts++;
    }

    public int getHearts() {
        return hearts;
    }

    public boolean takeHit() {
        if (isInvulnerable())
            return false;
        hearts--;
        invulnerableUntil = System.currentTimeMillis() + 1000;
        return hearts <= 0;
    }

    // LÕI XỬ LÝ LEVEL CỦA TẤT CẢ NÂNG CẤP (MAX LEVEL = 10)
    public void levelUpUpgrade(Upgrade u) {
        int current = upgradeLevels.getOrDefault(u, 0);
        if (current < 10)
            upgradeLevels.put(u, current + 1);
    }

    public int getUpgradeLevel(Upgrade u) {
        return upgradeLevels.getOrDefault(u, 0);
    }

    // Giữ nguyên hàm này để tránh báo lỗi các Kỹ năng cũ
    public void levelUpBreakthrough(Upgrade u) {
        levelUpUpgrade(u);
    }

    public int getBreakthroughLevel(Upgrade u) {
        return getUpgradeLevel(u);
    }

    public List<Upgrade> getOwnedBreakthroughs() {
        List<Upgrade> list = new ArrayList<>();
        for (Upgrade u : upgradeLevels.keySet()) {
            if (u.isBreakthrough)
                list.add(u);
        }
        return list;
    }

    public boolean isInvulnerable() {
        return System.currentTimeMillis() < invulnerableUntil;
    }

    public void addInvulnerability(long duration) {
        long now = System.currentTimeMillis();
        if (invulnerableUntil < now) invulnerableUntil = now;
        invulnerableUntil += duration;
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

    public boolean isDashing() {
        return isDashing;
    }

    public long getLastDashTime() {
        return lastDashTime;
    }

    public long getDashCooldown() {
        return dashCooldown;
    }
}