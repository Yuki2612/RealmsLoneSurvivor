package gameproject.environment;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.awt.Point;

public class FlowField {
    // CHIA NHỎ LƯỚI: Thay vì dùng ô 64x64, AI sẽ nhìn map qua các điểm 16x16
    public static final int NODE_SIZE = 16;
    private int subRows, subCols;

    private int[][] distanceField;
    private float[][] vectorX;
    private float[][] vectorY;
    public static final int MAX_COST = 1000000;

    private final int[] dRow = { -1, 1, 0, 0, -1, -1, 1, 1 };
    private final int[] dCol = { 0, 0, -1, 1, -1, 1, -1, 1 };

    public FlowField(int mapCols, int mapRows) {
        this.subCols = (mapCols * MapManager.TILE_SIZE) / NODE_SIZE;
        this.subRows = (mapRows * MapManager.TILE_SIZE) / NODE_SIZE;
        this.distanceField = new int[subRows][subCols];
        this.vectorX = new float[subRows][subCols];
        this.vectorY = new float[subRows][subCols];
    }

    // SỬA LẠI THAM SỐ: Nhận tọa độ X, Y thực tế (pixel)
    public void calculate(MapManager mapManager, float targetX, float targetY) {
        for (int r = 0; r < subRows; r++) {
            for (int c = 0; c < subCols; c++) {
                distanceField[r][c] = MAX_COST;
                vectorX[r][c] = 0;
                vectorY[r][c] = 0;
            }
        }

        // XÓA BỎ logic nhân chia TILE_SIZE cũ. Tính thẳng ra Node 16x16
        int targetNodeCol = (int) (targetX / NODE_SIZE);
        int targetNodeRow = (int) (targetY / NODE_SIZE);

        if (targetNodeCol < 0 || targetNodeCol >= subCols || targetNodeRow < 0 || targetNodeRow >= subRows)
            return;

        // BƯỚC ĐỘT PHÁ: Cache trước bản đồ vật cản dựa trên Hitbox pixel chuẩn
        boolean[][] solidNodes = new boolean[subRows][subCols];
        for (int r = 0; r < subRows; r++) {
            for (int c = 0; c < subCols; c++) {
                solidNodes[r][c] = isNodeSolid(mapManager, c, r);
            }
        }

        // BFS Loang khoảng cách trên lưới độ phân giải cao
        Queue<Point> queue = new LinkedList<>();
        // Đảm bảo node đích luôn có thể đi vào được để AI không bị kẹt khi người chơi đứng sát vật thể
        solidNodes[targetNodeRow][targetNodeCol] = false; 
        distanceField[targetNodeRow][targetNodeCol] = 0;
        queue.add(new Point(targetNodeCol, targetNodeRow));

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int r = p.y;
            int c = p.x;

            for (int i = 0; i < 8; i++) {
                int nr = r + dRow[i];
                int nc = c + dCol[i];

                if (nr >= 0 && nr < subRows && nc >= 0 && nc < subCols) {
                    // Penalty cho vật cản: Giảm xuống 15 để AI cân bằng giữa né tường và áp sát người chơi
                    int moveCost = (solidNodes[nr][nc]) ? 15 : 1;
                    int newCost = distanceField[r][c] + moveCost;

                    if (newCost < distanceField[nr][nc]) {
                        // Chặn cắt góc qua tường chéo nếu node bên cạnh là tường
                        if (i >= 4) {
                            if (solidNodes[r][nc] || solidNodes[nr][c])
                                continue;
                        }
                        distanceField[nr][nc] = newCost;
                        queue.add(new Point(nc, nr));
                    }
                }
            }
        }

        // Tạo Vector chỉ hướng (Gradient Descent)
        for (int r = 0; r < subRows; r++) {
            for (int c = 0; c < subCols; c++) {
                if (distanceField[r][c] == MAX_COST || distanceField[r][c] == 0)
                    continue;

                float vx = 0, vy = 0;
                int currentCost = distanceField[r][c];

                for (int i = 0; i < 8; i++) {
                    int nr = r + dRow[i];
                    int nc = c + dCol[i];

                    int neighborCost;
                    if (nr < 0 || nr >= subRows || nc < 0 || nc >= subCols) {
                        neighborCost = currentCost + 20;
                    } else {
                        neighborCost = distanceField[nr][nc];
                    }
                    
                    // Vector hướng về phía có cost thấp hơn
                    if (neighborCost < currentCost) {
                        vx += dCol[i] * (currentCost - neighborCost);
                        vy += dRow[i] * (currentCost - neighborCost);
                    }
                }

                float len = (float) Math.sqrt(vx * vx + vy * vy);
                if (len > 0) {
                    vectorX[r][c] = vx / len;
                    vectorY[r][c] = vy / len;
                }
            }
        }
    }

    /**
     * Helper quét xem Node 16x16 này có đè lên Hitbox thực tế nào không.
     */
    private boolean isNodeSolid(MapManager map, int subCol, int subRow) {
        float cx = subCol * NODE_SIZE + NODE_SIZE / 2.0f;
        float cy = subRow * NODE_SIZE + NODE_SIZE / 2.0f;
        
        // KIỂM TRA NHANH: Nếu ô lưới chứa node này được đánh dấu là Solid, thì node đó Solid
        if (map.isSolidGrid((int)(cx / MapManager.TILE_SIZE), (int)(cy / MapManager.TILE_SIZE))) {
            return true;
        }

        // Dùng thuật toán Broad-phase O(1) để lấy danh sách Hitbox gần đó
        List<Obstacle> near = map.getObstaclesInRadius(cx, cy, MapManager.TILE_SIZE);
        for (Obstacle obs : near) {
            if (obs.getHitbox() != null && obs.getHitbox().contains(cx, cy)) {
                return true; // Chính xác tới từng pixel!
            }
        }
        return false;
    }

    // ĐÃ ĐỔI: Phương thức này giờ nhận thẳng tọa độ THẾ GIỚI (World X, Y)
    // AI không cần tự chia cho TILE_SIZE nữa
    public float getDirX(float worldX, float worldY) {
        int c = (int) (worldX / NODE_SIZE);
        int r = (int) (worldY / NODE_SIZE);
        if (c >= 0 && c < subCols && r >= 0 && r < subRows)
            return vectorX[r][c];
        return 0;
    }

    public float getDirY(float worldX, float worldY) {
        int c = (int) (worldX / NODE_SIZE);
        int r = (int) (worldY / NODE_SIZE);
        if (c >= 0 && c < subCols && r >= 0 && r < subRows)
            return vectorY[r][c];
        return 0;
    }
}