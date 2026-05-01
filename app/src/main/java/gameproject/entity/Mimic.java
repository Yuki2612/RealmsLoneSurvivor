package gameproject.entity;

import gameproject.GamePanel;
import gameproject.ImageManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Mimic extends Enemy {
    private long spawnTime;

    public Mimic(float x, float y, int wave) {
        // Máu Mimic: 150 + wave * 40
        super(x, y, 70, 150 + (wave * 40), 2.8f, Color.MAGENTA);
        this.spawnTime = GamePanel.getTickTime();
        this.isBoss = false;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies,
            int screenW, int screenH, GamePanel panel) {
        
        long currentTime = GamePanel.getTickTime();
        
        // Logic đuổi theo người chơi rất hung hãn
        float dx = playerX - x;
        float dy = playerY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 0) {
            float currentSpeed = speed * speedMultiplier;
            
            // Mimic có lunge attack (lao tới) mỗi 3 giây - tăng tốc lên ~4.5f
            long elapsed = currentTime - spawnTime;
            if (elapsed % 3000 < 600) {
                currentSpeed *= 1.6f;
            }

            float vx = (dx / dist) * currentSpeed;
            float vy = (dy / dist) * currentSpeed;

            // Di chuyển với vật lý cơ bản
            x += vx + velX + kbX;
            y += vy + velY + kbY;
        }

        // Ma sát vật lý quán tính
        velX *= 0.9f;
        velY *= 0.9f;
        kbX *= 0.85f;
        kbY *= 0.85f;
        
        // AI Phá vật cản
        if (dist > 0) {
            handleObstacleBreaking((playerX - x) / dist * speed, (playerY - y) / dist * speed, panel);
        }
    }

    @Override
    public void draw(Graphics g) {
        // Không vẽ nếu đang tan biến (lớp cha đã xử lý)
        if (isDying) return;

        Graphics2D g2d = (Graphics2D) g;
        BufferedImage img = ImageManager.get("mimic");
        if (img != null) {
            g2d.drawImage(img, (int) x, (int) y, size, size, null);
        } else {
            g2d.setColor(Color.MAGENTA);
            g2d.fillRect((int) x, (int) y, size, size);
        }
        
        // HP bar (Lớp cha cũng có thể vẽ, nhưng ta vẽ đè lên cho đặc thù)
        g2d.setColor(Color.BLACK);
        g2d.fillRect((int) x, (int) y - 10, size, 5);
        g2d.setColor(Color.RED);
        g2d.fillRect((int) x, (int) y - 10, (int) (size * ((float) hp / maxHp)), 5);
    }

    // Xóa takeDamage và getExpValue vì lớp cha đã quản lý qua hp và maxHp
}
