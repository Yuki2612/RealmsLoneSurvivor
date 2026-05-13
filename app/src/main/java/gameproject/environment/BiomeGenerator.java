package gameproject.environment;

public abstract class BiomeGenerator {
    protected int rows, cols;

    public void init(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    public abstract void generate(Tile[][] grid, MapConfig config);

    protected int countNeighborsOfType(Tile[][] grid, int row, int col, String type) {
        int count = 0;
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (r == row && c == col) continue;
                if (r < 0 || r >= rows || c < 0 || c >= cols) count++;
                else if (grid[r][c].type.equals(type)) count++;
            }
        }
        return count;
    }

    protected boolean isGround(Tile[][] grid, int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return true; // Rìa bản đồ coi là đất để có bọt trắng
        return !grid[r][c].type.equals("water");
    }

    protected void calculateBitmasks(Tile[][] grid) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].type.equals("water")) continue;
                int mask = 0;
                boolean n = isGround(grid, r - 1, c);
                boolean s = isGround(grid, r + 1, c);
                boolean e = isGround(grid, r, c + 1);
                boolean w = isGround(grid, r, c - 1);

                if (n) mask |= 1;
                if (s) mask |= 2;
                if (e) mask |= 4;
                if (w) mask |= 8;

                if (!n && !w && isGround(grid, r - 1, c - 1)) mask |= 16;
                if (!n && !e && isGround(grid, r - 1, c + 1)) mask |= 32;
                if (!s && !w && isGround(grid, r + 1, c - 1)) mask |= 64;
                if (!s && !e && isGround(grid, r + 1, c + 1)) mask |= 128;
                
                grid[r][c].bitmask = mask;
            }
        }
    }
}
