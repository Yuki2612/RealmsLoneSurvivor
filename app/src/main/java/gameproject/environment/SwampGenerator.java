package gameproject.environment;

import java.util.ArrayList;
import java.util.List;

public class SwampGenerator extends BiomeGenerator {

    @Override
    public void generate(Tile[][] grid, MapConfig config) {
        int totalTiles = rows * cols;
        int waterCount = 0;
        int attempts = 0;

        // Thử sinh cho đến khi đạt được lượng nước ổn định (tối thiểu 15% diện tích map)
        while (waterCount < totalTiles * 0.15 && attempts < 10) {
            waterCount = 0;
            // 0. Khởi tạo mặc định
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    grid[r][c].type = "ground";
                }
            }

            // 1. Khởi tạo ngẫu nhiên (Tăng lên 0.49 để ổn định hơn)
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (grid[r][c].obstacle == null && !grid[r][c].isBuildingZone) {
                        if (Math.random() < 0.49) {
                            grid[r][c].type = "water";
                        }
                    }
                }
            }

            // 2. Smoothing (8-10 lượt là đủ để ổn định)
            for (int i = 0; i < 10; i++) {
                String[][] nextState = new String[rows][cols];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int waterNeighbors = countNeighborsOfType(grid, r, c, "water");
                        if (waterNeighbors >= 5) nextState[r][c] = "water";
                        else if (waterNeighbors <= 3) nextState[r][c] = "ground";
                        else nextState[r][c] = grid[r][c].type;
                    }
                }
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (grid[r][c].obstacle == null && !grid[r][c].isBuildingZone) {
                            grid[r][c].type = (nextState[r][c] != null) ? nextState[r][c] : "ground";
                        }
                    }
                }
            }

            // Đếm số lượng nước sau khi sinh
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (grid[r][c].type.equals("water")) waterCount++;
                }
            }
            attempts++;
        }

        // 3. Xử lý Rìa nhọn (Double Spike Removal)
        for (int pass = 0; pass < 2; pass++) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (grid[r][c].type.equals("water")) {
                        // Nếu có ít hơn 2 láng giềng nước -> Xóa (Spike)
                        if (countNeighborsOfType(grid, r, c, "water") < 2) {
                            grid[r][c].type = "ground";
                        }
                    } else {
                        // Nếu là đất mà bị bao quanh bởi 7-8 ô nước -> Biến thành nước (Fill hole)
                        if (countNeighborsOfType(grid, r, c, "water") >= 7) {
                            grid[r][c].type = "water";
                        }
                    }
                }
            }
        }

        // 4. Lọc hồ nhỏ (Ngưỡng 100)
        removeSmallLakes(grid, 100);

        // 5. Vùng Spawn an toàn (Bán kính 8)
        int spawnR = rows / 2;
        int spawnC = cols / 2;
        for (int r = spawnR - 8; r <= spawnR + 8; r++) {
            for (int c = spawnC - 8; c <= spawnC + 8; c++) {
                if (r >= 0 && r < rows && c >= 0 && c < cols) grid[r][c].type = "ground";
            }
        }

        // 6. Tính toán Bitmasks
        calculateBitmasks(grid);
    }

    private void removeSmallLakes(Tile[][] grid, int minSize) {
        boolean[][] visited = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].type.equals("water") && !visited[r][c]) {
                    List<int[]> cluster = new ArrayList<>();
                    floodFill(grid, r, c, visited, cluster);
                    if (cluster.size() < minSize) {
                        for (int[] pos : cluster) {
                            grid[pos[0]][pos[1]].type = "ground";
                        }
                    }
                }
            }
        }
    }

    private void floodFill(Tile[][] grid, int r, int c, boolean[][] visited, List<int[]> cluster) {
        java.util.Stack<int[]> stack = new java.util.Stack<>();
        stack.push(new int[] { r, c });
        visited[r][c] = true;
        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            cluster.add(curr);
            int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
            for (int[] d : dirs) {
                int nr = curr[0] + d[0];
                int nc = curr[1] + d[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc].type.equals("water") && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    stack.push(new int[] { nr, nc });
                }
            }
        }
    }
}
