package gameproject.entity;

import java.awt.Rectangle;

public class ChestDrop {
    public float x, y;
    public boolean isRare;
    public long expirationTime;

    public ChestDrop(float x, float y, boolean isRare, long expirationTime) {
        this.x = x;
        this.y = y;
        this.isRare = isRare;
        this.expirationTime = expirationTime;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 40, 40);
    }
}
