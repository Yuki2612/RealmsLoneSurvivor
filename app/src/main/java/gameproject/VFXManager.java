package gameproject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;

// Chuyển FireZone từ GamePanel sang đây
class FireZone {
    float x, y;
    long expireTime;
    boolean isExplosion;

    FireZone(float x, float y, long expireTime, boolean isExplosion) {
        this.x = x;
        this.y = y;
        this.expireTime = expireTime;
        this.isExplosion = isExplosion;
    }
}

public class VFXManager {
    public ArrayList<FireZone> fireZones = new ArrayList<>();
    private int shakeTimer = 0;
    private int currentDx = 0, currentDy = 0;

    public void triggerScreenShake(int durationFrames) {
        this.shakeTimer = durationFrames;
    }

    public void addExplosion(float x, float y, float radius, long currentTime) {
        fireZones.add(new FireZone(x - radius / 2, y - radius / 2, currentTime + 200, true));
    }

    public void addFireTrail(float x, float y, long currentTime) {
        fireZones.add(new FireZone(x, y, currentTime + 3000, false));
    }

    public void update(long currentTime) {
        Iterator<FireZone> fIt = fireZones.iterator();
        while (fIt.hasNext()) {
            if (currentTime > fIt.next().expireTime) {
                fIt.remove();
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
            currentDx = 0;
            currentDy = 0;
        }
    }

    public void resetScreenShake(Graphics2D g2d) {
        if (currentDx != 0 || currentDy != 0) {
            g2d.translate(-currentDx, -currentDy);
        }
    }

    public void draw(Graphics g, Player player) {
        int corpseLevel = player.getBreakthroughLevel(Upgrade.EXPLOSIVE_CORPSE);

        for (FireZone fz : fireZones) {
            if (fz.isExplosion) {
                g.setColor(new Color(255, 50, 50, 150));
                int radius = 60 + (corpseLevel * 20);
                g.fillOval((int) fz.x, (int) fz.y, radius, radius);
            } else {
                g.setColor(new Color(255, 100, 0, 150));
                g.fillRect((int) fz.x, (int) fz.y, 20, 20);
            }
        }
    }
}