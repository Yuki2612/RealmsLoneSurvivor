package gameproject;

import java.util.ArrayList;

public class Shotgun extends Weapon {
    public Shotgun() {
        super(8, 800); // Sát thương thấp hơn mỗi viên, nhưng bắn chậm và ra nhiều viên
    }

    @Override
    public void shoot(float startX, float startY, float targetX, float targetY,
            float bulletSpeedMulti, int extraDamage, int bounces,
            ArrayList<Projectile> projectiles) {

        // Tính toán góc bắn tới mục tiêu
        float dx = targetX - startX;
        float dy = targetY - startY;
        double baseAngle = Math.atan2(dy, dx);

        // Bắn 3 viên đạn hình quạt (lệch nhau 15 độ = ~0.26 radian)
        double[] angles = { baseAngle - 0.26, baseAngle, baseAngle + 0.26 };

        for (double angle : angles) {
            float tX = startX + (float) Math.cos(angle) * 100;
            float tY = startY + (float) Math.sin(angle) * 100;

            Projectile p = new Projectile(startX, startY, tX, tY, bulletSpeedMulti);
            p.damage = this.baseDamage + extraDamage;
            p.bouncesLeft = bounces; // Shotgun nảy sét sẽ rất hủy diệt
            projectiles.add(p);
        }

        this.lastShootTime = System.currentTimeMillis();
        SoundManager.play("shoot");
    }
}