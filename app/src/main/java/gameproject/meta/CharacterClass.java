package gameproject.meta;

import gameproject.skill.Upgrade;

public enum CharacterClass {
    YUKI("Yuki", 5, 1.0f, 1.0f, null, 0),
    NAME2("Name 2", 3, 1.1f, 1.1f, Upgrade.DASH_COOLDOWN, 1000),
    NAME3("Name 3", 4, 1.0f, 1.15f, Upgrade.TRAIL_OF_FIRE, 2000),
    NAME4("Name 4", 4, 1f, 1.1f, Upgrade.FROST_AURA, 2000),
    NAME5("Name 5", 3, 1.2f, 1.0f, Upgrade.EXPLOSIVE_BULLETS, 5000);

    public final String name;
    public final int baseHp;
    public final float speedMulti;
    public final float damageMulti;
    public final Upgrade startingUpgrade;
    public final int unlockCost;

    CharacterClass(String name, int baseHp, float speedMulti, float damageMulti, Upgrade startingUpgrade,
            int unlockCost) {
        this.name = name;
        this.baseHp = baseHp;
        this.speedMulti = speedMulti;
        this.damageMulti = damageMulti;
        this.startingUpgrade = startingUpgrade;
        this.unlockCost = unlockCost;
    }
}
