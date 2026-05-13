package gameproject.entity;

public class RuneItem {
    public float x, y;
    public long expireTime;
    public boolean collected = false;

    public RuneItem(float x, float y, long expireTime) {
        this.x = x;
        this.y = y;
        this.expireTime = expireTime;
    }
}
