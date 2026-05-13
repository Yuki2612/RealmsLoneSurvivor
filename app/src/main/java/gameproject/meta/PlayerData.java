package gameproject.meta;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class PlayerData {
    public static int gold = 2000;
    public static int soulStones = 20;
    public static Set<CharacterClass> unlockedClasses = new HashSet<>();
    public static Set<gameproject.skill.Upgrade> unlockedSkills = new HashSet<>();
    public static CharacterClass selectedClass = CharacterClass.YUKI;

    public static String getPlayerImageKey() {
        return "player" + (selectedClass.ordinal() + 1);
    }

    // Admin Debug settings (Reset every session or persist as you like, here we
    // keep them static)
    public static int debugStartWave = 1;
    public static int debugStartLevel = 1;

    public static int statHealthLevel = 0; // 1 level = +1 Max HP (max 3)
    public static int statDamageLevel = 0; // +1 dmg per level
    public static int statSpeedLevel = 0; // +2% speed per level
    public static int statDashLevel = 0; // -2% cooldown per level
    public static int statCritLevel = 0; // +1% crit per level
    public static int statCooldownLevel = 0; // -1% weapon cooldown per level

    // Evolution Stats (5 levels each)
    public static int evoVampirism = 0; // Life on Kill
    public static int evoBloodlust = 0; // Crit Multiplier
    public static int evoPhantomDash = 0; // Explosive Illusion
    public static int evoBerserker = 0; // Speed boost on damage
    public static int evoFrenzy = 0; // Fire rate on Crit

    public static java.util.Map<gameproject.skill.Upgrade, Integer> skillSoulLevels = new java.util.HashMap<>();

    private static final String SAVE_FILE = "savegame.dat";

    public static void load() {
        unlockedClasses.add(CharacterClass.YUKI);
        unlockedSkills.add(gameproject.skill.Upgrade.CHAIN_LIGHTNING);
        unlockedSkills.add(gameproject.skill.Upgrade.TRAIL_OF_FIRE);
        unlockedSkills.add(gameproject.skill.Upgrade.ORBITING_ORBS);
        unlockedSkills.add(gameproject.skill.Upgrade.EXPLOSIVE_BULLETS);
        unlockedSkills.add(gameproject.skill.Upgrade.FROST_AURA);
        unlockedSkills.add(gameproject.skill.Upgrade.POISON_CLOUD);

        File file = new File(SAVE_FILE);
        if (!file.exists())
            return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder encryptedContent = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                encryptedContent.append(line);
            }

            String decrypted = decrypt(encryptedContent.toString());
            StringReader sr = new StringReader(decrypted);
            BufferedReader reader = new BufferedReader(sr);

            String gLine = reader.readLine();
            if (gLine != null && gLine.contains(":") && gLine.split(":").length > 1)
                gold = Integer.parseInt(gLine.split(":")[1]);

            String sLine = reader.readLine();
            if (sLine != null && sLine.contains(":") && sLine.split(":").length > 1)
                soulStones = Integer.parseInt(sLine.split(":")[1]);

            String selLine = reader.readLine();
            if (selLine != null && selLine.contains(":") && selLine.split(":").length > 1) {
                try {
                    selectedClass = CharacterClass.valueOf(selLine.split(":")[1]);
                } catch (Exception e) {
                }
            }

            String unlockedLine = reader.readLine();
            if (unlockedLine != null && unlockedLine.contains(":") && unlockedLine.split(":").length > 1) {
                String[] classes = unlockedLine.split(":")[1].split(",");
                for (String c : classes) {
                    if (!c.trim().isEmpty()) {
                        try {
                            unlockedClasses.add(CharacterClass.valueOf(c.trim()));
                        } catch (Exception e) {
                        }
                    }
                }
            }

            String statsLine = reader.readLine();
            if (statsLine != null && statsLine.contains(":") && statsLine.split(":").length > 1) {
                String[] stats = statsLine.split(":")[1].split(",");
                if (stats.length >= 6) {
                    statHealthLevel = Integer.parseInt(stats[0]);
                    statDamageLevel = Integer.parseInt(stats[1]);
                    statSpeedLevel = Integer.parseInt(stats[2]);
                    statDashLevel = Integer.parseInt(stats[3]);
                    statCritLevel = Integer.parseInt(stats[4]);
                    statCooldownLevel = Integer.parseInt(stats[5]);
                    if (stats.length >= 10) {
                        evoVampirism = Integer.parseInt(stats[6]);
                        evoBloodlust = Integer.parseInt(stats[7]);
                        evoPhantomDash = Integer.parseInt(stats[8]);
                        evoBerserker = Integer.parseInt(stats[9]);
                        if (stats.length >= 11)
                            evoFrenzy = Integer.parseInt(stats[10]);
                    }
                }
            }

            String skillsLine = reader.readLine();
            if (skillsLine != null && skillsLine.contains(":") && skillsLine.split(":").length > 1) {
                String[] skills = skillsLine.split(":")[1].split(",");
                for (String s : skills) {
                    if (!s.trim().isEmpty()) {
                        String[] parts = s.split("=");
                        if (parts.length == 2) {
                            try {
                                skillSoulLevels.put(gameproject.skill.Upgrade.valueOf(parts[0]),
                                        Integer.parseInt(parts[1]));
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }

            String unlockedSkillsLine = reader.readLine();
            if (unlockedSkillsLine != null && unlockedSkillsLine.contains(":")
                    && unlockedSkillsLine.split(":").length > 1) {
                unlockedSkills.clear();
                String[] sks = unlockedSkillsLine.split(":")[1].split(",");
                for (String s : sks) {
                    if (!s.trim().isEmpty()) {
                        try {
                            unlockedSkills.add(gameproject.skill.Upgrade.valueOf(s.trim()));
                        } catch (Exception e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading or decrypting save file: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Gold:").append(gold).append("\n");
            sb.append("Souls:").append(soulStones).append("\n");
            sb.append("SelectedClass:").append(selectedClass.name()).append("\n");

            StringBuilder sbClasses = new StringBuilder();
            for (CharacterClass c : unlockedClasses) {
                sbClasses.append(c.name()).append(",");
            }
            sb.append("Unlocked:").append(sbClasses.toString()).append("\n");

            sb.append("Stats:").append(statHealthLevel).append(",").append(statDamageLevel).append(",")
                    .append(statSpeedLevel).append(",").append(statDashLevel).append(",")
                    .append(statCritLevel).append(",").append(statCooldownLevel).append(",")
                    .append(evoVampirism).append(",").append(evoBloodlust).append(",")
                    .append(evoPhantomDash).append(",").append(evoBerserker).append(",")
                    .append(evoFrenzy).append("\n");

            StringBuilder sbSkills = new StringBuilder();
            for (java.util.Map.Entry<gameproject.skill.Upgrade, Integer> entry : skillSoulLevels.entrySet()) {
                sbSkills.append(entry.getKey().name()).append("=").append(entry.getValue()).append(",");
            }
            sb.append("Skills:").append(sbSkills.toString()).append("\n");

            StringBuilder sbUnSkills = new StringBuilder();
            for (gameproject.skill.Upgrade u : unlockedSkills) {
                sbUnSkills.append(u.name()).append(",");
            }
            sb.append("UnlockedSkills:").append(sbUnSkills.toString()).append("\n");

            String encrypted = encrypt(sb.toString());
            try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
                pw.print(encrypted);
            }
        } catch (Exception e) {
            System.out.println("Error saving or encrypting file: " + e.getMessage());
        }
    }

    public static void reset() {
        gold = 2000;
        soulStones = 20;
        statHealthLevel = 0;
        statDamageLevel = 0;
        statSpeedLevel = 0;
        statDashLevel = 0;
        statCritLevel = 0;
        statCooldownLevel = 0;
        evoVampirism = 0;
        evoBloodlust = 0;
        evoPhantomDash = 0;
        evoBerserker = 0;
        evoFrenzy = 0;
        skillSoulLevels.clear();
        debugStartWave = 1;
        debugStartLevel = 1;
        unlockedClasses.clear();
        unlockedClasses.add(CharacterClass.YUKI);
        unlockedSkills.clear();
        unlockedSkills.add(gameproject.skill.Upgrade.CHAIN_LIGHTNING);
        unlockedSkills.add(gameproject.skill.Upgrade.TRAIL_OF_FIRE);
        unlockedSkills.add(gameproject.skill.Upgrade.ORBITING_ORBS);
        unlockedSkills.add(gameproject.skill.Upgrade.EXPLOSIVE_BULLETS);
        unlockedSkills.add(gameproject.skill.Upgrade.FROST_AURA);
        unlockedSkills.add(gameproject.skill.Upgrade.POISON_CLOUD);
        selectedClass = CharacterClass.YUKI;
        save();
    }

    private static final String CRYPTO_KEY = "pixel_survivor_2026_secret_key_v1";

    private static String encrypt(String data) {
        try {
            byte[] bytes = data.getBytes("UTF-8");
            byte[] keyBytes = CRYPTO_KEY.getBytes("UTF-8");
            byte[] result = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                result[i] = (byte) (bytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return java.util.Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            return data;
        }
    }

    private static String decrypt(String data) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(data);
            byte[] keyBytes = CRYPTO_KEY.getBytes("UTF-8");
            byte[] result = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                result[i] = (byte) (bytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return new String(result, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
