package gameproject;

public class HeartDrop {
    public float x, y;
    public long expireTime;

    public HeartDrop(float x, float y, long expireTime) {
        this.x = x;
        this.y = y;
        this.expireTime = expireTime;
    }
}