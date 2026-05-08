package gameproject;

import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] frames;
    private int currentFrame;
    private int numFrames;
    private int count;
    private int delay; // Tốc độ animation (số tick game loop để chuyển frame)
    private boolean looping = true; // Mặc định là lặp lại

    public Animation(int delay) {
        this.delay = delay;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setFrames(BufferedImage[] frames) {
        this.frames = frames;
        this.currentFrame = 0;
        this.count = 0;
        if (frames != null) {
            this.numFrames = frames.length;
        } else {
            this.numFrames = 0;
        }
    }

    public void update() {
        if (delay <= 0 || numFrames == 0)
            return;

        count++;
        if (count >= delay) {
            count = 0;
            currentFrame++;
            if (currentFrame >= numFrames) {
                if (looping) {
                    currentFrame = 0;
                } else {
                    currentFrame = numFrames - 1; // Dừng ở khung cuối
                }
            }
        }
    }

    public BufferedImage getCurrentFrame() {
        if (frames == null || numFrames == 0)
            return null;
        return frames[currentFrame];
    }

    public BufferedImage[] getFrames() {
        return frames;
    }

    public void reset() {
        this.currentFrame = 0;
        this.count = 0;
    }

    public int getFrameIndex() {
        return currentFrame;
    }

    public int getTotalFrames() {
        return numFrames;
    }

    /**
     * Kiểm tra xem animation đã chạy xong toàn bộ số tick của khung hình cuối cùng
     * chưa.
     */
    public boolean hasFinishedCycle() {
        if (numFrames == 0)
            return true;
        return (currentFrame == numFrames - 1) && (count >= delay - 1);
    }
}
