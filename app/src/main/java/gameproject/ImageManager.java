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
            // System.err.println("LỖI CHÍ MẠNG: Không tìm thấy ảnh tại đường dẫn -> " + path);
        }
    }

    public static void loadAnimation(String key, String path, int frames) {
        if (animations.containsKey(key)) return;
        File file = new File(path);
        
        if (!file.exists() && path.endsWith(".png")) {
            File altFile = new File(path + ".png");
            if (altFile.exists()) file = altFile;
        }

        if (!file.exists()) {
            // if (key.startsWith("player1")) {
            //     System.err.println("THIẾU ANIMATION: " + path);
            // }
            return;
        }
        try {
            BufferedImage sheet = ImageIO.read(file);
            int framesCount = frames;
            int w = sheet.getWidth() / framesCount;
            int h = sheet.getHeight();
            
            BufferedImage[] anim = new BufferedImage[framesCount];
            for (int i = 0; i < framesCount; i++) {
                anim[i] = sheet.getSubimage(i * w, 0, w, h);
            }
            animations.put(key, anim);
        } catch (IOException e) {
            // System.err.println("LỖI ĐỌC FILE ANIMATION: " + path);
        }
    }

    public static BufferedImage get(String name) {
        return images.get(name);
    }

    public static BufferedImage[] getAnimation(String key) {
        return animations.get(key);
    }
}