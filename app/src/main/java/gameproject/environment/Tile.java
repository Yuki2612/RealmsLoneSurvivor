package gameproject.environment;

public class Tile {
    public Obstacle obstacle = null;
    public Obstacle decoration = null; // Cỏ, hoa, v.v. (Không va chạm, vẽ dưới chân)
    public int cost = Integer.MAX_VALUE; // Khoảng cách tới Player
    public float dirX = 0, dirY = 0;    // Hướng dòng chảy
    public String type = "ground"; // ground, water, etc.
    public int bitmask = 0;        // Dùng cho Auto-tiling (4 bits: N, S, E, W)
    public boolean isEntrance = false;
    public boolean isBuildingZone = false;
    public boolean isSolid = false;
}
