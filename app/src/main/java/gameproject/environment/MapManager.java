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
            int padding = 18;
            return !(this.col >= other.col + other.width + padding ||
                    this.col + this.width + padding <= other.col ||
                    this.row >= other.row + other.height + padding ||
                    this.row + this.height + padding <= other.row);
        }
    }

    public List<Obstacle> getObstaclesInRadius(float x, float y, float radius) {
        List<Obstacle> near = new ArrayList<>();
        int startCol = (int) ((x - radius) / TILE_SIZE);
        int endCol = (int) ((x + radius) / TILE_SIZE);
        int startRow = (int) ((y - radius) / TILE_SIZE);
        int endRow = (int) ((y + radius) / TILE_SIZE);

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

    public MapManager(int mapWidth, int mapHeight, List<Building> buildingList, MapConfig mapConfig) {
        this.cols = mapWidth / TILE_SIZE;
        this.rows = mapHeight / TILE_SIZE;
        this.grid = new Tile[rows][cols];
        this.flowField = new FlowField(cols, rows);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Tile();
            }
        }

        generateStructuredMap(buildingList, mapConfig);
    }

    public void generateStructuredMap(List<Building> buildingList, MapConfig mapConfig) {
        if (mapConfig.maxBuildings > 0) {
            generateBuildings(buildingList, mapConfig);
        }

        // generateNaturalObstacles sẽ được gọi sau generator.generate

        clearSpawnArea();

        BiomeGenerator generator;
        switch (mapConfig.type) {
            case SWAMP:
                generator = new SwampGenerator();
                break;
            default:
                generator = new DefaultGenerator();
                break;
        }

        generator.init(rows, cols);
        generator.generate(grid, mapConfig);

        // 2.5 SINH ALTAR (Ưu tiên sinh trước để chiếm chỗ trống rộng)
        if (mapConfig.type == MapType.SWAMP) {
            spawnOneAltar();
        }

        // 3. SINH VẬT THỂ TỰ NHIÊN (Cây, Đá)
        if (mapConfig.obstacleDensity > 0) {
            generateNaturalObstacles(mapConfig);
        }
    }

    private void generateBuildings(List<Building> buildingList, MapConfig mapConfig) {
        List<Room> rooms = new ArrayList<>();
        int maxBuildings = mapConfig.maxBuildings;
        int attempts = 0;

        while (buildingList.size() < maxBuildings && attempts < 300) {
            int baseW = (int) (Math.random() * 4) + 8;
            int baseH = (int) (Math.random() * 4) + 8;
            int rCol = (int) (Math.random() * (cols - 25)) + 10;
            int rRow = (int) (Math.random() * (rows - 25)) + 10;

            List<Room> buildingRooms = new ArrayList<>();
            int type = (int) (Math.random() * 4);

            if (type == 0)
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
            else if (type == 1) {
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
                buildingRooms.add(new Room(rCol + baseW - 2, rRow + baseH - 2, (int) (Math.random() * 4) + 6,
                        (int) (Math.random() * 4) + 6));
            } else if (type == 2) {
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
                int wingW = (int) (Math.random() * 4) + 6;
                buildingRooms.add(
                        new Room(rCol + baseW / 2 - wingW / 2, rRow + baseH - 2, wingW, (int) (Math.random() * 4) + 6));
            } else {
                buildingRooms.add(new Room(rCol, rRow, baseW, baseH));
                buildingRooms.add(new Room(rCol - 4, rRow + baseH - 2, 6, 8));
                buildingRooms.add(new Room(rCol + baseW - 2, rRow + baseH - 2, 6, 8));
            }

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
                int[][] buildingMask = new int[rows][cols];
                for (Room room : buildingRooms) {
                    rooms.add(room);
                    components.add(new Rectangle(room.col * TILE_SIZE, room.row * TILE_SIZE, room.width * TILE_SIZE,
                            room.height * TILE_SIZE));
                    for (int r = room.row; r < room.row + room.height; r++) {
                        for (int c = room.col; c < room.col + room.width; c++)
                            buildingMask[r][c] = 1;
                    }
                }

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
                for (int r = Math.max(0, minR - 6); r < Math.min(rows, maxR + 6); r++) {
                    for (int c = Math.max(0, minC - 6); c < Math.min(cols, maxC + 6); c++) {
                        grid[r][c].obstacle = null;
                        grid[r][c].isBuildingZone = true;
                    }
                }

                for (int r = 1; r < rows - 1; r++) {
                    for (int c = 1; c < cols - 1; c++) {
                        if (buildingMask[r][c] == 0) {
                            if (buildingMask[r - 1][c] == 1 || buildingMask[r + 1][c] == 1
                                    || buildingMask[r][c - 1] == 1 || buildingMask[r][c + 1] == 1 ||
                                    buildingMask[r - 1][c - 1] == 1 || buildingMask[r - 1][c + 1] == 1
                                    || buildingMask[r + 1][c - 1] == 1 || buildingMask[r + 1][c + 1] == 1) {
                                grid[r][c].obstacle = new Wall(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                            }
                        }
                    }
                }

                List<Building.DoorInfo> doors = placeDoors(buildingRooms, buildingMask);
                for (Building.DoorInfo d : doors) {
                    if (d.side.equals("N") || d.side.equals("S"))
                        components.add(new Rectangle(d.x, d.y, 128, 64));
                    else
                        components.add(new Rectangle(d.x, d.y, 64, 128));
                }

                placeCrates(buildingRooms);
                buildingList.add(new Building(components, doors));
            }
            attempts++;
        }
    }

    private List<Building.DoorInfo> placeDoors(List<Room> buildingRooms, int[][] buildingMask) {
        List<Building.DoorInfo> doors = new ArrayList<>();
        for (Room room : buildingRooms) {
            if (room.width >= 6) {
                int midW = room.col + (room.width - 2) / 2;
                if (isAreaClear(buildingMask, room.row - 3, midW - 1, 3, 4)) {
                    grid[room.row - 1][midW].obstacle = null;
                    grid[room.row - 1][midW + 1].obstacle = null;
                    grid[room.row - 1][midW].isEntrance = true;
                    grid[room.row - 1][midW + 1].isEntrance = true;
                    buildingMask[room.row - 1][midW] = 1;
                    buildingMask[room.row - 1][midW + 1] = 1;
                    doors.add(new Building.DoorInfo(midW * TILE_SIZE, (room.row - 1) * TILE_SIZE, "N"));
                } else if (isAreaClear(buildingMask, room.row + room.height, midW - 1, 3, 4)) {
                    grid[room.row + room.height][midW].obstacle = null;
                    grid[room.row + room.height][midW + 1].obstacle = null;
                    grid[room.row + room.height][midW].isEntrance = true;
                    grid[room.row + room.height][midW + 1].isEntrance = true;
                    buildingMask[room.row + room.height][midW] = 1;
                    buildingMask[room.row + room.height][midW + 1] = 1;
                    doors.add(new Building.DoorInfo(midW * TILE_SIZE, (room.row + room.height) * TILE_SIZE, "S"));
                }
            }
            if (room.height >= 6) {
                int midH = room.row + (room.height - 2) / 2;
                if (isAreaClear(buildingMask, midH - 1, room.col - 3, 4, 3)) {
                    grid[midH][room.col - 1].obstacle = null;
                    grid[midH + 1][room.col - 1].obstacle = null;
                    grid[midH][room.col - 1].isEntrance = true;
                    grid[midH + 1][room.col - 1].isEntrance = true;
                    buildingMask[midH][room.col - 1] = 1;
                    buildingMask[midH + 1][room.col - 1] = 1;
                    doors.add(new Building.DoorInfo((room.col - 1) * TILE_SIZE, midH * TILE_SIZE, "W"));
                } else if (isAreaClear(buildingMask, midH - 1, room.col + room.width, 4, 3)) {
                    grid[midH][room.col + room.width].obstacle = null;
                    grid[midH + 1][room.col + room.width].obstacle = null;
                    grid[midH][room.col + room.width].isEntrance = true;
                    grid[midH + 1][room.col + room.width].isEntrance = true;
                    buildingMask[midH][room.col + room.width] = 1;
                    buildingMask[midH + 1][room.col + room.width] = 1;
                    doors.add(new Building.DoorInfo((room.col + room.width) * TILE_SIZE, midH * TILE_SIZE, "E"));
                }
            }
        }
        return doors;
    }

    private void placeCrates(List<Room> buildingRooms) {
        for (Room room : buildingRooms) {
            int numCrates = (int) (Math.random() * 5) + 3;
            for (int j = 0; j < numCrates; j++) {
                int cCol = room.col + 1 + (int) (Math.random() * (room.width - 2));
                int cRow = room.row + 1 + (int) (Math.random() * (room.height - 2));
                if (grid[cRow][cCol].obstacle == null) {
                    grid[cRow][cCol].obstacle = new WoodenCrate(cCol * TILE_SIZE, cRow * TILE_SIZE, TILE_SIZE,
                            TILE_SIZE);
                }
            }
        }
    }

    private void generateNaturalObstacles(MapConfig mapConfig) {
        int mapId = mapConfig.mapId;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double roll = Math.random();

                // 1. XỬ LÝ TRANG TRÍ (Cỏ trên đất và cỏ dưới nước)
                if (mapId > 0) {
                    double grassThreshold = (mapId == 1) ? 0.15 : 0.35;
                    if (grid[r][c].type.equals("ground") && roll < grassThreshold) {
                        // Cỏ trên đất: Chỉ lấy grass1 hoặc grass2 (biến thể mapId và mapId+1)
                        int variant = mapId + (int) (Math.random() * 2);
                        String key = "grass" + variant;
                        int grassSize = 40;
                        int offX = (int) (Math.random() * (TILE_SIZE - grassSize));
                        int offY = (int) (Math.random() * (TILE_SIZE - grassSize));
                        grid[r][c].decoration = new Decoration(c * TILE_SIZE + offX, r * TILE_SIZE + offY, grassSize,
                                grassSize, key);
                    } else if (grid[r][c].type.equals("water") && roll < 0.12) {
                        // Cỏ dưới nước: Chỉ lấy grass3 (biến thể 3*mapId)
                        // Giảm mật độ xuống ~12% nhưng tăng kích thước
                        String key = "grass" + (3 * mapId);
                        int grassSize = 56;
                        int offX = (int) (Math.random() * (TILE_SIZE - grassSize));
                        int offY = (int) (Math.random() * (TILE_SIZE - grassSize));
                        grid[r][c].decoration = new Decoration(c * TILE_SIZE + offX, r * TILE_SIZE + offY, grassSize,
                                grassSize, key);
                    }
                }

                // 2. SINH VẬT CẢN (Chỉ trên đất)
                if (grid[r][c].type.equals("ground") && grid[r][c].obstacle == null && !grid[r][c].isBuildingZone) {
                    if (isNaturalObstacleNearby(r, c, 1) || isWaterNearby(r, c, 1))
                        continue;

                    if (roll < 0.02) {
                        String key = (mapId == 0) ? "rock" : getVariantKey("rock", mapId);
                        grid[r][c].obstacle = new Rock(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, key);
                        grid[r][c].isSolid = true;
                    } else if (roll < mapConfig.obstacleDensity) {
                        String key = (mapId == 0) ? "tree" : getVariantKey("tree", mapId);
                        grid[r][c].obstacle = new Tree(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, key);
                        grid[r][c].isSolid = true;
                    } else if (roll < mapConfig.obstacleDensity + 0.01) {
                        String key = (mapId == 0) ? "woodencrate" : getVariantKey("woodencrate", mapId);
                        grid[r][c].obstacle = new WoodenCrate(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, key);
                        grid[r][c].isSolid = true;
                    }
                }
            }
        }
    }

    private void spawnOneAltar() {
        java.util.List<int[]> candidates = new java.util.ArrayList<>();
        int rCenter = rows / 2;
        int cCenter = cols / 2;
        int minTileDist = 20; // Tránh người chơi ít nhất 20 ô (1280px)

        // Cố gắng tìm vùng đất trống rộng (bắt đầu từ bán kính 8, giảm dần nếu không
        // thấy)
        for (int searchRadius = 8; searchRadius >= 4; searchRadius--) {
            candidates.clear();
            for (int r = searchRadius; r < rows - searchRadius; r++) {
                for (int c = searchRadius; c < cols - searchRadius; c++) {
                    // Kiểm tra khoảng cách tới tâm (người chơi)
                    int dist = (int) Math.sqrt(Math.pow(r - rCenter, 2) + Math.pow(c - cCenter, 2));
                    if (dist < minTileDist)
                        continue;

                    if (isAreaGround(r, c, searchRadius)) {
                        candidates.add(new int[] { r, c });
                    }
                }
            }
            if (!candidates.isEmpty())
                break; // Đã tìm thấy vị trí
        }

        if (!candidates.isEmpty()) {
            int[] pos = candidates.get((int) (Math.random() * candidates.size()));
            grid[pos[0]][pos[1]].obstacle = new Altar(pos[1] * TILE_SIZE, pos[0] * TILE_SIZE, TILE_SIZE, TILE_SIZE,
                    "altar1");

            // Đảm bảo xung quanh thoáng đãng và đánh dấu Solid cho vùng Altar chiếm dụng
            int blockRadius = 4;
            for (int i = pos[0] - blockRadius; i <= pos[0] + blockRadius; i++) {
                for (int j = pos[1] - blockRadius; j <= pos[1] + blockRadius; j++) {
                    if (i >= 0 && i < rows && j >= 0 && j < cols) {
                        grid[i][j].isBuildingZone = true; // Chặn spawn tự nhiên
                        
                        // Đánh dấu solid vùng bệ đá Altar (6x6 tile quanh tâm) để AI né tránh
                        if (i >= pos[0] - 3 && i <= pos[0] + 2 && j >= pos[1] - 3 && j <= pos[1] + 2) {
                            grid[i][j].isSolid = true;
                        }
                    }
                }
            }
        }
    }

    private boolean isAreaGround(int r, int c, int radius) {
        for (int i = r - radius; i <= r + radius; i++) {
            for (int j = c - radius; j <= c + radius; j++) {
                if (i < 0 || i >= rows || j < 0 || j >= cols)
                    return false;
                if (!grid[i][j].type.equals("ground") || grid[i][j].obstacle != null || grid[i][j].isBuildingZone)
                    return false;
            }
        }
        return true;
    }

    private String getVariantKey(String base, int mapId) {
        if (mapId <= 0)
            return base;
        int min = mapId;
        int max = 3 * mapId;
        int variant = min + (int) (Math.random() * (max - min + 1));
        String key = base + variant;
        // Kiểm tra xem ảnh có tồn tại không, nếu không fallback về base + mapId
        if (gameproject.ImageManager.get(key) == null) {
            return base + mapId;
        }
        return key;
    }

    private boolean isWaterNearby(int r, int c, int radius) {
        for (int i = r - radius; i <= r + radius; i++) {
            for (int j = c - radius; j <= c + radius; j++) {
                if (i >= 0 && i < rows && j >= 0 && j < cols) {
                    if (grid[i][j].type.equals("water"))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean isNaturalObstacleNearby(int r, int c, int radius) {
        for (int i = r - radius; i <= r + radius; i++) {
            for (int j = c - radius; j <= c + radius; j++) {
                if (i >= 0 && i < rows && j >= 0 && j < cols) {
                    Obstacle obs = grid[i][j].obstacle;
                    if (obs != null && (obs instanceof Tree || obs instanceof Rock || obs instanceof WoodenCrate))
                        return true;
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
            flowField.calculate(this, playerX, playerY);
            lastPlayerTileX = pCol;
            lastPlayerTileY = pRow;
            needsRebuild = false;
        }
    }

    public void render(Graphics2D g) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j].obstacle != null)
                    grid[i][j].obstacle.render(g);
            }
        }
    }

    public void renderTiles(Graphics2D g, int camX, int camY, int sw, int sh, String mapPrefix) {
        int startCol = Math.max(0, camX / TILE_SIZE);
        int endCol = Math.min(cols - 1, (camX + sw) / TILE_SIZE + 1);
        int startRow = Math.max(0, camY / TILE_SIZE);
        int endRow = Math.min(rows - 1, (camY + sh) / TILE_SIZE + 1);

        java.awt.image.BufferedImage waterImg = gameproject.ImageManager.get(mapPrefix + "_tile_water");
        java.awt.image.BufferedImage borderImg = gameproject.ImageManager.get(mapPrefix + "_water_border");

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                int x = c * TILE_SIZE;
                int y = r * TILE_SIZE;

                // 1. Vẽ nước và bờ hồ trước
                if (grid[r][c].type.equals("water") && waterImg != null) {
                    int mask = grid[r][c].bitmask;
                    g.drawImage(waterImg, x, y, TILE_SIZE, TILE_SIZE, null);
                    if (mask > 0)
                        drawAutoTiles(g, x, y, mask, borderImg);
                }

                // 2. Vẽ trang trí đè lên trên (Cỏ, hoa...)
                // Đặt sau Water để cỏ dưới nước có thể nhìn thấy được
                if (grid[r][c].decoration != null) {
                    grid[r][c].decoration.render(g);
                }
            }
        }
    }

    private void drawAutoTiles(Graphics2D g, int x, int y, int mask, java.awt.image.BufferedImage border) {
        if (border == null)
            return;

        int w = TILE_SIZE;
        int h = TILE_SIZE;
        int imgW = border.getWidth();
        int imgH = border.getHeight();
        int f = 14; // Giảm xuống 14px để bám sát bờ hơn

        java.awt.geom.AffineTransform oldAt = g.getTransform();
        java.awt.Shape oldClip = g.getClip();

        // 1. Vẽ các biên cardinal (Dùng Clip để khóa bọt không lọt vào hồ)
        // North
        if ((mask & 1) != 0) {
            g.setClip(x, y, w, f);
            g.drawImage(border, x, y, x + w, y + f, 0, 0, imgW, f, null);
            g.setClip(oldClip);
        }
        // South
        if ((mask & 2) != 0) {
            g.setClip(x, y + h - f, w, f);
            g.drawImage(border, x, y + h - f, x + w, y + h, 0, f, imgW, 0, null);
            g.setClip(oldClip);
        }
        // East
        if ((mask & 4) != 0) {
            g.setClip(x + w - f, y, f, h);
            g.translate(x + w, y);
            g.rotate(Math.toRadians(90));
            g.drawImage(border, 0, 0, h, f, 0, 0, imgW, f, null);
            g.setTransform(oldAt);
            g.setClip(oldClip);
        }
        // West
        if ((mask & 8) != 0) {
            g.setClip(x, y, f, h);
            g.translate(x, y + h);
            g.rotate(Math.toRadians(-90));
            g.drawImage(border, 0, 0, h, f, 0, 0, imgW, f, null);
            g.setTransform(oldAt);
            g.setClip(oldClip);
        }

        // 2. Vá các góc (Inner Corners) - Cũng dùng Clip 14x14px
        // NW (16)
        if ((mask & 16) != 0) {
            g.setClip(x, y, f, f);
            g.drawImage(border, x, y, x + f, y + f, 0, 0, f, f, null);
            g.translate(x, y + f);
            g.rotate(Math.toRadians(-90));
            g.drawImage(border, 0, 0, f, f, 0, 0, f, f, null);
            g.setTransform(oldAt);
            g.setClip(oldClip);
        }
        // NE (32)
        if ((mask & 32) != 0) {
            g.setClip(x + w - f, y, f, f);
            g.drawImage(border, x + w - f, y, x + w, y + f, imgW - f, 0, imgW, f, null);
            g.translate(x + w, y);
            g.rotate(Math.toRadians(90));
            g.drawImage(border, 0, 0, f, f, 0, 0, f, f, null);
            g.setTransform(oldAt);
            g.setClip(oldClip);
        }
        // SW (64)
        if ((mask & 64) != 0) {
            g.setClip(x, y + h - f, f, f);
            g.drawImage(border, x, y + h - f, x + f, y + h, 0, f, f, 0, null);
            g.translate(x, y + h);
            g.rotate(Math.toRadians(-90));
            g.drawImage(border, 0, 0, f, f, imgW - f, 0, imgW, f, null);
            g.setTransform(oldAt);
            g.setClip(oldClip);
        }
        // SE (128)
        if ((mask & 128) != 0) {
            g.setClip(x + w - f, y + h - f, f, f);
            g.drawImage(border, x + w - f, y + h - f, x + w, y + h, imgW - f, f, imgW, 0, null);
            g.translate(x + w, y + h - f);
            g.rotate(Math.toRadians(90));
            g.drawImage(border, 0, 0, f, f, imgW - f, 0, imgW, f, null);
            g.setTransform(oldAt);
            g.setClip(oldClip);
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
        if (x < TILE_SIZE || x + w > (cols - 1) * TILE_SIZE || y < TILE_SIZE || y + h > (rows - 1) * TILE_SIZE)
            return true;
        // Tăng bán kính tìm kiếm lên TILE_SIZE * 4 để phát hiện các vật thể lớn như
        // Altar
        List<Obstacle> near = getObstaclesInRadius(x + w / 2f, y + h / 2f, TILE_SIZE * 4);
        for (Obstacle obs : near) {
            if (obs.isSolid() && obs.getHitbox() != null) {
                if (obs.getHitbox().intersects(x, y, w, h))
                    return true;
            } else if (obs.isSolid()) {
                if (x < obs.x + obs.width && x + w > obs.x && y < obs.y + obs.height && y + h > obs.y)
                    return true;
            }
        }
        return false;
    }

    public boolean isSolidGrid(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows)
            return true;
        return grid[row][col].isSolid || (grid[row][col].obstacle != null && grid[row][col].obstacle.isSolid());
    }

    public float getFlowDirX(int worldX, int worldY) {
        return flowField.getDirX(worldX, worldY);
    }

    public float getFlowDirY(int worldX, int worldY) {
        return flowField.getDirY(worldX, worldY);
    }

    public boolean isSolid(int worldX, int worldY) {
        if (worldX < TILE_SIZE || worldX > (cols - 1) * TILE_SIZE || worldY < TILE_SIZE
                || worldY > (rows - 1) * TILE_SIZE)
            return true;
        // Kiểm tra vật thể lớn trong bán kính TILE_SIZE * 4
        List<Obstacle> near = getObstaclesInRadius(worldX, worldY, TILE_SIZE * 4);
        for (Obstacle obs : near) {
            if (obs.isSolid()) {
                if (obs.getHitbox() != null) {
                    if (obs.getHitbox().contains(worldX, worldY))
                        return true;
                } else {
                    if (worldX >= obs.x && worldX < obs.x + obs.width && worldY >= obs.y && worldY < obs.y + obs.height)
                        return true;
                }
            }
        }
        return false;
    }

    public void damageObstacleAt(int worldX, int worldY, int damage) {
        Obstacle obs = getObstacleAtWorld(worldX, worldY);
        if (obs != null) {
            obs.takeDamage(damage);
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

    public String getTileTypeAtWorld(float worldX, float worldY) {
        int col = (int) (worldX / TILE_SIZE);
        int row = (int) (worldY / TILE_SIZE);
        return getTileTypeAt(row, col);
    }

    public String getTileTypeAt(int r, int c) {
        if (r >= 0 && r < rows && c >= 0 && c < cols)
            return grid[r][c].type;
        return "ground";
    }

    public Obstacle getObstacleAt(int r, int c) {
        if (r >= 0 && r < rows && c >= 0 && c < cols)
            return grid[r][c].obstacle;
        return null;
    }

    public Obstacle getObstacleAtWorld(float worldX, float worldY) {
        List<Obstacle> near = getObstaclesInRadius(worldX, worldY, TILE_SIZE * 4);
        for (Obstacle obs : near) {
            if (obs.getHitbox() != null) {
                if (obs.getHitbox().contains(worldX, worldY))
                    return obs;
            }
        }
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