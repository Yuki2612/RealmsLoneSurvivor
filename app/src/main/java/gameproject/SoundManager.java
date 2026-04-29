package gameproject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundManager {
    // Sử dụng Clip Pool (Danh sách các kênh phát có sẵn) để tránh giới hạn phần
    // cứng (Line Unavailable)
    private static HashMap<String, Clip[]> clipPool = new HashMap<>();
    private static HashMap<String, Integer> clipIndex = new HashMap<>();

    public static void load(String name, String path) {
        if (clipPool.containsKey(name))
            return;
        try {
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            byte[] fileBytes = out.toByteArray();
            fis.close();
            out.close();

            // Cấp phát 15 kênh (Clip) riêng cho tiếng súng, các âm thanh khác dùng 5 kênh
            int poolSize = name.equals("shoot") ? 15 : 10;
            Clip[] clips = new Clip[poolSize];
            for (int i = 0; i < poolSize; i++) {
                Clip clip = AudioSystem.getClip();
                AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(fileBytes));
                clip.open(stream);
                clips[i] = clip;
            }
            clipPool.put(name, clips);
            clipIndex.put(name, 0);
        } catch (Exception e) {
            System.err.println("LỖI: Không nạp được âm thanh " + name + " tại " + path);
        }
    }

    public static void play(String name) {
        Clip[] clips = clipPool.get(name);
        if (clips == null)
            return;

        int idx = clipIndex.get(name);
        Clip clip = clips[idx];

        // Tua về đầu và phát (những Clip đang phát ở Index khác vẫn sẽ không bị ngắt)
        clip.setFramePosition(0);
        clip.start();

        // Xoay vòng Index
        clipIndex.put(name, (idx + 1) % clips.length);
    }
}