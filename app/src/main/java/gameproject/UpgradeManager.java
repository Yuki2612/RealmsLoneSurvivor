package gameproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gameproject.skill.Upgrade;
import gameproject.skill.PassiveSkill;
import gameproject.skill.OrbitingOrbsSkill;
import gameproject.skill.TrailOfFireSkill;
import gameproject.skill.FrostAuraSkill;
import gameproject.skill.ExplosiveCorpseSkill;
import gameproject.skill.VampirismSkill;
import gameproject.skill.PoisonCloudSkill;
import gameproject.skill.EnergyShieldSkill;
import gameproject.skill.MeteorStrikeSkill;
import gameproject.skill.PulseWaveSkill;
import gameproject.weapon.Weapon;

public class UpgradeManager {
    public int playerLevel = 1;
    public int currentExp = 0;
    public int expToNextLevel = 100;

    // Những chỉ số gốc của Player được quản lý tập trung ở đây
    public int playerDamage = 10;
    public float bulletSpeedMulti = 1.0f;

    // Lưu 3 thẻ nâng cấp hiện tại trên màn hình
    public Upgrade[] currentUpgradeOptions;

    public void startNewGame(int startingLevel) {
        playerLevel = 1;
        currentExp = 0;
        expToNextLevel = 100;
        playerDamage = 10 + gameproject.meta.PlayerData.statDamageLevel;
        bulletSpeedMulti = 1.0f;

        if (startingLevel > 1) {
            // Tính toán tổng EXP cần thiết để đạt đến level mong muốn
            int totalExpNeeded = 0;
            for (int i = 1; i < startingLevel; i++) {
                totalExpNeeded += (int) (100 * Math.pow(1.25, i - 1));
            }
            // Gán EXP vào. Khi game bắt đầu, processLevelUp sẽ tự động kích hoạt giao diện chọn thẻ liên tục.
            currentExp = totalExpNeeded;
        }
        currentUpgradeOptions = null;
    }

    public void addExp(int amount) {
        currentExp += amount;
    }

    public boolean canLevelUp() {
        return currentExp >= expToNextLevel;
    }

    // Xử lý tăng cấp và sinh ra 3 thẻ nâng cấp ngẫu nhiên
    public boolean processLevelUp(Player player) {
        if (currentExp >= expToNextLevel) {
            currentExp -= expToNextLevel;
            playerLevel++;
            expToNextLevel = (int) (100 * Math.pow(1.25, playerLevel - 1));

            generateOptions(player);
            return true; // Trả về true báo hiệu game nên dừng để bốc thẻ
        }
        return false;
    }

    private void generateOptions(Player player) {
        if (playerLevel % 3 == 0) {
            generateBreakthroughOptions(player);
        } else {
            // Cấp bình thường sẽ bốc các thẻ tăng chỉ số
            List<Upgrade> normals = new ArrayList<>();
            for (Upgrade u : Upgrade.values()) {
                if (!u.isBreakthrough && player.getUpgradeLevel(u) < u.maxLevel)
                    normals.add(u);
            }
            Collections.shuffle(normals);
            while (normals.size() < 3)
                normals.add(Upgrade.SHIELD); // Fallback an toàn
            currentUpgradeOptions = new Upgrade[] { normals.get(0), normals.get(1), normals.get(2) };
        }
    }

    public void generateBreakthroughOptions(Player player) {
        List<Upgrade> owned = player.getOwnedBreakthroughs();
        List<Upgrade> allValidOptions = new ArrayList<>();

        // 1. Thêm các đột phá đã sở hữu mà chưa đạt cấp tối đa
        for (Upgrade u : owned) {
            if (player.getUpgradeLevel(u) < u.maxLevel)
                allValidOptions.add(u);
        }

        // 2. Nếu chưa đủ 5 loại đột phá, thêm các loại chưa sở hữu vào danh sách lựa chọn
        if (owned.size() < 5) {
            for (Upgrade u : Upgrade.values()) {
                if (u.isBreakthrough && !owned.contains(u) && gameproject.meta.PlayerData.unlockedSkills.contains(u)) {
                    allValidOptions.add(u);
                }
            }
        }

        // 3. Xáo trộn toàn bộ để đảm bảo tính ngẫu nhiên (không ưu tiên cái đã có lên slot 1)
        Collections.shuffle(allValidOptions);

        List<Upgrade> options = new ArrayList<>();
        for (int i = 0; i < Math.min(3, allValidOptions.size()); i++) {
            options.add(allValidOptions.get(i));
        }

        // 4. Fallback an toàn nếu không có đủ 3 lựa chọn
        while (options.size() < 3)
            options.add(Upgrade.SHIELD);

        currentUpgradeOptions = new Upgrade[] { options.get(0), options.get(1), options.get(2) };
    }

    // Khi người chơi chọn 1 thẻ nâng cấp khi người chơi chọn
    public void applyUpgrade(Upgrade upgrade, Player player, List<PassiveSkill> activeSkills, Weapon currentWeapon) {
        player.levelUpUpgrade(upgrade);

        if (upgrade.isBreakthrough) {
            boolean hasSkill = false;
            for (PassiveSkill s : activeSkills) {
                if ((upgrade == Upgrade.ORBITING_ORBS && s instanceof OrbitingOrbsSkill) ||
                        (upgrade == Upgrade.TRAIL_OF_FIRE && s instanceof TrailOfFireSkill) ||
                        (upgrade == Upgrade.FROST_AURA && s instanceof FrostAuraSkill) ||
                        (upgrade == Upgrade.EXPLOSIVE_CORPSE && s instanceof ExplosiveCorpseSkill) ||
                        (upgrade == Upgrade.POISON_CLOUD && s instanceof PoisonCloudSkill)) {
                    hasSkill = true;
                    break;
                }
            }
            if (!hasSkill) {
                if (upgrade == Upgrade.ORBITING_ORBS)
                    activeSkills.add(new OrbitingOrbsSkill());
                else if (upgrade == Upgrade.TRAIL_OF_FIRE)
                    activeSkills.add(new TrailOfFireSkill());
                else if (upgrade == Upgrade.FROST_AURA)
                    activeSkills.add(new FrostAuraSkill());
                else if (upgrade == Upgrade.EXPLOSIVE_CORPSE)
                    activeSkills.add(new ExplosiveCorpseSkill());
                else if (upgrade == Upgrade.POISON_CLOUD)
                    activeSkills.add(new PoisonCloudSkill());
                else if (upgrade == Upgrade.ENERGY_SHIELD)
                    activeSkills.add(new EnergyShieldSkill());
                else if (upgrade == Upgrade.METEOR_STRIKE)
                    activeSkills.add(new MeteorStrikeSkill());
                else if (upgrade == Upgrade.PULSE_WAVE)
                    activeSkills.add(new PulseWaveSkill());
            }
        } else {
            switch (upgrade) {
                case SHIELD -> player.addHeart();
                case DAMAGE -> playerDamage += 5;
                case FIRE_RATE -> currentWeapon.cooldown = (long) (currentWeapon.cooldown * 0.91);
                case MOVE_SPEED -> player.upgradeSpeed(0.3f);
                case DASH_COOLDOWN -> player.upgradeDashCooldown(150);
                case BULLET_SPEED -> bulletSpeedMulti += 0.12f;
                case VAMPIRISM -> {
                    boolean hasVamp = false;
                    for (PassiveSkill s : activeSkills) {
                        if (s instanceof VampirismSkill) {
                            hasVamp = true;
                            break;
                        }
                    }
                    if (!hasVamp) {
                        activeSkills.add(new VampirismSkill());
                    }
                }
                case OPTICAL_SCOPE -> currentWeapon.range *= 1.09f;
                default -> {
                }
            }
        }
    }
}
