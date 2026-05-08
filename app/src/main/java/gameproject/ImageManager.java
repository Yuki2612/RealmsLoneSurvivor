package gameproject;

import java.awt.Graphics2D;
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
            File file = new File(path);
            if (!file.exists() && !path.startsWith("app/")) {
                file = new File("app/" + path);
            }
            BufferedImage img = ImageIO.read(file);
            images.put(name, img);
        } catch (IOException e) {
            // System.err.println("LỖI CHÍ MẠNG: Không tìm thấy ảnh tại đường dẫn -> " +
            // path);
        }
    }

    /**
     * Nạp animation đơn giản (Tự động nhận diện _f)
     */
    public static void loadAnimation(String key, String path) {
        loadAnimation(key, path, 1);
    }

    public static void loadAnimation(String key, String path, int defaultFrames) {
        if (animations.containsKey(key))
            return;
        File file = new File(path);
        if (!file.exists() && !path.startsWith("app/")) {
            file = new File("app/" + path);
        }

        // LOGIC THÔNG MINH: Nếu không thấy file chính xác, thử tìm file có hậu tố _f hoặc tên tương tự (không phân biệt hoa thường)
        if (!file.exists()) {
            String dirPath = file.getParent();
            if (dirPath == null) {
                dirPath = "app/res";
            }
            File directory = new File(dirPath);
            
            if (directory.exists() && directory.isDirectory()) {
                String nameOnly = file.getName().toLowerCase();
                if (nameOnly.contains("."))
                    nameOnly = nameOnly.substring(0, nameOnly.lastIndexOf("."));
                
                final String searchPrefix = nameOnly + "_f";
                File[] matchingFiles = directory.listFiles((d, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.startsWith(searchPrefix) && lowerName.endsWith(".png");
                });

                if (matchingFiles != null && matchingFiles.length > 0) {
                    file = matchingFiles[0];
                }
            }
        }

        if (!file.exists())
            return;

        try {
            BufferedImage sheet = ImageIO.read(file);
            int w = sheet.getWidth();
            int h = sheet.getHeight();

            int framesCount = defaultFrames;
            String fileName = file.getName().toLowerCase();
            
            // Tìm vị trí chữ 'f' cuối cùng (để tránh nhầm với 'f' trong tên file khác)
            int fIndex = fileName.lastIndexOf("f");
            if (fIndex != -1) {
                try {
                    // Trích xuất chuỗi số ngay sau chữ 'f'
                    StringBuilder numPart = new StringBuilder();
                    for (int i = fIndex + 1; i < fileName.length(); i++) {
                        char c = fileName.charAt(i);
                        if (Character.isDigit(c)) {
                            numPart.append(c);
                        } else {
                            break;
                        }
                    }
                    if (numPart.length() > 0) {
                        framesCount = Integer.parseInt(numPart.toString());
                    }
                } catch (Exception e) {
                    framesCount = defaultFrames;
                }
            }

            if (framesCount <= 0)
                framesCount = 1;

            BufferedImage[] rawFrames = new BufferedImage[framesCount];
            int[] frameMinX = new int[framesCount];
            int[] frameMaxX = new int[framesCount];
            int globalMinY = h, globalMaxY = 0;
            int maxContentW = 0;
            boolean anyPixelFound = false;

            // Bước 1: Phân tích từng khung hình
            for (int i = 0; i < framesCount; i++) {
                int xStart = i * w / framesCount;
                int xEnd = (i + 1) * w / framesCount;
                int currentFrameW = xEnd - xStart;
                rawFrames[i] = sheet.getSubimage(xStart, 0, currentFrameW, h);

                int fMinX = currentFrameW, fMaxX = 0;
                boolean frameHasPixel = false;

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < currentFrameW; x++) {
                        if (((rawFrames[i].getRGB(x, y) >> 24) & 0xff) > 10) {
                            if (x < fMinX) fMinX = x;
                            if (x > fMaxX) fMaxX = x;
                            if (y < globalMinY) globalMinY = y;
                            if (y > globalMaxY) globalMaxY = y;
                            frameHasPixel = true;
                            anyPixelFound = true;
                        }
                    }
                }
                frameMinX[i] = fMinX;
                frameMaxX[i] = fMaxX;
                if (frameHasPixel) {
                    maxContentW = Math.max(maxContentW, fMaxX - fMinX + 1);
                }
            }

            // Bước 2: Tạo mảng ảnh đã được căn giữa
            BufferedImage[] anim = new BufferedImage[framesCount];
            if (anyPixelFound) {
                int finalW = maxContentW + 4; // Thêm chút padding
                int finalH = globalMaxY - globalMinY + 5;
                globalMinY = Math.max(0, globalMinY - 2);

                for (int i = 0; i < framesCount; i++) {
                    BufferedImage centeredFrame = new BufferedImage(finalW, finalH, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = centeredFrame.createGraphics();
                    
                    if (frameMaxX[i] >= frameMinX[i]) {
                        int contentW = frameMaxX[i] - frameMinX[i] + 1;
                        int destX = (finalW - contentW) / 2; // Căn giữa chiều ngang
                        int sourceX = frameMinX[i];
                        int sourceY = globalMinY;
                        
                        g2.drawImage(rawFrames[i], 
                            destX, 0, destX + contentW, finalH,
                            sourceX, sourceY, sourceX + contentW, sourceY + finalH, 
                            null);
                    }
                    g2.dispose();
                    anim[i] = centeredFrame;
                }
            } else {
                anim = rawFrames;
            }

            animations.put(key, anim);
        } catch (IOException e) {
            // Error
        }
    }

    public static BufferedImage get(String name) {
        return images.get(name);
    }

    public static BufferedImage[] getAnimation(String key) {
        return animations.get(key);
    }
}