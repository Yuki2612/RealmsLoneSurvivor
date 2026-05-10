package gameproject.meta;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import gameproject.GamePanel;

public class AchievementManager {
    private static AchievementManager instance;
    private Map<String, Achievement> achievements = new HashMap<>();
    
    // Chỉ số tích lũy lưu trữ vĩnh viễn
    private int totalKills = 0;
    private int totalGold = 0;
    
    // Toast notification queue
    private List<Achievement> toastQueue = new ArrayList<>();
    private Achievement currentToast = null;
    private long toastStartTime = 0;
    private static final long TOAST_DURATION = 4000; // 4 seconds
    
    // Theo dõi vũ khí tiến hóa duy nhất và các chỉ số tích lũy
    private int totalDeaths = 0;
    private int totalDestructions = 0;
    private int totalSoulStones = 0;
    private java.util.Set<String> evolvedWeaponsUnlocked = new java.util.HashSet<>();

    private AchievementManager() {
        initAchievements();
        load();
    }

    public static AchievementManager getInstance() {
        if (instance == null) {
            instance = new AchievementManager();
        }
        return instance;
    }

    private void initAchievements() {
        // KILLS
        addAchievement(new Achievement("kill_50", "Novice Hunter", "Defeat 50 enemies", "kills", 50, 100, 0));
        addAchievement(new Achievement("kill_200", "Pixel Warrior", "Defeat 200 enemies", "kills", 200, 500, 1));
        addAchievement(new Achievement("kill_1000", "Meat Grinder", "Defeat 1000 enemies", "kills", 1000, 2000, 5));

        // GOLD
        addAchievement(new Achievement("gold_200", "Lucky Find", "Collect 200 Gold", "gold", 200, 100, 0));
        addAchievement(new Achievement("gold_1000", "Well-to-do", "Collect 1000 Gold", "gold", 1000, 500, 1));
        addAchievement(new Achievement("gold_5000", "Pixel Millionaire", "Collect 5000 Gold", "gold", 5000, 2000, 10));

        // LEVEL
        addAchievement(new Achievement("level_5", "Good Start", "Reach level 5", "level", 5, 200, 1));
        addAchievement(new Achievement("level_15", "Seasoned Fighter", "Reach level 15", "level", 15, 1000, 5));
        addAchievement(new Achievement("level_25", "Living Legend", "Reach level 25", "level", 25, 2000, 15));

        // BOSS
        addAchievement(new Achievement("boss_1", "Death Defier", "Defeat Soul Reaper (Boss 1)", "boss", 1, 500, 2));
        addAchievement(new Achievement("boss_2", "Shadow Slayer", "Defeat Shadow Boss (Boss 2)", "boss", 2, 800, 4));
        addAchievement(new Achievement("boss_3", "Dark Wings", "Defeat Dark Fairy (Boss 3)", "boss", 3, 1200, 8));
        addAchievement(new Achievement("boss_4", "Regicide", "Defeat King Boss (Boss 4)", "boss", 4, 1600, 12));
        addAchievement(new Achievement("boss_5", "Soul Manipulator", "Defeat Phantom Warlock (Boss 5)", "boss", 5, 2000, 20));

        // SURVIVAL
        addAchievement(new Achievement("time_3", "Stamina", "Survive for 3 minutes", "time", 180, 300, 1));
        addAchievement(new Achievement("time_10", "Survivor", "Survive for 10 minutes", "time", 600, 1000, 5));
        addAchievement(new Achievement("time_15", "Immortal", "Survive for 15 minutes", "time", 900, 2000, 15));

        // EVENTS (First Encounters)
        addAchievement(new Achievement("event_darkness", "Night Owl", "Encounter the Darkness event", "event", 1, 500, 2));
        addAchievement(new Achievement("event_acid_rain", "Under the Shelter", "Encounter the Acid Rain event", "event", 1, 500, 2));
        addAchievement(new Achievement("event_mimic_mania", "Trust Issues", "Encounter the Mimic Mania event", "event", 1, 500, 2));
        addAchievement(new Achievement("event_blood_moon", "Bloodthirst", "Encounter the Blood Moon event", "event", 1, 1000, 5));

        addAchievement(new Achievement("kill_100", "Small Fry", "Kill 100 enemies", "kills", 100, 100, 1));
        addAchievement(new Achievement("kill_1000", "Bounty Hunter", "Kill 1000 enemies", "kills", 1000, 500, 5));
        addAchievement(new Achievement("kill_10000", "Natural Disaster", "Kill 10000 enemies", "kills", 10000, 2000, 20));

        // CATEGORY 1: COMBAT (Tiered)
        addAchievement(new Achievement("combat_combo_1", "Style Points", "Reach 50 Combo", "combat", 50, 200, 2));
        addAchievement(new Achievement("combat_combo_2", "Combo Master", "Reach 100 Combo", "combat", 100, 500, 5));
        addAchievement(new Achievement("combat_combo_3", "Untouchable God", "Reach 200 Combo", "combat", 200, 1500, 15));

        addAchievement(new Achievement("combat_flawless_1", "Untouchable", "Complete 1 wave without taking damage", "combat", 1, 300, 3));
        addAchievement(new Achievement("combat_flawless_2", "Matrix Style", "Complete 3 waves without taking damage", "combat", 3, 800, 8));
        addAchievement(new Achievement("combat_flawless_3", "Can't Touch This", "Complete 5 waves without taking damage", "combat", 5, 2000, 20));

        addAchievement(new Achievement("combat_pacifist_1", "Non-Violent Resistance", "Survive 1 wave without shooting", "combat", 1, 500, 5));
        addAchievement(new Achievement("combat_pacifist_2", "Gandhi Mode", "Survive 3 waves without shooting", "combat", 3, 1200, 12));
        addAchievement(new Achievement("combat_pacifist_3", "Pure Zen", "Survive 5 waves without shooting", "combat", 5, 3000, 30));

        // CATEGORY 2: WEALTH & PROGRESS
        addAchievement(new Achievement("wealth_gold_1", "Spare Change", "Collect 500 gold in one run", "wealth", 500, 100, 1));
        addAchievement(new Achievement("wealth_gold_2", "Gold Digger", "Collect 2000 gold in one run", "wealth", 2000, 500, 5));
        addAchievement(new Achievement("wealth_gold_3", "Scrooge McDuck", "Collect 5000 gold in one run", "wealth", 5000, 2000, 20));

        addAchievement(new Achievement("wealth_soul_1", "Soul Collector", "Collect 100 total souls", "souls", 100, 500, 5));
        addAchievement(new Achievement("wealth_soul_2", "Reaper", "Collect 500 total souls", "souls", 500, 1500, 15));
        addAchievement(new Achievement("wealth_soul_3", "Death's Best Friend", "Collect 2000 total souls", "souls", 2000, 5000, 50));

        addAchievement(new Achievement("wealth_peak", "Over 9000!", "Max out any stat", "wealth", 1, 1000, 10));

        // CATEGORY 3: EXPLORATION
        addAchievement(new Achievement("explorer_marathon_1", "Steady Strides", "Travel 100k pixels", "explorer", 100000, 200, 2));
        addAchievement(new Achievement("explorer_marathon_2", "Leg Day", "Travel 300k pixels", "explorer", 300000, 600, 6));
        addAchievement(new Achievement("explorer_marathon_3", "Forrest Gump", "Travel 1M pixels", "explorer", 1000000, 2000, 20));

        addAchievement(new Achievement("explorer_homebody_1", "Rent Free", "Stay in buildings for 2 mins", "explorer", 2, 200, 2));
        addAchievement(new Achievement("explorer_homebody_2", "Squatter", "Stay in buildings for 5 mins", "explorer", 5, 500, 5));
        addAchievement(new Achievement("explorer_homebody_3", "Real Estate Mogul", "Stay in buildings for 10 mins", "explorer", 10, 1500, 15));

        addAchievement(new Achievement("explorer_lucky", "Jackpot!", "Find 3 items in one chest", "explorer", 1, 500, 5));
        addAchievement(new Achievement("explorer_architect", "The Grand Tour", "Visit all buildings in a run", "explorer", 1, 1000, 10));

        // SECRETS
        addAchievement(new Achievement("secret_3_evolutions", "Weapon Master", "Unlock 3 different evolved weapons", "secret", 3, 2000, 15));
        addAchievement(new Achievement("secret_mimic_trap", "Bamboozled", "Encounter a Mimic in a treasure chest", "secret", 1, 500, 5));
        addAchievement(new Achievement("secret_admin_access", "There is no Spoon", "Enter the correct Admin password", "secret", 1, 1000, 10));
        addAchievement(new Achievement("secret_untouchable", "God Mode", "Complete a run without taking damage", "secret", 1, 5000, 50));
        addAchievement(new Achievement("secret_speedrunner", "Sonic Speed", "Complete game in under 15 minutes", "secret", 1, 2000, 20));
        addAchievement(new Achievement("secret_clutch_survivor", "Living on the Edge", "Survive 5 mins with 1 HP remaining", "secret", 1, 3000, 30));
        addAchievement(new Achievement("secret_no_dash", "The Floor is Lava", "Complete a run without using Dash", "secret", 1, 3000, 30));
        addAchievement(new Achievement("secret_phantom_troll", "Stop Hitting Yourself", "Hit Phantom clones 10 times before killing it", "secret", 1, 2000, 20));

        // PROGRESSION & DESTRUCTION
        addAchievement(new Achievement("fail_1", "Hard Knock Life", "Die for the first time", "fail", 1, 100, 1));
        addAchievement(new Achievement("fail_10", "Determined", "Die 10 times", "fail", 10, 500, 5));
        addAchievement(new Achievement("victory_1", "First Light", "Complete your first run", "victory", 1, 1000, 10));
        addAchievement(new Achievement("destroy_10", "Vandal", "Destroy 10 environment objects", "destroy", 10, 200, 1));
        addAchievement(new Achievement("destroy_100", "Demolition Man", "Destroy 100 environment objects", "destroy", 100, 1000, 10));
    }

    private void addAchievement(Achievement a) {
        achievements.put(a.id, a);
    }

    public void addKill() {
        totalKills++;
        onStatChanged("kills", totalKills);
    }

    public int getTotalKills() {
        return totalKills;
    }

    public void addGold(int amount) {
        totalGold += amount;
        onStatChanged("gold", totalGold);
        save();
    }

    public void addSoul(int amount) {
        totalSoulStones += amount;
        onStatChanged("souls", totalSoulStones);
        save();
    }

    public void updateLevel(int level) {
        onStatChanged("level", level);
    }

    public void updateSurvivalTime(int seconds) {
        onStatChanged("time", seconds);
    }

    public void onEventMet(String eventName) {
        onStatChanged("event", 1, eventName);
    }

    public void onSecretTriggered(String secretId) {
        onStatChanged("secret", 1, secretId);
    }

    public void onEvolvedWeapon(String weaponId) {
        if (!evolvedWeaponsUnlocked.contains(weaponId)) {
            evolvedWeaponsUnlocked.add(weaponId);
            onStatChanged("secret", evolvedWeaponsUnlocked.size(), "3_evolutions");
            save();
        }
    }

    public void addDeath() {
        totalDeaths++;
        onStatChanged("fail", totalDeaths);
        save();
    }

    public void addDestruction() {
        totalDestructions++;
        onStatChanged("destroy", totalDestructions);
        save();
    }

    private float totalDistance = 0;

    public void updateDistance(float d) {
        totalDistance += d;
        onStatChanged("explorer", (int) totalDistance, "marathon");
    }

    public void onVictory(int timeSeconds, boolean noHit, boolean noDash) {
        onStatChanged("victory", 1, "1"); // victory_1
        if (timeSeconds < 900) { // 15 mins
            onStatChanged("secret", 1, "speedrunner");
        }
        if (noHit) {
            onStatChanged("secret", 1, "untouchable");
        }
        if (noDash) {
            onStatChanged("secret", 1, "no_dash");
        }
        save();
    }

    public void onStatMaxed() {
        onStatChanged("wealth", 1, "peak");
    }

    public void onTripleChest() {
        onStatChanged("explorer", 1, "lucky");
    }

    public void onClutchSurvived() {
        onStatChanged("secret", 1, "clutch_survivor");
    }

    public void onStatChanged(String category, int value) {
        onStatChanged(category, value, null);
    }

    public void onStatChanged(String category, int value, String specificId) {
        for (Achievement a : achievements.values()) {
            // Nếu có specificId thì ưu tiên kiểm tra exact match hoặc match pattern category_id
            if (specificId != null) {
                // Kiểm tra xem ID thành tựu có chứa specificId không (ví dụ: wealth_gold_1 chứa "gold")
                if (!a.id.contains(specificId)) continue;
            }

            if (a.category.equals(category) && !a.isCompleted) {
                boolean wasCompleted = a.isCompleted;
                a.updateProgress(value);
                if (a.isCompleted && !wasCompleted) {
                    toastQueue.add(a);
                    gameproject.SoundManager.play("achievement");
                    save();
                }
            }
        }
    }

    // Đặc biệt cho boss vì boss tính theo ID boss đã giết
    public void onBossKilled(int bossIndex) {
        String bossId = "boss_" + bossIndex;
        Achievement a = achievements.get(bossId);
        if (a != null && !a.isCompleted) {
            a.updateProgress(bossIndex);
            toastQueue.add(a);
            gameproject.SoundManager.play("achievement");
            save();
        }
    }

    public void claimReward(String id) {
        Achievement a = achievements.get(id);
        if (a != null && a.isCompleted && !a.isClaimed) {
            PlayerData.gold += a.goldReward;
            PlayerData.soulStones += a.soulReward;
            a.isClaimed = true;
            save();
            PlayerData.save();
        }
    }

    public void reset() {
        totalKills = 0;
        totalGold = 0;
        totalDeaths = 0;
        totalDestructions = 0;
        totalSoulStones = 0;
        totalDistance = 0;
        evolvedWeaponsUnlocked.clear();
        for (Achievement a : achievements.values()) {
            a.currentValue = 0;
            a.isCompleted = false;
            a.isClaimed = false;
        }
        save();
    }

    public void update(long currentTime) {
        if (currentToast == null && !toastQueue.isEmpty()) {
            currentToast = toastQueue.remove(0);
            toastStartTime = currentTime;
        }

        if (currentToast != null && currentTime - toastStartTime > TOAST_DURATION) {
            currentToast = null;
        }
    }

    public void drawToast(Graphics g, int sw, int sh) {
        Achievement toast = this.currentToast;
        if (toast == null) return;
        Graphics2D g2d = (Graphics2D) g;

        float alpha = 1.0f;
        long elapsed = System.currentTimeMillis() - toastStartTime;
        if (elapsed < 500) alpha = elapsed / 500f;
        else if (elapsed > 3500) alpha = Math.max(0, 1.0f - (elapsed - 3500) / 500f);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        int toastW = 350;
        int toastH = 80;
        int x = sw - toastW - 20;
        int y = 20;

        // Background
        g2d.setColor(new Color(20, 20, 35, 240));
        g2d.fillRoundRect(x, y, toastW, toastH, 15, 15);
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawRoundRect(x, y, toastW, toastH, 15, 15);

        // Icon
        g2d.setColor(new Color(255, 200, 0));
        g2d.fillOval(x + 15, y + 15, 50, 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(gameproject.FontManager.getFont(30f));
        g2d.drawString("🏆", x + 23, y + 50);

        // Text
        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(gameproject.FontManager.getFont(18f));
        g2d.drawString("NEW ACHIEVEMENT!", x + 80, y + 30);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(gameproject.FontManager.getFont(16f));
        g2d.drawString(toast.title, x + 80, y + 55);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    public Map<String, Achievement> getAchievements() {
        return achievements;
    }

    // Persistence
    private static final String ACHIEVEMENTS_FILE = "achievements.dat";

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ACHIEVEMENTS_FILE))) {
            Map<String, Object> saveData = new HashMap<>();
            saveData.put("achievements", achievements);
            saveData.put("totalKills", totalKills);
            saveData.put("totalGold", totalGold);
            saveData.put("totalDeaths", totalDeaths);
            saveData.put("totalDestructions", totalDestructions);
            saveData.put("totalDistance", (int) totalDistance);
            saveData.put("totalSoulStones", totalSoulStones);
            saveData.put("evolvedWeapons", new ArrayList<>(evolvedWeaponsUnlocked));
            oos.writeObject(saveData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void load() {
        File file = new File(ACHIEVEMENTS_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, Object> saveData = (Map<String, Object>) ois.readObject();
            
            totalKills = (int) saveData.getOrDefault("totalKills", 0);
            totalGold = (int) saveData.getOrDefault("totalGold", 0);
            totalDeaths = (int) saveData.getOrDefault("totalDeaths", 0);
            totalDestructions = (int) saveData.getOrDefault("totalDestructions", 0);
            totalDistance = (int) saveData.getOrDefault("totalDistance", 0);
            totalSoulStones = (int) saveData.getOrDefault("totalSoulStones", 0);
            
            List<String> evolved = (List<String>) saveData.get("evolvedWeapons");
            if (evolved != null) {
                evolvedWeaponsUnlocked = new java.util.HashSet<>(evolved);
            }
            
            Map<String, Achievement> loaded = (Map<String, Achievement>) saveData.get("achievements");
            if (loaded != null) {
                for (Map.Entry<String, Achievement> entry : loaded.entrySet()) {
                    Achievement current = achievements.get(entry.getKey());
                    if (current != null) {
                        current.currentValue = entry.getValue().currentValue;
                        current.isCompleted = entry.getValue().isCompleted;
                        current.isClaimed = entry.getValue().isClaimed;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing achievements found or error loading.");
        }
    }
}
