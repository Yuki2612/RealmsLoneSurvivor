package gameproject.state;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import gameproject.GamePanel;
import gameproject.FontManager;
import gameproject.meta.PlayerData;

public class StatsState implements State {
    private String[] statNames = {
        "Max HP", 
        "Might", 
        "Move Speed", 
        "Dash Cool",
        "Crit Chance",
        "Fire Rate"
    };
    private String[] statDescs = {
        "(+1/10lv)", "(+1 Dmg)", "(+2%)", "(-2% CD)", "(+1%)", "(+2%)"
    };
    private int[] statLevels = new int[6];
    private int[] maxLevels = {30, 20, 10, 10, 20, 10};

    private static int getCost(int idx, int level) {
        int[] baseCosts = {80, 250, 120, 100, 180, 220};
        return (int)(baseCosts[idx] * Math.pow(1.1, level));
    }

    public class StatNode {
        public int statIndex;
        public int cx, cy;
        public int[] required;
        public StatNode(int statIndex, int cx, int cy, int[] required) {
            this.statIndex = statIndex;
            this.cx = cx; this.cy = cy; this.required = required;
        }
    }

    private StatNode[] nodes;

    @Override
    public void update(GamePanel game) {
        if (nodes == null) {
            int cx = game.screenWidth / 2;
            nodes = new StatNode[] {
                new StatNode(1, cx, 200, new int[]{}),              // Might
                new StatNode(0, cx - 220, 320, new int[]{1}),       // Health
                new StatNode(2, cx + 220, 320, new int[]{1}),       // Speed
                new StatNode(3, cx - 220, 460, new int[]{0}),       // Dash
                new StatNode(5, cx + 220, 460, new int[]{2}),       // Cooldown
                new StatNode(4, cx, 580, new int[]{3, 5})           // Crit
            };
        }

        // Đọc lại từ PlayerData
        statLevels[0] = PlayerData.statHealthLevel;
        statLevels[1] = PlayerData.statDamageLevel;
        statLevels[2] = PlayerData.statSpeedLevel;
        statLevels[3] = PlayerData.statDashLevel;
        statLevels[4] = PlayerData.statCritLevel;
        statLevels[5] = PlayerData.statCooldownLevel;

        if (game.input.escPressed) {
            PlayerData.save();
            game.changeState(new MenuState());
            game.input.clearClickAndKey();
            return;
        }

        if (game.input.mouseClicked) {
            int mx = game.input.mouseX;
            int my = game.input.mouseY;

            for (StatNode node : nodes) {
                int r = 55;
                double dist = Math.sqrt(Math.pow(mx - node.cx, 2) + Math.pow(my - node.cy, 2));

                if (dist <= r) {
                    boolean canUnlock = true;
                    for (int req : node.required) {
                        if (statLevels[req] == 0) canUnlock = false;
                    }
                    if (canUnlock) {
                        // SỬ DỤNG HÀM getCost VỚI HỆ SỐ 1.1
                        int cost = getCost(node.statIndex, statLevels[node.statIndex]);
                        if (statLevels[node.statIndex] < maxLevels[node.statIndex] && PlayerData.gold >= cost) {
                            PlayerData.gold -= cost;
                            if (node.statIndex == 0) PlayerData.statHealthLevel++;
                            else if (node.statIndex == 1) PlayerData.statDamageLevel++;
                            else if (node.statIndex == 2) PlayerData.statSpeedLevel++;
                            else if (node.statIndex == 3) PlayerData.statDashLevel++;
                            else if (node.statIndex == 4) PlayerData.statCritLevel++;
                            else if (node.statIndex == 5) PlayerData.statCooldownLevel++;
                            statLevels[node.statIndex]++;
                            gameproject.SoundManager.play("shoot");
                        }
                    }
                }
            }

            if (mx >= 50 && mx <= 170 && my >= 50 && my <= 95) {
                PlayerData.save();
                game.changeState(new MenuState());
            }
            
            game.input.clearClickAndKey();
        }
    }

    @Override
    public void render(GamePanel game, Graphics g) {
        gameproject.ui.StatsUI.draw(g, game.screenWidth, game.screenHeight, statNames, statDescs, statLevels, maxLevels, PlayerData.gold, nodes, game.input.mouseX, game.input.mouseY);
    }
}
