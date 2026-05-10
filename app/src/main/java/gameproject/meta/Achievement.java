package gameproject.meta;

import java.io.Serializable;

public class Achievement implements Serializable {
    public String id;
    public String title;
    public String description;
    public int targetValue;
    public int currentValue;
    public int goldReward;
    public int soulReward;
    public boolean isCompleted;
    public boolean isClaimed;
    public String category; // kills, gold, level, boss, time

    public Achievement(String id, String title, String description, String category, int targetValue, int goldReward, int soulReward) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.targetValue = targetValue;
        this.goldReward = goldReward;
        this.soulReward = soulReward;
        this.currentValue = 0;
        this.isCompleted = false;
        this.isClaimed = false;
    }

    public void updateProgress(int value) {
        if (isCompleted) return;
        
        // Luôn giữ kỷ lục cao nhất để thanh tiến trình không bị lùi
        if (value > currentValue) {
            currentValue = value;
        }

        if (currentValue >= targetValue) {
            isCompleted = true;
        }
    }
}
