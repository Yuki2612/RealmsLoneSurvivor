package gameproject;

import java.awt.Color;

public class ComboManager {
    private int comboCount = 0;
    private long lastKillTime = 0;
    private final long COMBO_DURATION = 3000; // Chuẩn 3 giây
    
    // Tier thresholds chuẩn từ tài liệu
    public static final int TIER_1 = 15;
    public static final int TIER_2 = 30;
    public static final int TIER_3 = 50;

    private float pulseScale = 1.0f;

    public void onEnemyKilled(boolean isBoss) {
        lastKillTime = System.currentTimeMillis();
        comboCount += (isBoss ? 20 : 1); // Boss chuẩn +20
        pulseScale = 1.4f;
    }

    public void update() {
        if (comboCount > 0 && System.currentTimeMillis() - lastKillTime > COMBO_DURATION) {
            comboCount = 0;
        }
        if (pulseScale > 1.0f) {
            pulseScale -= 0.04f;
            if (pulseScale < 1.0f) pulseScale = 1.0f;
        }
    }

    public float getFireRateBonus() {
        if (comboCount >= TIER_3) return 0.10f; // Tier 3: +10%
        if (comboCount >= TIER_2) return 0.05f; // Tier 2: +5%
        return 0f;
    }

    public float getMoveSpeedBonus() {
        if (comboCount >= TIER_3) return 0.20f; // Tier 3: +20%
        if (comboCount >= TIER_2) return 0.15f; // Tier 2: +15%
        if (comboCount >= TIER_1) return 0.10f; // Tier 1: +10%
        return 0f;
    }

    public String getTierTitle() {
        if (comboCount >= TIER_3) return "RAMPAGE!";
        if (comboCount >= TIER_2) return "GREAT!";
        return "";
    }

    public int getComboCount() { return comboCount; }
    public float getPulseScale() { return pulseScale; }
    
    public float getTimerRatio() {
        if (comboCount == 0) return 0;
        long elapsed = System.currentTimeMillis() - lastKillTime;
        return Math.max(0, 1.0f - (float) elapsed / COMBO_DURATION);
    }

    public Color getComboColor() {
        if (comboCount >= TIER_3) return new Color(255, 140, 0); // Orange-Gold rực cháy
        if (comboCount >= TIER_2) return new Color(255, 165, 0); 
        if (comboCount >= TIER_1) return new Color(255, 255, 100); 
        return Color.WHITE;
    }

    public int getTier() {
        if (comboCount >= TIER_3) return 3;
        if (comboCount >= TIER_2) return 2;
        if (comboCount >= TIER_1) return 1;
        return 0;
    }
}
