package gameproject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Player {
    private float x, y;
    private final int SIZE = 40;

    private float speed = 5.0f;
    private long dashCooldown = 2000;

    // Hệ thống HP
    private int hearts = 3;
    private final int MAX_HEARTS = 10;
    private long invulnerableUntil = 0;

    private Map<Upgrade, Integer> breakthroughLevels = new HashMap<>();

    private boolean up, down, left, right;
    private float lastDirX = 1, lastDirY = 0;
    private boolean isDashing = false;
    private long lastDashTime = 0;
    private long dashStartTime = 0;
    private float dashDirX = 0, dashDirY = 0;
    private final long DASH_DURATION = 150;
    private final float DASH_SPEED = 18.0f;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        this.lastDashTime = -dashCooldown;
    }

    // UPDATE ĐÃ NHẬN THÊM KÍCH THƯỚC MÀN HÌNH ĐỂ CHẶN BIÊN ĐỘNG
    public void update(int screenWidth, int screenHeight) {
        if (isDashing) {
            if (System.currentTimeMillis() - dashStartTime >= DASH_DURATION) {
                isDashing = false;
            } else {
                x += dashDirX * DASH_SPEED;
                y += dashDirY * DASH_SPEED;
                x = Math.max(0, Math.min(x, screenWidth - SIZE));
                y = Math.max(0, Math.min(y, screenHeight - SIZE));
            }
        } else {
            boolean isMoving = false;
            float currentDirX = 0, currentDirY = 0;
            if (up && y > 0) {
                y -= speed;
                currentDirY = -1;
                isMoving = true;
            }
            if (down && y < screenHeight - SIZE) {
                y += speed;
                currentDirY = 1;
                isMoving = true;
            }
            if (left && x > 0) {
                x -= speed;
                currentDirX = -1;
                isMoving = true;
            }
            if (right && x < screenWidth - SIZE) {
                x += speed;
                currentDirX = 1;
                isMoving = true;
            }
            if (isMoving) {
                lastDirX = currentDirX;
                lastDirY = currentDirY;
            }
        }
    }

    public void draw(Graphics g) {
        if (isInvulnerable() && System.currentTimeMillis() % 200 < 100)
            return;

        if (isDashing)
            g.setColor(Color.CYAN);
        else
            g.setColor(Color.RED);
        g.fillRect((int) x, (int) y, SIZE, SIZE);
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

    public void levelUpBreakthrough(Upgrade u) {
        breakthroughLevels.put(u, breakthroughLevels.getOrDefault(u, 0) + 1);
    }

    public int getBreakthroughLevel(Upgrade u) {
        return breakthroughLevels.getOrDefault(u, 0);
    }

    public List<Upgrade> getOwnedBreakthroughs() {
        return new ArrayList<>(breakthroughLevels.keySet());
    }

    public boolean isInvulnerable() {
        return System.currentTimeMillis() < invulnerableUntil;
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