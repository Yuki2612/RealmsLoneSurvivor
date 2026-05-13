package gameproject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class SoundManager {
    private static HashMap<String, Clip[]> clipPool = new HashMap<>();
    private static HashMap<String, Integer> clipIndex = new HashMap<>();
    private static HashMap<String, Clip> musicPool = new HashMap<>();
    private static String currentMusicName = null;

    private static float sfxVolume = 0.8f;
    private static float musicVolume = 0.6f;

    // Cờ để tránh xung đột giữa fade thủ công và loop tự động
    private static boolean isManualFading = false;

    public static void load(String name, String path) {
        if (clipPool.containsKey(name))
            return;
        try {
            byte[] fileBytes = readFileBytes(path);
            if (fileBytes == null)
                return;

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
        }
    }

    public static void loadMusic(String name, String path) {
        if (musicPool.containsKey(name))
            return;
        try {
            byte[] fileBytes = readFileBytes(path);
            if (fileBytes == null)
                return;

            Clip clip = AudioSystem.getClip();
            AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(fileBytes));
            clip.open(stream);
            musicPool.put(name, clip);
        } catch (Exception e) {
        }
    }

    private static byte[] readFileBytes(String path) {
        try {
            File file = new File(path);
            if (!file.exists() && !path.startsWith("app/")) {
                file = new File("app/" + path);
            }
            if (!file.exists())
                return null;
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            byte[] bytes = out.toByteArray();
            fis.close();
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }

    public static void play(String name) {
        play(name, 1.0f);
    }

    public static void play(String name, float volMulti) {
        Clip[] clips = clipPool.get(name);
        if (clips == null)
            return;

        // Giảm âm lượng mặc định cho hiệu ứng nổ (explosion) để bớt chói tai
        if (name.equals("explosion")) {
            volMulti *= 0.6f;
        }

        int idx = clipIndex.get(name);
        Clip clip = clips[idx];
        setClipVolume(clip, sfxVolume * volMulti);
        clip.setFramePosition(0);
        clip.start();
        clipIndex.put(name, (idx + 1) % clips.length);
    }

    public static void playMusic(String name) {
        if (name == null || name.equals(currentMusicName))
            return;

        if (currentMusicName != null) {
            startFadeOut(currentMusicName, 250);
        }

        Clip next = musicPool.get(name);
        if (next != null) {
            currentMusicName = name;
            // Preload trên thread riêng để không block game loop
            new Thread(() -> {
                try {
                    next.setFramePosition(0);
                    setClipVolume(next, 0.0f);
                    next.start(); // Khởi động clip ở volume 0
                    Thread.sleep(300); // Chờ audio buffer sẵn sàng
                    next.stop();
                    next.setFramePosition(0); // Reset về đầu bài
                    next.loop(Clip.LOOP_CONTINUOUSLY);
                    next.start();
                    startFadeIn(name, 250);
                } catch (Exception e) {}
            }).start();
        }
    }

    public static void stopMusic() {
        if (currentMusicName != null) {
            Clip clip = musicPool.get(currentMusicName);
            if (clip != null)
                clip.stop();
            currentMusicName = null;
        }
    }

    public static void setSfxVolume(float vol) {
        sfxVolume = Math.max(0, Math.min(1, vol));
    }

    public static float getSfxVolume() {
        return sfxVolume;
    }

    public static void setMusicVolume(float vol) {
        musicVolume = Math.max(0, Math.min(1, vol));
        if (currentMusicName != null && !isManualFading) {
            setClipVolume(musicPool.get(currentMusicName), musicVolume);
        }
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    private static void setClipVolume(Clip clip, float vol) {
        if (clip == null) return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(vol <= 0 ? 0.0001 : vol) / Math.log(10.0) * 20.0);
                
                // Khuếch đại nhạc Boss (+5dB để tạo độ căng thẳng)
                if (currentMusicName != null && currentMusicName.startsWith("bossbgm")) {
                    dB += 5.0f;
                }
                
                // Đảm bảo dB nằm trong giới hạn cho phép
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }
        } catch (Exception e) {}
    }

    private static void startFadeIn(String name, int durationMs) {
        isManualFading = true;
        new Thread(() -> {
            Clip clip = musicPool.get(name);
            if (clip == null)
                return;
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < durationMs) {
                float progress = (float) (System.currentTimeMillis() - start) / durationMs;
                setClipVolume(clip, progress * musicVolume);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
            }
            setClipVolume(clip, musicVolume);
            isManualFading = false;
        }).start();
    }

    private static void startFadeOut(String name, int durationMs) {
        isManualFading = true;
        new Thread(() -> {
            Clip clip = musicPool.get(name);
            if (clip == null)
                return;
            float startVol = musicVolume;
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < durationMs) {
                float progress = 1.0f - (float) (System.currentTimeMillis() - start) / durationMs;
                setClipVolume(clip, progress * startVol);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
            }
            clip.stop();
            isManualFading = false;
        }).start();
    }

    public static void updateLoopFading() {
        // Không can thiệp nếu đang thực hiện chuyển nhạc thủ công
        if (currentMusicName == null || isManualFading)
            return;

        Clip clip = musicPool.get(currentMusicName);
        if (clip == null || !clip.isRunning())
            return;

        long currentFrame = clip.getFramePosition();
        long totalFrames = clip.getFrameLength();
        long framesRemaining = totalFrames - currentFrame;

        // Vùng Fade Out cuối bài (2 giây cuối)
        if (framesRemaining < 88200 && framesRemaining > 0) {
            float volFactor = (float) framesRemaining / 88200f;
            setClipVolume(clip, Math.max(0.01f, volFactor * musicVolume));
        } else {
            // Đảm bảo luôn ở mức âm lượng chuẩn nếu không ở vùng kết thúc
            // Điều này giải quyết lỗi nhạc bị nhỏ sau khi loop
            setClipVolume(clip, musicVolume);
        }
    }
}