package gameproject;

import java.io.File;
import java.util.HashMap;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundManager {
    // Dùng HashMap để lưu trữ âm thanh trên RAM, gọi ra bằng tên (key)
    private static HashMap<String, Clip> clips = new HashMap<>();

    // Hàm nạp âm thanh vào bộ nhớ
    public static void load(String name, String path) {
        if (clips.containsKey(name))
            return;
        try {
            File file = new File(path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clips.put(name, clip);
        } catch (Exception e) {
            System.err.println("LỖI: Không tìm thấy hoặc không đọc được file âm thanh tại: " + path);
        }
    }

    // Hàm phát âm thanh
    public static void play(String name) {
        Clip clip = clips.get(name);
        if (clip == null)
            return; // Tránh crash nếu chưa nạp file

        // Đưa âm thanh về vạch xuất phát và phát lại
        clip.setFramePosition(0);
        clip.start();
    }
}