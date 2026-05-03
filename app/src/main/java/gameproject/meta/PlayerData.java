package gameproject.meta;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class PlayerData {
    public static int gold = 0;
    public static int soulStones = 0;
    public static Set<CharacterClass> unlockedClasses = new HashSet<>();
    public static Set<gameproject.skill.Upgrade> unlockedSkills = new HashSet<>();
    public static CharacterClass selectedClass = CharacterClass.YUKI;

    public static String getPlayerImageKey() {
        return "player" + (selectedClass.ordinal() + 1);
    }
    
    // Admin Debug settings (Reset every session or persist as you like, here we keep them static)
    public static int debugStartWave = 1;
    public static int debugStartLevel = 1;

    public static int statHealthLevel = 0; // 10 level = 1 Heart
    public static int statDamageLevel = 0; // +1 dmg per level
    public static int statSpeedLevel = 0; // +2% speed per level
    public static int statDashLevel = 0; // -2% cooldown per level
    public static int statCritLevel = 0; // +1% crit per level
    public static int statCooldownLevel = 0; // -2% weapon cooldown per level

    public static java.util.Map<gameproject.skill.Upgrade, Integer> skillSoulLevels = new java.util.HashMap<>();

    private static final String SAVE_FILE = "savegame.dat";

    public static void load() {
        unlockedClasses.add(CharacterClass.YUKI); // Luôn mở khóa Yuki
        
        // Default unlocked skills (excluding Shield, Meteor, Pulsewave)
        unlockedSkills.add(gameproject.skill.Upgrade.CHAIN_LIGHTNING);
        unlockedSkills.add(gameproject.skill.Upgrade.TRAIL_OF_FIRE);
        unlockedSkills.add(gameproject.skill.Upgrade.ORBITING_ORBS);
        unlockedSkills.add(gameproject.skill.Upgrade.EXPLOSIVE_BULLETS);
        unlockedSkills.add(gameproject.skill.Upgrade.FROST_AURA);
        unlockedSkills.add(gameproject.skill.Upgrade.POISON_CLOUD);

        File file = new File(SAVE_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String gLine = br.readLine();
            if (gLine != null && gLine.contains(":") && gLine.split(":").length > 1) gold = Integer.parseInt(gLine.split(":")[1]);
            
            String sLine = br.readLine();
            if (sLine != null && sLine.contains(":") && sLine.split(":").length > 1) soulStones = Integer.parseInt(sLine.split(":")[1]);
            
            String selLine = br.readLine();
            if (selLine != null && selLine.contains(":") && selLine.split(":").length > 1) {
                try { selectedClass = CharacterClass.valueOf(selLine.split(":")[1]); } catch(Exception e) {}
            }
            
            String unlockedLine = br.readLine();
            if (unlockedLine != null && unlockedLine.contains(":") && unlockedLine.split(":").length > 1) {
                String[] classes = unlockedLine.split(":")[1].split(",");
                for (String c : classes) {
                    if (!c.trim().isEmpty()) {
                        try { unlockedClasses.add(CharacterClass.valueOf(c.trim())); } catch(Exception e) {}
                    }
                }
            }
            
            String statsLine = br.readLine();
            if (statsLine != null && statsLine.contains(":") && statsLine.split(":").length > 1) {
                String[] stats = statsLine.split(":")[1].split(",");
                if (stats.length >= 6) {
                    statHealthLevel = Integer.parseInt(stats[0]);
                    statDamageLevel = Integer.parseInt(stats[1]);
                    statSpeedLevel = Integer.parseInt(stats[2]);
                    statDashLevel = Integer.parseInt(stats[3]);
                    statCritLevel = Integer.parseInt(stats[4]);
                    statCooldownLevel = Integer.parseInt(stats[5]);
                }
            }
            
            String skillsLine = br.readLine();
            if (skillsLine != null && skillsLine.contains(":") && skillsLine.split(":").length > 1) {
                String[] skills = skillsLine.split(":")[1].split(",");
                for (String s : skills) {
                    if (!s.trim().isEmpty()) {
                        String[] parts = s.split("=");
                        if (parts.length == 2) {
                            try {
                                skillSoulLevels.put(gameproject.skill.Upgrade.valueOf(parts[0]), Integer.parseInt(parts[1]));
                            } catch(Exception e) {}
                        }
                    }
                }
            }

            String unlockedSkillsLine = br.readLine();
            if (unlockedSkillsLine != null && unlockedSkillsLine.contains(":") && unlockedSkillsLine.split(":").length > 1) {
                unlockedSkills.clear(); // Overwrite defaults if file has data
                String[] sks = unlockedSkillsLine.split(":")[1].split(",");
                for (String s : sks) {
                    if (!s.trim().isEmpty()) {
                        try { unlockedSkills.add(gameproject.skill.Upgrade.valueOf(s.trim())); } catch(Exception e) {}
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading save file: " + e.getMessage());
        }
    }

    public static void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
            pw.println("Gold:" + gold);
            pw.println("Souls:" + soulStones);
            pw.println("SelectedClass:" + selectedClass.name());
            
            StringBuilder sb = new StringBuilder();
            for (CharacterClass c : unlockedClasses) {
                sb.append(c.name()).append(",");
            }
            pw.println("Unlocked:" + sb.toString());
            
            pw.println("Stats:" + statHealthLevel + "," + statDamageLevel + "," + statSpeedLevel + "," + statDashLevel + "," + statCritLevel + "," + statCooldownLevel);
            
            StringBuilder sbSkills = new StringBuilder();
            for (java.util.Map.Entry<gameproject.skill.Upgrade, Integer> entry : skillSoulLevels.entrySet()) {
                sbSkills.append(entry.getKey().name()).append("=").append(entry.getValue()).append(",");
            }
            pw.println("Skills:" + sbSkills.toString());

            StringBuilder sbUnSkills = new StringBuilder();
            for (gameproject.skill.Upgrade u : unlockedSkills) {
                sbUnSkills.append(u.name()).append(",");
            }
            pw.println("UnlockedSkills:" + sbUnSkills.toString());
        } catch (Exception e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }
}
