package gameproject.environment;

import java.awt.Graphics;

public interface Hitbox {
    /** Kiểm tra một điểm (px, py) có nằm trong vùng va chạm không */
    boolean contains(float px, float py);
    boolean intersects(float x, float y, float w, float h);
    void draw(Graphics g);
    void drawAbsolute(Graphics g, int drawX, int drawY, int worldX, int worldY);
}