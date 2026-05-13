package gameproject.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ParallaxBackground {
    private BufferedImage[] layers;
    private float[] xPos;
    private float[] speeds;
    private int sliceWidth;
    private int sliceHeight;
    private float scale;

    /**
     * Khởi tạo hiệu ứng Parallax
     * @param rawSheet Tấm ảnh chứa 5 lớp xếp ngang
     * @param screenWidth Chiều rộng màn hình để tính toán scale
     * @param screenHeight Chiều cao màn hình để phóng ảnh phủ kín
     */
    public ParallaxBackground(BufferedImage rawSheet, int screenWidth, int screenHeight) {
        this.layers = new BufferedImage[5];
        this.xPos = new float[5];
        
        // Tốc độ di chuyển: Lớp xa (mây) chậm, lớp gần (núi) nhanh hơn
        this.speeds = new float[]{0.15f, 0.4f, 0.8f, 1.5f, 2.8f}; 

        // Cắt ảnh thành 5 phần bằng nhau
        this.sliceWidth = rawSheet.getWidth() / 5;
        this.sliceHeight = rawSheet.getHeight();
        
        // Tính toán tỷ lệ phóng để ảnh luôn phủ kín chiều cao màn hình
        this.scale = (float) screenHeight / sliceHeight;

        for (int i = 0; i < 5; i++) {
            this.layers[i] = rawSheet.getSubimage(i * sliceWidth, 0, sliceWidth, sliceHeight);
            this.xPos[i] = 0;
        }
    }

    public void update() {
        float scaledWidth = sliceWidth * scale;
        for (int i = 0; i < 5; i++) {
            // Di chuyển sang trái
            xPos[i] -= speeds[i];
            
            // Nếu ảnh trôi hết sang trái, dịch chuyển về bên phải để lặp lại (Loop)
            if (xPos[i] <= -scaledWidth) {
                xPos[i] += scaledWidth;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        int drawW = (int) (sliceWidth * scale);
        int drawH = (int) (sliceHeight * scale);

        for (int i = 0; i < 5; i++) {
            int currentX = (int) xPos[i];
            
            // Vẽ 2 mảnh nối đuôi nhau để tạo cảm giác vô tận
            g2d.drawImage(layers[i], currentX, 0, drawW, drawH, null);
            g2d.drawImage(layers[i], currentX + drawW, 0, drawW, drawH, null);
            
            // Đề phòng màn hình siêu rộng (Ultra-wide), vẽ thêm mảnh thứ 3
            if (currentX + drawW < 2560) { // Giả sử giới hạn an toàn
                g2d.drawImage(layers[i], currentX + drawW * 2, 0, drawW, drawH, null);
            }
        }
    }
}
