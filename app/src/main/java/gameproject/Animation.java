package gameproject;

import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] frames;
    private int currentFrame;
    private int numFrames;
    private int count;
    private int delay; // Tốc độ animation (số tick game loop để chuyển frame)

    public Animation(int delay) {
        this.delay = delay;
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
        if (delay <= 0 || numFrames == 0) return; 

        count++;
        if (count >= delay) {
            currentFrame++;
            count = 0;
        }

        if (currentFrame >= numFrames) {
            currentFrame = 0; // Loop lại từ đầu
        }
    }

    public BufferedImage getCurrentFrame() {
        if (frames == null || numFrames == 0) return null;
        return frames[currentFrame];
    }
    
    public int getFrameIndex() { return currentFrame; }
}
