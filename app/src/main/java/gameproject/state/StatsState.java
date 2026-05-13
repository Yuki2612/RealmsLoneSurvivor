package gameproject.state;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import gameproject.GamePanel;
import gameproject.meta.PlayerData;

public class StatsState implements State {
    public static final double PRICE_MULTIPLIER = 1.14;
    public static float offsetX = 0, offsetY = 0;
    public static final float TREE_SCALE = 0.8f; // Synchronized scale factor

    public static class StatNode {
        public int statIndex;
        public String specialName = "";
        public int bonusAmount;
        public int levelTarget;
        public int cx, cy;
        public int parentIdx = -1;
        public int parentIdx2 = -1;
        public boolean isMajor = false;
        public boolean isEvolution = false;
        public Color branchColor;

        public StatNode(int idx, int bonus, int target, int x, int y, int pIdx, Color color, boolean major) {
            this.statIndex = idx;
            this.bonusAmount = bonus;
            this.levelTarget = target;
            this.cx = x;
            this.cy = y;
            this.parentIdx = pIdx;
            this.branchColor = color;
            this.isMajor = major;
        }
    }

    public static StatNode[] nodes;
    private String[] statNames = { "Health", "Might", "Speed", "Dash", "Crit", "Fire Rate" };
    private String[] statDescs = { "+1 Heart", "+1 Damage", "+2% Speed", "-2% Cool", "+1% Crit", "+2% Rate" };

    @Override
    public void update(GamePanel game) {
        if (nodes == null)
            initTree(game.screenWidth, game.screenHeight);

        if (game.input.escPressed) {
            PlayerData.save();
            game.changeState(new MenuState());
            game.input.clearClickAndKey();
            return;
        }

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            // BACK BUTTON (Fixed UI Space - Screen Coordinates)
            if (mx >= 50 && mx <= 190 && my >= game.screenHeight - 80 && my <= game.screenHeight - 35) {
                PlayerData.save();
                game.changeState(new MenuState());
                game.input.clearClickAndKey();
                return;
            }

            // NODES (World Space - Scaled Coordinates)
            int sw = game.screenWidth;
            int sh = game.screenHeight;
            int adjMx = (int) ((mx - sw / 2) / TREE_SCALE + sw / 2 - offsetX);
            int adjMy = (int) ((my - sh / 2) / TREE_SCALE + sh / 2 - offsetY);

            for (int i = 0; i < nodes.length; i++) {
                StatNode node = nodes[i];
                int r = node.isEvolution ? 70 : (node.isMajor ? 55 : 35);
                double dist = Math.sqrt(Math.pow(adjMx - node.cx, 2) + Math.pow(adjMy - node.cy, 2));

                if (dist <= r) {
                    // Nếu đã nâng cấp tối đa ô này rồi thì bỏ qua
                    if (isNodeUnlocked(node)) continue;

                    if (node.parentIdx != -1 && !isNodeUnlocked(nodes[node.parentIdx]))
                        continue;
                    if (node.isEvolution && node.parentIdx2 != -1 && !isNodeUnlocked(nodes[node.parentIdx2]))
                        continue;

                    if (node.isEvolution) {
                        int currentLv = getEvolutionLevel(node.specialName);
                        if (currentLv < 5) {
                            int cost = calculateEvoCost(node.specialName, currentLv);
                            if (PlayerData.soulStones >= cost) {
                                PlayerData.soulStones -= cost;
                                upgradeEvolution(node.specialName);
                                gameproject.SoundManager.play("evolve");
                                gameproject.ui.StatsUI.requestRebake();
                            }
                        }
                    } else {
                        int currentLv = getPlayerDataLevel(node.statIndex);
                        if (currentLv >= node.levelTarget - node.bonusAmount) {
                            int cost = calculateNodeCost(node, currentLv);
                            if (PlayerData.gold >= cost) {
                                PlayerData.gold -= cost;
                                upgradeStat(node.statIndex, node.bonusAmount);
                                gameproject.SoundManager.play("shoot");
                                gameproject.ui.StatsUI.requestRebake();
                            }
                        }
                    }
                }
            }
            game.input.clearClickAndKey();
        }
    }

    private void initTree(int sw, int sh) {
        List<StatNode> list = new ArrayList<>();
        int cx = sw / 2;
        int cy = sh / 2 + 300; // Moved up from +500 to allow room for downward branches

        Color green = new Color(50, 255, 100);
        Color red = new Color(255, 50, 80);
        Color yellow = new Color(255, 220, 50);
        Color white = Color.WHITE;

        StatNode root = new StatNode(1, 1, 1, cx, cy, -1, white, false);
        list.add(root);
        int rootIdx = 0;

        // 1. HEALTH (statIndex = 0)
        int lastHPIdx = rootIdx;
        int hpMajor1 = -1;
        for (int i = 0; i < 3; i++) {
            StatNode n = new StatNode(0, 1, i + 1, cx, cy - (i + 1) * 450, lastHPIdx, green, true);
            list.add(n);
            lastHPIdx = list.size() - 1;
            if (i == 0) hpMajor1 = lastHPIdx;
        }

        // 2. DAMAGE (statIndex = 1)
        int lastMightIdx = rootIdx;
        int mTarget = 0;
        int mightMajor1 = -1;
        for (int g = 0; g < 3; g++) { // 3 groups * 10 = 30
            for (int i = 0; i < 2; i++) {
                mTarget += 2;
                StatNode n = new StatNode(1, 2, mTarget, (int) (cx + 350 + g * 400 + i * 130),
                        (int) (cy - 350 - g * 400 - i * 130), lastMightIdx, red, false);
                list.add(n);
                lastMightIdx = list.size() - 1;
            }
            mTarget += 6;
            StatNode major = new StatNode(1, 6, mTarget, (int) (cx + 650 + g * 400), (int) (cy - 650 - g * 400),
                    lastMightIdx, red, true);
            list.add(major);
            lastMightIdx = list.size() - 1;
            if (g == 0) mightMajor1 = lastMightIdx;
        }

        // 3. CRIT (statIndex = 4)
        int lastCritIdx = rootIdx;
        int crTarget = 0;
        int critMajor1 = -1;
        for (int g = 0; g < 3; g++) {
            for (int i = 0; i < 2; i++) {
                crTarget++;
                StatNode n = new StatNode(4, 1, crTarget, cx + 450 + g * 450 + i * 140, cy, lastCritIdx, red, false);
                list.add(n);
                lastCritIdx = list.size() - 1;
            }
            crTarget += 3;
            StatNode major = new StatNode(4, 3, crTarget, cx + 750 + g * 450, cy, lastCritIdx, red, true);
            list.add(major);
            lastCritIdx = list.size() - 1;
            if (g == 0) critMajor1 = lastCritIdx;
        }

        // 4. MOBILITY (statIndex = 2)
        int lastSpeedIdx = rootIdx;
        int sTarget = 0;
        int speedMajor1 = -1;
        for (int g = 0; g < 2; g++) {
            for (int i = 0; i < 2; i++) {
                sTarget++;
                StatNode n = new StatNode(2, 1, sTarget, (int) (cx - 350 - g * 400 - i * 130),
                        (int) (cy - 350 - g * 400 - i * 130), lastSpeedIdx, yellow, false);
                list.add(n);
                lastSpeedIdx = list.size() - 1;
            }
            sTarget += 3;
            StatNode major = new StatNode(2, 3, sTarget, (int) (cx - 650 - g * 400), (int) (cy - 650 - g * 400),
                    lastSpeedIdx, yellow, true);
            list.add(major);
            lastSpeedIdx = list.size() - 1;
            if (g == 0) speedMajor1 = lastSpeedIdx;
        }

        // 5. DASH (statIndex = 3)
        int lastDashIdx = rootIdx;
        int dTarget = 0;
        int dashMajor1 = -1;
        for (int g = 0; g < 2; g++) {
            for (int i = 0; i < 2; i++) {
                dTarget++;
                StatNode n = new StatNode(3, 1, dTarget, cx - 450 - g * 450 - i * 140, cy, lastDashIdx, yellow, false);
                list.add(n);
                lastDashIdx = list.size() - 1;
            }
            dTarget += 3;
            StatNode major = new StatNode(3, 3, dTarget, cx - 750 - g * 450, cy, lastDashIdx, yellow, true);
            list.add(major);
            lastDashIdx = list.size() - 1;
            if (g == 0) dashMajor1 = lastDashIdx;
        }

        // 6. FIRE RATE (statIndex = 5)
        int lastFireIdx = rootIdx;
        int frTarget = 0;
        int fireMajor1 = -1;
        for (int g = 0; g < 3; g++) {
            for (int i = 0; i < 2; i++) {
                frTarget++;
                StatNode n = new StatNode(5, 1, frTarget, cx + 400 + g * 600 + i * 130, cy + 450 + i * 40 - 20,
                        lastFireIdx, red, false);
                list.add(n);
                lastFireIdx = list.size() - 1;
            }
            frTarget += 3;
            StatNode major = new StatNode(5, 3, frTarget, cx + 750 + g * 600, cy + 450, lastFireIdx, red, true);
            list.add(major);
            lastFireIdx = list.size() - 1;
            if (g == 0)
                fireMajor1 = lastFireIdx;
        }

        // EVOLUTIONS
        double dist = 1000;
        list.add(createEvoNode("Vampiric", (int) (cx + dist * Math.cos(Math.toRadians(-67.5))),
                (int) (cy + dist * Math.sin(Math.toRadians(-67.5))), mightMajor1, hpMajor1, Color.MAGENTA));
        list.add(createEvoNode("Deadly Focus", (int) (cx + dist * Math.cos(Math.toRadians(-22.5))),
                (int) (cy + dist * Math.sin(Math.toRadians(-22.5))), mightMajor1, critMajor1, Color.MAGENTA));
        list.add(createEvoNode("Phantom Dash", (int) (cx + dist * Math.cos(Math.toRadians(-157.5))),
                (int) (cy + dist * Math.sin(Math.toRadians(-157.5))), dashMajor1, speedMajor1, Color.MAGENTA));
        list.add(createEvoNode("Adrenaline", (int) (cx + dist * Math.cos(Math.toRadians(-112.5))),
                (int) (cy + dist * Math.sin(Math.toRadians(-112.5))), speedMajor1, hpMajor1, Color.MAGENTA));

        // Frenzy nối vào Major 1 của cả 2 nhánh (X khoảng 800, Y giữa cy và cy+450)
        list.add(createEvoNode("Frenzy", cx + 800, cy + 225, fireMajor1, critMajor1, Color.MAGENTA));

        nodes = list.toArray(new StatNode[0]);
    }

    private StatNode createEvoNode(String name, int x, int y, int p1, int p2, Color c) {
        StatNode n = new StatNode(-1, 0, 5, x, y, p1, c, false);
        n.isEvolution = true;
        n.parentIdx2 = p2;
        n.specialName = name;
        return n;
    }

    public static int calculateEvoCost(String name, int currentLv) {
        int[] costs = { 30, 60, 100, 160, 250 };
        if (currentLv < 5)
            return costs[currentLv];
        return 9999;
    }

    private int calculateNodeCost(StatNode node, int currentLv) {
        if (node.statIndex == 0) {
            if (node.levelTarget == 1)
                return 3000;
            if (node.levelTarget == 2)
                return 7000;
            if (node.levelTarget == 3)
                return 12000;
        }
        int[] bases = { 110, 180, 160, 130, 240, 280 };
        int sum = 0;
        for (int i = 0; i < node.bonusAmount; i++)
            sum += (int) (bases[node.statIndex] * Math.pow(PRICE_MULTIPLIER, currentLv + i));
        return sum;
    }

    private boolean isNodeUnlocked(StatNode node) {
        if (node.isEvolution)
            return getEvolutionLevel(node.specialName) >= 5;
        return getPlayerDataLevel(node.statIndex) >= node.levelTarget;
    }

    public static int getEvolutionLevel(String name) {
        if (name.equals("Vampiric"))
            return PlayerData.evoVampirism;
        if (name.equals("Deadly Focus") || name.equals("Bloodlust"))
            return PlayerData.evoBloodlust;
        if (name.equals("Phantom Dash"))
            return PlayerData.evoPhantomDash;
        if (name.equals("Adrenaline") || name.equals("Berserker"))
            return PlayerData.evoBerserker;
        if (name.equals("Frenzy"))
            return PlayerData.evoFrenzy;
        return 0;
    }

    private void upgradeEvolution(String name) {
        if (name.equals("Vampiric"))
            PlayerData.evoVampirism++;
        else if (name.equals("Deadly Focus") || name.equals("Bloodlust"))
            PlayerData.evoBloodlust++;
        else if (name.equals("Phantom Dash"))
            PlayerData.evoPhantomDash++;
        else if (name.equals("Adrenaline") || name.equals("Berserker"))
            PlayerData.evoBerserker++;
        else if (name.equals("Frenzy"))
            PlayerData.evoFrenzy++;
        PlayerData.save();
    }

    private int getPlayerDataLevel(int idx) {
        if (idx == 0)
            return PlayerData.statHealthLevel;
        if (idx == 1)
            return PlayerData.statDamageLevel;
        if (idx == 2)
            return PlayerData.statSpeedLevel;
        if (idx == 3)
            return PlayerData.statDashLevel;
        if (idx == 4)
            return PlayerData.statCritLevel;
        if (idx == 5)
            return PlayerData.statCooldownLevel;
        return 0;
    }

    private void upgradeStat(int idx, int amount) {
        if (idx == 0)
            PlayerData.statHealthLevel += amount;
        else if (idx == 1)
            PlayerData.statDamageLevel += amount;
        else if (idx == 2)
            PlayerData.statSpeedLevel += amount;
        else if (idx == 3)
            PlayerData.statDashLevel += amount;
        else if (idx == 4)
            PlayerData.statCritLevel += amount;
        else if (idx == 5)
            PlayerData.statCooldownLevel += amount;

        // Check for wealth_peak achievement
        int currentLv = getPlayerDataLevel(idx);
        boolean isMax = false;
         if (idx == 0 && currentLv >= 3) isMax = true;
        else if (idx == 1 && currentLv >= 30) isMax = true;
        else if (idx == 2 && currentLv >= 10) isMax = true;
        else if (idx == 3 && currentLv >= 10) isMax = true;
        else if (idx == 4 && currentLv >= 15) isMax = true;
        else if (idx == 5 && currentLv >= 15) isMax = true;

        if (isMax) {
            gameproject.meta.AchievementManager.getInstance().onStatChanged("wealth", 1, "peak");
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        gameproject.ui.StatsUI.draw(g, game.screenWidth, game.screenHeight, statNames, statDescs, PlayerData.gold,
                nodes, game.input.mouseX, game.input.mouseY, game.input.isMouseHolding);
    }
}
