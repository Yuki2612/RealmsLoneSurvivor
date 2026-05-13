package gameproject.environment;

import java.awt.Graphics2D;

public abstract class Obstacle implements gameproject.Renderable {
    public int x, y;
    protected int width, height;
    protected Hitbox hitbox; // ĐÃ ĐỔI: Sử dụng interface Hitbox

    public int getX() { return x; }
    public int getY() { return y; }

    public Obstacle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // KHÔNG khởi tạo hitbox ở đây
    }

    public abstract boolean isSolid();

    public abstract void takeDamage(int dmg);

    public abstract boolean isDestroyed();

    @Override
    public abstract void render(Graphics2D g);

    @Override
    public float getBottomY() {
        return y + height;
    }

    public Hitbox getHitbox() {
        return hitbox;
    }
}