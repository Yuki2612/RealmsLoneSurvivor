package gameproject.environment;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class MapManager {
    public static final int TILE_SIZE = 64;
    private Tile[][] grid;
    private int rows, cols;

    private int lastPlayerTileX = -1, lastPlayerTileY = -1;
    private FlowField flowField;
    private boolean needsRebuild = true;

    private static class Room {
        int col, row, width, height;

        public Room(int col, int row, int width, int height) {
            this.col = col;
            this.row = row;
            this.width = width;
            this.height = height;
        }

        public boolean intersects(Room other) {
            int padding = 18; // Tăng từ 10 lên 18 để nhà thưa hơn
            return !(this.col >= other.col + other.width + padding ||
                    this.col + this.width + padding <= other.col ||
                    this.row >= other.row + other.height + padding ||
                    this.row + this.height + padding <= other.row);
        }
    }

    /**
     * Thuật toán Broad-phase: Chỉ lấy ra các vật cản trong vùng lân cận (Bounding
     * Box của bán kính).
     * Duy trì hiệu năng O(1) bằng cách giới hạn phạm vi quét ô lưới từ 4-9 ô.
     */
    public List<Obstacle> getObstaclesInRadius(float x, float y, float radius) {
        List<Obstacle> near = new ArrayList<>();

        // Tính toán giới hạn các ô lưới cần quét
        int startCol = (int) ((x - radius) / TILE_SIZE);
        int endCol = (int) ((x + radius) / TILE_SIZE);
        int startRow = (int) ((y - radius) / TILE_SIZE);
        int endRow = (int) ((y + radius) / TILE_SIZE);

        // Đảm bảo không vượt quá kích thước map (Boundary check)
        startCol = Math.max(0, startCol);
        endCol = Math.min(cols - 1, endCol);
        startRow = Math.max(0, startRow);
        endRow = Math.min(rows - 1, endRow);

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                Obstacle obs = grid[r][c].obstacle;
                if (obs != null && obs.isSolid()) {
                    near.add(obs);
                }
            }
        }
        return near;
    }

    public MapManager(int mapWidth, int mapHeight, List<Building> buildingList) {
        this.cols = mapWidth / TILE_SIZE;
        this.rows = mapHeight / TILE_SIZE;
        this.grid = new Tile[rows][cols];
        this.flowField = new FlowField(cols, rows);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Tile();
            }
        }

        generateStructuredMap(buildingList);
    }

    public void generateStructuredMap(List<Building> buildingList) {
        List<Room> rooms = new ArrayList<>();
        int maxBuildings = 18; // Giảm từ 25 xuống 18 để bản đồ thoáng hơn
        int attempts = 0;

        while (buildingList.size() < maxBuildings && attempts < 300) {
            int baseW = (int) (Math.random() * 4) + 8;
            int baseH = (int) (Math.random() * 4) + 8;
            int rCol = (int) (Math.random() * (cols - 25)) + 10;
            int rRow = (int) (Math.random() * (rows - 25)) + 10;

            List<Room> buildingRooms = new ArrayList<>();
            int type = (int) (Math.random() * 4); // 0: Rect, 1: L, 2: T, 3: U

            if (type == 0) { // Rectangle
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
            } else if (type == 1) { // L-Shape
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
                buildingRooms.add(new Room(rCol + baseW - 2, rRow + baseH - 2, (int) (Math.random() * 4) + 6,
                        (int) (Math.random() * 4) + 6));
            } else if (type == 2) { // T-Shape
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
                int wingW = (int) (Math.random() * 4) + 6;
                buildingRooms.add(
                        new Room(rCol + baseW / 2 - wingW / 2, rRow + baseH - 2, wingW, (int) (Math.random() * 4) + 6));
            } else { // U-Shape
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
                buildingRooms.add(new Room(rCol - 4, rRow + baseH - 2, 6, 8)); // Wing 1
                buildingRooms.add(new Room(rCol + baseW - 2, rRow + baseH - 2, 6, 8)); // Wing 2
            }

            // Kiểm tra va chạm với các tòa nhà khác VÀ kiểm tra biên bản đồ
            boolean overlapping = false;
            for (Room br : buildingRooms) {
                if (br.col < 2 || br.row < 2 || br.col + br.width >= cols - 2 || br.row + br.height >= rows - 2) {
                    overlapping = true;
                    break;
                }
                for (Room r : rooms) {
                    if (br.intersects(r)) {
                        overlapping = true;
                        break;
                    }
                }
                if (overlapping)
                    break;
            }

            if (!overlapping) {
                List<Rectangle> components = new ArrayList<>();
                // BƯỚC 1: ĐỔ MÓNG (Boolean Masking)
                int[][] buildingMask = new int[rows][cols];
                for (Room room : buildingRooms) {
                    rooms.add(room);
                    components.add(new Rectangle(room.col * TILE_SIZE, room.row * TILE_SIZE,
                            room.width * TILE_SIZE, room.height * TILE_SIZE));
                    for (int r = room.row; r < room.row + room.height; r++) {
                        for (int c = room.col; c < room.col + room.width; c++) {
                            buildingMask[r][c] = 1; // Đánh dấu sàn nhà
                        }
                    }
                }

                // Tính toán giới hạn (Bounds) của toàn bộ khối nhà để xóa vật thể xung quanh
                int minR = rows, maxR = 0, minC = cols, maxC = 0;
                for (Room room : buildingRooms) {
                    if (room.row < minR)
                        minR = room.row;
                    if (room.row + room.height > maxR)
                        maxR = room.row + room.height;
                    if (room.col < minC)
                        minC = room.col;
                    if (room.col + room.width > maxC)
                        maxC = room.col + room.width;
                }

                // BƯỚC 2: XÓA VẬT THỂ XUNG QUANH (Clearance Zone 6 ô để nhà thoáng hơn)
                int clearance = 6;
                for (int r = Math.max(0, minR - clearance); r < Math.min(rows, maxR + clearance); r++) {
                    for (int c = Math.max(0, minC - clearance); c < Math.min(cols, maxC + clearance); c++) {
                        grid[r][c].obstacle = null;
                        grid[r][c].isBuildingZone = true;
                    }
                }

                // BƯỚC 3: TRÍCH XUẤT CHU VI & XÂY TƯỜNG
                for (int r = 1; r < rows - 1; r++) {
                    for (int c = 1; c < cols - 1; c++) {
                        if (buildingMask[r][c] == 1) {
                            // Đây là sàn nhà, đảm bảo không có vật cản bên trong
                            grid[r][c].obstacle = null;
                        } else if (buildingMask[r][c] == 0) {
                            // Kiểm tra xem có kề cạnh sàn nhà không (Boolean Union Boundary)
                            if (buildingMask[r - 1][c] == 1 || buildingMask[r + 1][c] == 1 ||
                                    buildingMask[r][c - 1] == 1 || buildingMask[r][c + 1] == 1 ||
                                    buildingMask[r - 1][c - 1] == 1 || buildingMask[r - 1][c + 1] == 1 ||
                                    buildingMask[r + 1][c - 1] == 1 || buildingMask[r + 1][c + 1] == 1) {

                                grid[r][c].obstacle = new Wall(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                            }
                        }
                    }
                }

                // BƯỚC 3: ĐẶT CỬA THÔNG MINH (Với kiểm tra vùng an toàn mở rộng)
                List<Building.DoorInfo> buildingDoors = new ArrayList<>();
                for (Room room : buildingRooms) {
                    if (room.width >= 6) {
                        int midW = room.col + (room.width - 2) / 2;
                        // Cửa Trên: Kiểm tra vùng 4x3 phía trên (bao gồm cả lề để tránh chạm tường bên)
                        if (isAreaClear(buildingMask, room.row - 3, midW - 1, 3, 4)) {
                            grid[room.row - 1][midW].obstacle = null;
                            grid[room.row - 1][midW + 1].obstacle = null;
                            grid[room.row - 1][midW].isEntrance = true;
                            grid[room.row - 1][midW + 1].isEntrance = true;
                            buildingMask[room.row - 1][midW] = 1;
                            buildingMask[room.row - 1][midW + 1] = 1;
                            buildingDoors.add(new Building.DoorInfo(midW * TILE_SIZE, (room.row - 1) * TILE_SIZE, "N"));
                        }
                        // Cửa Dưới: Kiểm tra vùng 4x3 phía dưới
                        else if (isAreaClear(buildingMask, room.row + room.height, midW - 1, 3, 4)) {
                            grid[room.row + room.height][midW].obstacle = null;
                            grid[room.row + room.height][midW + 1].obstacle = null;
                            grid[room.row + room.height][midW].isEntrance = true;
                            grid[room.row + room.height][midW + 1].isEntrance = true;
                            buildingMask[room.row + room.height][midW] = 1;
                            buildingMask[room.row + room.height][midW + 1] = 1;
                            buildingDoors.add(
                                    new Building.DoorInfo(midW * TILE_SIZE, (room.row + room.height) * TILE_SIZE, "S"));
                        }
                    }

                    if (room.height >= 6) {
                        int midH = room.row + (room.height - 2) / 2;
                        // Cửa Trái: Kiểm tra vùng 3x4 bên trái
                        if (isAreaClear(buildingMask, midH - 1, room.col - 3, 4, 3)) {
                            grid[midH][room.col - 1].obstacle = null;
                            grid[midH + 1][room.col - 1].obstacle = null;
                            grid[midH][room.col - 1].isEntrance = true;
                            grid[midH + 1][room.col - 1].isEntrance = true;
                            buildingMask[midH][room.col - 1] = 1;
                            buildingMask[midH + 1][room.col - 1] = 1;
                            buildingDoors.add(new Building.DoorInfo((room.col - 1) * TILE_SIZE, midH * TILE_SIZE, "W"));
                        }
                        // Cửa Phải: Kiểm tra vùng 3x4 bên phải
                        else if (isAreaClear(buildingMask, midH - 1, room.col + room.width, 4, 3)) {
                            grid[midH][room.col + room.width].obstacle = null;
                            grid[midH + 1][room.col + room.width].obstacle = null;
                            grid[midH][room.col + room.width].isEntrance = true;
                            grid[midH + 1][room.col + room.width].isEntrance = true;
                            buildingMask[midH][room.col + room.width] = 1;
                            buildingMask[midH + 1][room.col + room.width] = 1;
                            buildingDoors.add(
                                    new Building.DoorInfo((room.col + room.width) * TILE_SIZE, midH * TILE_SIZE, "E"));
                        }
                    }
                }

                // CẬP NHẬT COMPONENTS (Bao gồm ô cửa)
                for (Building.DoorInfo d : buildingDoors) {
                    if (d.side.equals("N") || d.side.equals("S"))
                        components.add(new Rectangle(d.x, d.y, 128, 64));
                    else
                        components.add(new Rectangle(d.x, d.y, 64, 128));
                }

                // THÊM THÙNG GỖ VÀO BÊN TRONG (Tăng số lượng lên 3-7 thùng)
                for (Room room : buildingRooms) {
                    int numCrates = (int) (Math.random() * 5) + 3;
                    for (int j = 0; j < numCrates; j++) {
                        int crateCol = room.col + 1 + (int) (Math.random() * (room.width - 2));
                        int crateRow = room.row + 1 + (int) (Math.random() * (room.height - 2));
                        if (grid[crateRow][crateCol].obstacle == null) {
                            grid[crateRow][crateCol].obstacle = new WoodenCrate(crateCol * TILE_SIZE,
                                    crateRow * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    }
                }

                buildingList.add(new Building(components, buildingDoors));
            }
            attempts++;
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // CHỈ SINH VẬT THỂ NẾU NẰM NGOÀI VÙNG ĐỆM CỦA NHÀ (isBuildingZone)
                if (grid[r][c].obstacle == null && !grid[r][c].isBuildingZone) {
                    // Kiểm tra khoảng cách với các vật thể tự nhiên khác (bán kính 1 ô)
                    if (isNaturalObstacleNearby(r, c, 1)) continue;

                    double roll = Math.random();
                    if (roll < 0.02)
                        grid[r][c].obstacle = new Rock(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    else if (roll < 0.05)
                        grid[r][c].obstacle = new Tree(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    else if (roll < 0.06)
                        grid[r][c].obstacle = new WoodenCrate(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        clearSpawnArea();
    }

    private boolean isNaturalObstacleNearby(int r, int c, int radius) {
        for (int i = r - radius; i <= r + radius; i++) {
            for (int j = c - radius; j <= c + radius; j++) {
                if (i >= 0 && i < rows && j >= 0 && j < cols) {
                    Obstacle obs = grid[i][j].obstacle;
                    if (obs != null && (obs instanceof Tree || obs instanceof Rock || obs instanceof WoodenCrate)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void clearSpawnArea() {
        int centerC = (gameproject.GamePanel.WORLD_WIDTH / 2) / TILE_SIZE;
        int centerR = (gameproject.GamePanel.WORLD_HEIGHT / 2) / TILE_SIZE;
        for (int r = centerR - 3; r <= centerR + 3; r++) {
            for (int c = centerC - 3; c <= centerC + 3; c++) {
                if (r >= 0 && r < rows && c >= 0 && c < cols)
                    grid[r][c].obstacle = null;
            }
        }
    }

    public void update(int playerX, int playerY) {
        boolean obstacleDestroyed = false;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j].obstacle != null && grid[i][j].obstacle.isDestroyed()) {
                    grid[i][j].obstacle = null;
                    obstacleDestroyed = true;
                    gameproject.meta.AchievementManager.getInstance().addDestruction();
                }
            }
        }

        int pCol = playerX / TILE_SIZE;
        int pRow = playerY / TILE_SIZE;

        if (obstacleDestroyed || pCol != lastPlayerTileX || pRow != lastPlayerTileY || needsRebuild) {
            // SỬA TẠI ĐÂY: Truyền thẳng tọa độ pixel (playerX, playerY)
            flowField.calculate(this, playerX, playerY);
            lastPlayerTileX = pCol;
            lastPlayerTileY = pRow;
            needsRebuild = false;
        }
    }

    public void render(Graphics2D g) {
        // Hiện tại chỉ vẽ vật cản. Tile nền được vẽ bởi PlayingState
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j].obstacle != null)
                    grid[i][j].obstacle.render(g);
            }
        }
    }

    public List<Obstacle> getAllObstacles() {
        List<Obstacle> list = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j].obstacle != null)
                    list.add(grid[i][j].obstacle);
            }
        }
        return list;
    }

    public boolean isColliding(float x, float y, float w, float h) {
        // LỚP CHẮN TÀNG HÌNH: Chặn mọi thực thể lọt ra ngoài phạm vi 1 ô ở rìa map
        if (x < TILE_SIZE || x + w > (cols - 1) * TILE_SIZE || 
            y < TILE_SIZE || y + h > (rows - 1) * TILE_SIZE) {
            return true;
        }

        // Kiểm tra va chạm dựa trên Hitbox thực tế thay vì ô lưới thô
        List<Obstacle> near = getObstaclesInRadius(x + w / 2f, y + h / 2f, TILE_SIZE);
        for (Obstacle obs : near) {
            if (obs.isSolid() && obs.getHitbox() != null) {
                if (obs.getHitbox().intersects(x, y, w, h)) {
                    return true;
                }
            } else if (obs.isSolid()) {
                if (x < obs.x + obs.width && x + w > obs.x && y < obs.y + obs.height && y + h > obs.y) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSolidGrid(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows)
            return true;
        return grid[row][col].obstacle != null && grid[row][col].obstacle.isSolid();
    }

    public float getFlowDirX(int worldX, int worldY) {
        // Truyền thẳng tọa độ thực tế, FlowField sẽ tự quy đổi ra Node 16x16
        return flowField.getDirX(worldX, worldY);
    }

    public float getFlowDirY(int worldX, int worldY) {
        return flowField.getDirY(worldX, worldY);
    }

    public boolean isSolid(int worldX, int worldY) {
        // LỚP CHẮN TÀNG HÌNH: Biên 1 ô lưới
        if (worldX < TILE_SIZE || worldX > (cols - 1) * TILE_SIZE || 
            worldY < TILE_SIZE || worldY > (rows - 1) * TILE_SIZE) {
            return true;
        }

        // CHỐNG "LỖI TÁN LÁ" VÀ "LỖI LỆCH GỐC": Quét các vật thể lân cận
        List<Obstacle> near = getObstaclesInRadius(worldX, worldY, TILE_SIZE);
        for (Obstacle obs : near) {
            if (obs.isSolid()) {
                if (obs.getHitbox() != null) {
                    if (obs.getHitbox().contains(worldX, worldY))
                        return true;
                } else {
                    // Tường/Vật cản thô: check theo bounds ô lưới
                    if (worldX >= obs.x && worldX < obs.x + obs.width &&
                            worldY >= obs.y && worldY < obs.y + obs.height)
                        return true;
                }
            }
        }
        return false;
    }

    public void damageObstacleAt(int worldX, int worldY, int damage) {
        int col = worldX / TILE_SIZE;
        int row = worldY / TILE_SIZE;
        if (col >= 0 && col < cols && row >= 0 && row < rows && grid[row][col].obstacle != null) {
            grid[row][col].obstacle.takeDamage(damage);
        }
    }

    public boolean isNavigable(int worldX, int worldY) {
        int col = worldX / TILE_SIZE;
        int row = worldY / TILE_SIZE;
        if (col < 0 || col >= cols || row < 0 || row >= rows)
            return false;
        return grid[row][col].obstacle == null
                && (flowField.getDirX(col, row) != 0 || flowField.getDirY(col, row) != 0);
    }

    public boolean isEntrance(int worldX, int worldY) {
        int col = worldX / TILE_SIZE;
        int row = worldY / TILE_SIZE;
        if (col < 0 || col >= cols || row < 0 || row >= rows)
            return false;
        return grid[row][col].isEntrance;
    }

    public Obstacle getObstacleAt(int r, int c) {
        if (r >= 0 && r < rows && c >= 0 && c < cols)
            return grid[r][c].obstacle;
        return null;
    }

    public Obstacle getObstacleAtWorld(float worldX, float worldY) {
        int col = (int) (worldX / TILE_SIZE);
        int row = (int) (worldY / TILE_SIZE);
        return getObstacleAt(row, col);
    }

    private boolean isAreaClear(int[][] mask, int startR, int startC, int h, int w) {
        for (int r = startR; r < startR + h; r++) {
            for (int c = startC; c < startC + w; c++) {
                if (r < 0 || r >= rows || c < 0 || c >= cols)
                    return false;
                if (mask[r][c] != 0)
                    return false;
            }
        }
        return true;
    }
}