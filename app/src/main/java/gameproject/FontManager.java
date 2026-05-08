package gameproject;

import java.awt.Font;
import java.io.File;

public class FontManager {
    private static Font baseFont;

    public static void load(String path) {
        try {
            File fontFile = new File(path);
            // Nếu không tìm thấy ở res/, thử tìm ở app/res/ (trường hợp chạy từ root của project đa module)
            if (!fontFile.exists() && !path.startsWith("app/")) {
                fontFile = new File("app/" + path);
            }
            
            baseFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
        } catch (Exception e) {
            System.err.println("LỖI: Không tải được Font. Sẽ dùng Arial làm dự phòng. Lỗi: " + path);
            baseFont = new Font("Arial", Font.BOLD, 20);
        }
    }

    // Hàm lấy font với kích cỡ tùy chỉnh
    public static Font getFont(float size) {
        if (baseFont != null) {
            return baseFont.deriveFont(size); // Scale font không bị vỡ
        }
        return new Font("Arial", Font.BOLD, (int) size);
    }
}