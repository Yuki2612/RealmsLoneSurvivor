package gameproject.entity;

import gameproject.GamePanel;
import gameproject.ImageManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Mimic extends Enemy {
    public Mimic(float x, float y, int wave) {
        // Máu Mimic: 150 + wave * 40
        // GIẢM KÍCH CỠ: Để nhỏ gọn và tinh ranh hơn (45)
        super(x, y, 45, 300 + (wave * 70), 2.6f, Color.MAGENTA);
        this.isBoss = false;
    }

    @Override
    public void update(float playerX, float playerY, float speedMultiplier, ArrayList<Enemy> allEnemies,
            int screenW, int screenH, GamePanel panel) {
        // Sử dụng bộ não AI tập trung (Sliding Collision, Obstacle Avoidance)
        EnemyController.moveEnemy(this, panel, speedMultiplier);
    }

    @Override
    public void draw(Graphics g) {
        long now = GamePanel.getTickTime();
        BufferedImage[] anim = ImageManager.getAnimation("mimic_f");
        BufferedImage img = (anim != null && anim.length > 0) ? anim[(int)((now/150)%anim.length)] : null;
        
        if (img != null) {
            Graphics2D g2d = (Graphics2D) g;
            
            // 1. Alpha: fade out khi đang chết
            float alpha = 1.0f;
            if (isDying && deathFadeStartTime >= 0) {
                alpha = 1.0f - Math.min(1f, (float) (now - deathFadeStartTime) / deathFadeDuration);
            }
            java.awt.Composite oldComp = g2d.getComposite();
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));

            int drawX = (int) Math.round(x) - 20;
            int drawY = (int) Math.round(y) - 10;
            int drawW = size + 40; // Rộng hơn hẳn (85)
            int drawH = size + 10; // Thấp hơn (55)

            // 2. Vẽ bóng
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillOval(drawX + 10, drawY + drawH - 10, drawW - 20, 12);

            // 3. Lật ảnh & Vẽ
            if (movingRight) {
                g2d.drawImage(img, drawX, drawY, drawW, drawH, null);
            } else {
                g2d.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null);
            }
            
            // 4. Hit flash (giống Enemy.drawSprite)
            if (now < hitFlashEndTime) {
                float flashAlpha = 0.65f * (float) (hitFlashEndTime - now) / 80f;
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                        Math.max(0, Math.min(0.65f, flashAlpha))));
                g2d.setColor(Color.WHITE);
                g2d.fillRect(drawX, drawY, drawW, drawH);
            }
            
            g2d.setComposite(oldComp);
        } else {
            // FALLBACK: Nếu mất ảnh, vẽ hình vuông màu tím rực rỡ để không bị mất hình
            g.setColor(color != null ? color : Color.MAGENTA);
            g.fillRect((int) x, (int) y, size, size);
        }

        // KHÔI PHỤC: Thanh máu (HP bar) dưới chân giống các quái khác
        if (!isDying) {
            g.setColor(Color.RED);
            g.fillRect((int) x, (int) y + size, size, 4);
            g.setColor(Color.GREEN);
            g.fillRect((int) x, (int) y + size, (int) (size * ((float) hp / maxHp)), 4);
        }
    }

    @Override
    public int getExpValue() {
        // Giảm EXP nhận được từ Mimic (chỉ bằng 1/4 máu tối đa) để cân bằng game
        return maxHp / 4;
    }
}
