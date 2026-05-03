package gameproject.state;

import java.awt.Graphics;
import gameproject.GamePanel;
import gameproject.ui.UpgradeUI;
import gameproject.weapon.Weapon;

public class LevelUpState implements State {
    private long menuOpenTime;
    
    public LevelUpState() {
        this.menuOpenTime = System.currentTimeMillis();
    }

    // Chuyển tỉ lệ cooldown & range từ vũ khí cũ sang vũ khí tiến hóa
    private Weapon transferStats(Weapon from, Weapon to) {
        float cooldownRatio = (float) from.cooldown / from.baseCooldown;
        float rangeRatio    = from.range / from.baseRange;
        to.cooldown = Math.max(30, (long) (to.baseCooldown * cooldownRatio));
        to.range    = to.baseRange * rangeRatio;
        return to;
    }

    @Override
    public void update(GamePanel game) {
        if (game.input.mouseClicked) {
            if (System.currentTimeMillis() - menuOpenTime < 500) {
                game.input.clearClickAndKey();
                return;
            }
            int boxWidth = 320, boxHeight = 400, spacing = 50;
            int startX = (game.screenWidth - (3 * boxWidth + 2 * spacing)) / 2;
            int by = (game.screenHeight - boxHeight) / 2 + 50;
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            for (int i = 0; i < 3; i++) {
                int bx = startX + i * (boxWidth + spacing);
                if (mx >= bx && mx <= bx + boxWidth && my >= by && my <= by + boxHeight) {
                    game.upgradeManager.applyUpgrade(game.upgradeManager.currentUpgradeOptions[i], game.player, game.activeSkills, game.currentWeapon);
                    
                    // Tiến hóa vũ khí – kế thừa tỉ lệ cooldown & range từ vũ khí cũ
                    Weapon old = game.currentWeapon;
                    if (old instanceof gameproject.weapon.Shotgun &&
                        game.player.getBreakthroughLevel(gameproject.skill.Upgrade.EXPLOSIVE_BULLETS) > 0 &&
                        game.player.getUpgradeLevel(gameproject.skill.Upgrade.DAMAGE) >= 3) {
                        game.currentWeapon = transferStats(old, new gameproject.weapon.HellfireBoomstick());
                    } else if (old instanceof gameproject.weapon.AssaultRifle &&
                        game.player.getUpgradeLevel(gameproject.skill.Upgrade.OPTICAL_SCOPE) >= 3) {
                        game.currentWeapon = transferStats(old, new gameproject.weapon.Railgun());
                    } else if (old instanceof gameproject.weapon.SMG &&
                        game.player.getBreakthroughLevel(gameproject.skill.Upgrade.CHAIN_LIGHTNING) > 0 &&
                        game.player.getUpgradeLevel(gameproject.skill.Upgrade.FIRE_RATE) >= 3) {
                        game.currentWeapon = transferStats(old, new gameproject.weapon.LightningGun());
                    }

                    game.changeState(new PlayingState());
                    break;
                }
            }
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        UpgradeUI.draw(g, game.screenWidth, game.screenHeight, game.upgradeManager.playerLevel, 
                      game.upgradeManager.currentUpgradeOptions, game.player, game.input.mouseX, game.input.mouseY);
    }
}
