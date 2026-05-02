package gameproject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;

public class ImageManager {
    private static HashMap<String, BufferedImage> images = new HashMap<>();
    private static HashMap<String, BufferedImage[]> animations = new HashMap<>();

    public static void load(String name, String path) {
        if (images.containsKey(name))
            return;
        try {
            BufferedImage img = ImageIO.read(new File(path));
            images.put(name, img);
        } catch (IOException e) {
            // System.err.println("LỖI CHÍ MẠNG: Không tìm thấy ảnh tại đường dẫn -> " +
            // path);
        }
    }

    /**
     * Nạp animation và TỰ ĐỘNG đọc số khung hình từ tên file (Ví dụ: player2_run_f15.png)
     */
    public static void loadAnimation(String key, String path, int defaultFrames) {
        if (animations.containsKey(key))
            return;
        File file = new File(path);

        if (!file.exists()) return;

        try {
            BufferedImage sheet = ImageIO.read(file);
            int w = sheet.getWidth();
            int h = sheet.getHeight();

            // LOGIC THÔNG MINH: Tìm số frame trong tên file
            int framesCount = defaultFrames;
            String fileName = file.getName();
            if (fileName.contains("_f")) {
                try {
                    String suffix = fileName.substring(fileName.lastIndexOf("_f") + 2);
                    if (suffix.contains(".")) suffix = suffix.substring(0, suffix.lastIndexOf("."));
                    framesCount = Integer.parseInt(suffix);
                } catch (Exception e) {
                    framesCount = defaultFrames;
                }
            }

            if (framesCount <= 0) framesCount = 1;
            int frameW = w / framesCount;

            BufferedImage[] anim = new BufferedImage[framesCount];
            for (int i = 0; i < framesCount; i++) {
                anim[i] = sheet.getSubimage(i * frameW, 0, frameW, h);
            }
            animations.put(key, anim);
        } catch (IOException e) {
            // Error handling
        }
    }

    /**
     * Hàm overload để dùng số mặc định là 10 nếu không có thông tin
     */
    public static void loadAnimation(String key, String path) {
        loadAnimation(key, path, 10);
    }

    public static BufferedImage get(String name) {
        return images.get(name);
    }

    public static BufferedImage[] getAnimation(String key) {
        return animations.get(key);
    }
}