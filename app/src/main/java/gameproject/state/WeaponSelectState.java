package gameproject.state;

import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.ui.WeaponSelectUI;
import gameproject.weapon.*;
import gameproject.skill.Upgrade;

public class WeaponSelectState implements State {

    private Weapon[] options = null;

    // Chuyển tỉ lệ cooldown & range từ vũ khí cũ sang vũ khí được chọn
    private Weapon transferStats(Weapon from, Weapon to) {
        float cooldownRatio = (float) from.cooldown / from.baseCooldown;
        float rangeRatio = from.range / from.baseRange;
        to.cooldown = Math.max(30, (long) (to.baseCooldown * cooldownRatio));
        to.range = to.baseRange * rangeRatio;
        return to;
    }

    @Override
    public void update(GamePanel game) {

        if (options == null) {
            options = new Weapon[3];
            options[0] = new Shotgun();
            options[1] = new AssaultRifle();
            options[2] = new SMG();

            // Shotgun + Explosive Bullets lv1 + Might lv3 → Hellfire Boomstick
            if (game.currentWeapon instanceof Shotgun &&
                    game.player.getBreakthroughLevel(Upgrade.EXPLOSIVE_BULLETS) > 0 &&
                    game.player.getUpgradeLevel(Upgrade.DAMAGE) >= 3) {
                options[0] = new HellfireBoomstick();
            }
            // AssaultRifle + Optical Scope lv3 → Railgun
            if (game.currentWeapon instanceof AssaultRifle &&
                    game.player.getUpgradeLevel(Upgrade.OPTICAL_SCOPE) >= 3) {
                options[1] = new Railgun();
            }
            // SMG + Chain Lightning lv1 + Fire Rate lv3 → Lightning Gun
            if (game.currentWeapon instanceof SMG &&
                    game.player.getBreakthroughLevel(Upgrade.CHAIN_LIGHTNING) > 0 &&
                    game.player.getUpgradeLevel(Upgrade.FIRE_RATE) >= 3) {
                options[2] = new LightningGun();
            }
        }

        if (game.input.mouseClicked) {
            // ĐỒNG BỘ KÍCH THƯỚC VÀ VỊ TRÍ VỚI UI MỚI
            int cardW = 320, cardH = 460, spacing = 60;
            int totalW = (3 * cardW + 2 * spacing);
            int startX = (game.screenWidth - totalW) / 2;
            int startY = (game.screenHeight - cardH) / 2 + 40;
            
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            for (int i = 0; i < 3; i++) {
                int bx = startX + i * (cardW + spacing);
                // Kiểm tra click trong phạm vi thẻ (cardH có thể offset nhẹ nhưng ở đây ta dùng tọa độ tĩnh)
                if (mx >= bx && mx <= bx + cardW && my >= startY && my <= startY + cardH) {
                    // Kế thừa tỉ lệ nâng cấp từ vũ khí hiện tại
                    game.currentWeapon = transferStats(game.currentWeapon, options[i]);
                    gameproject.SoundManager.play("shoot"); // Âm thanh xác nhận
                    game.changeState(new PlayingState());
                    break;
                }
            }
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        if (options != null) {
            WeaponSelectUI.draw(g, game.screenWidth, game.screenHeight, options, game.input.mouseX, game.input.mouseY);
        }
    }
}
