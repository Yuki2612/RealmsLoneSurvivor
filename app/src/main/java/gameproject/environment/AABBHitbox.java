package gameproject.environment;

import java.awt.Graphics;

public class AABBHitbox implements Hitbox {
    public float x, y, width, height;

    public AABBHitbox(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    @Override
    public boolean intersects(float rx, float ry, float rw, float rh) {
        return rx < x + width && rx + rw > x && ry < y + height && ry + rh > y;
    }

    @Override
    public void draw(Graphics g) {
        g.drawRect((int) Math.round(x) - gameproject.GamePanel.instance.camIntX,
                (int) Math.round(y) - gameproject.GamePanel.instance.camIntY, (int) width, (int) height);
    }

    @Override
    public void drawAbsolute(Graphics g, int drawX, int drawY, int worldX, int worldY) {
        int offX = (int) Math.round(x) - worldX;
        int offY = (int) Math.round(y) - worldY;
        g.drawRect(drawX + offX, drawY + offY, (int) width, (int) height);
    }
}