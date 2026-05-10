package gameproject.skill;

public enum Upgrade {
        // Nâng cấp chỉ số (Normal)
        SHIELD("Extra Heart (+1 Max HP)", false, 10),
        DAMAGE("Might (+5 Damage)", false, 10),
        FIRE_RATE("Light Barrel (-9% Cooldown)", false, 10),
        MOVE_SPEED("Swift Boots (+0.3 Move Speed)", false, 10),
        DASH_COOLDOWN("Engine Core (-150ms Dash Cooldown)", false, 10),

        CRIT_CHANCE("Precision (+7% Crit Chance)", false, 7),
        OPTICAL_SCOPE("Optical Scope (+9% Weapon Range)", false, 6),
        VAMPIRISM("Vampirism (0.5% Heal chance on kill)", false, 5),

        // Nâng cấp Đột phá (Breakthrough) - Tất cả Cap ở Lv 5
        CHAIN_LIGHTNING("Chain Lightning (+Bounces & Range)", true, 5),
        TRAIL_OF_FIRE("Trail of Fire (Movement leaves fire)", true, 5),
        ORBITING_ORBS("Orbiting Orbs (+Orbs & Damage)", true, 5),
        EXPLOSIVE_BULLETS("Explosive Bullets (+Radius & Damage)", true, 5),
        FROST_AURA("Frost Aura (+Freeze Radius)", true, 5),
        POISON_CLOUD("Poison Cloud (+Radius & Chance)", true, 5),
        ENERGY_SHIELD("Energy Shield ( Guard, -- CD)", true, 5),
        METEOR_STRIKE("Meteor Strike (Random AOE explosions)", true, 5),
        PULSE_WAVE("Pulse Wave (AOE knockback & damage)", true, 5);

        public final String description;
        public final boolean isBreakthrough;
        public final int maxLevel;

        Upgrade(String description, boolean isBreakthrough, int maxLevel) {
                this.description = description;
                this.isBreakthrough = isBreakthrough;
                this.maxLevel = maxLevel;
        }
}