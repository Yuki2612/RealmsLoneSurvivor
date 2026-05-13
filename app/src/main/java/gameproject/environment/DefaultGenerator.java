package gameproject.environment;

public class DefaultGenerator extends BiomeGenerator {
    @Override
    public void generate(Tile[][] grid, MapConfig config) {
        // Mặc định không làm gì thêm, chỉ giữ nguyên ground và building
    }
}
