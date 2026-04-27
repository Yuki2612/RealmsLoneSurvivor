package gameproject;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GamePanel extends JPanel implements Runnable, KeyListener, MouseListener {
    private long menuOpenTime = 0;
    private Thread gameThread;
    private final int FPS = 60;
    private GameState currentState;
    private Player player;

    private int screenWidth = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private int screenHeight = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    // --- HỆ THỐNG MANAGER ---
    private EntityManager entityManager;
    private VFXManager vfxManager;
    private Weapon currentWeapon;
    private List<PassiveSkill> activeSkills;

    // --- BIẾN HỆ THỐNG TRÒ CHƠI ---
    private int score;
    private long startTime;
    private int surviveTimeSeconds;

    private int playerLevel;
    private int currentExp;
    private int expToNextLevel;

    private int playerDamage;
    private float bulletSpeedMulti;

    private Upgrade[] currentUpgradeOptions;

    public GamePanel() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        addKeyListener(this);
        addMouseListener(this);
        setFocusable(true);
        currentState = GameState.MENU;

        SoundManager.load("shoot", "app/res/shoot.wav");
        SoundManager.load("hit", "app/res/hit.wav");
        SoundManager.load("explosion", "app/res/explosion.wav");
        SoundManager.load("levelup", "app/res/levelup.wav");

        entityManager = new EntityManager();
        vfxManager = new VFXManager();
        activeSkills = new ArrayList<>();
        currentWeapon = new Pistol();

        gameThread = new Thread(this);
        gameThread.start();
    }

    private void startNewGame() {
        player = new Player(screenWidth / 2, screenHeight / 2);
        score = 0;
        playerDamage = 10;
        bulletSpeedMulti = 1.0f;
        playerLevel = 1;
        currentExp = 0;
        expToNextLevel = 100;

        activeSkills.clear();
        currentWeapon = new Pistol();
        vfxManager.fireZones.clear();

        long currentTime = System.currentTimeMillis();
        startTime = currentTime;
        surviveTimeSeconds = 0;

        entityManager.startNewGame(currentTime);
        currentState = GameState.PLAYING;
    }

    public void triggerGameOver() {
        currentState = GameState.GAME_OVER;
    }

    public void addScoreAndExp(int amount) {
        score += amount;
        currentExp += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        if (currentExp >= expToNextLevel) {
            currentExp -= expToNextLevel;
            playerLevel++;
            expToNextLevel = (int) (100 * Math.pow(1.25, playerLevel - 1));

            if (playerLevel % 3 == 0) {
                List<Upgrade> owned = player.getOwnedBreakthroughs();
                List<Upgrade> options = new ArrayList<>(owned);

                if (owned.size() < 3) {
                    List<Upgrade> unowned = new ArrayList<>();
                    for (Upgrade u : Upgrade.values()) {
                        if (u.isBreakthrough && !owned.contains(u))
                            unowned.add(u);
                    }
                    Collections.shuffle(unowned);
                    for (Upgrade u : unowned) {
                        if (options.size() < 3)
                            options.add(u);
                    }
                }
                currentUpgradeOptions = new Upgrade[] { options.get(0), options.get(1), options.get(2) };
            } else {
                List<Upgrade> normals = new ArrayList<>();
                for (Upgrade u : Upgrade.values())
                    if (!u.isBreakthrough)
                        normals.add(u);
                Collections.shuffle(normals);
                currentUpgradeOptions = new Upgrade[] { normals.get(0), normals.get(1), normals.get(2) };
            }

            player.resetMovement();
            menuOpenTime = System.currentTimeMillis();
            SoundManager.play("levelup");
            currentState = GameState.LEVEL_UP;
        }
    }

    private void applyUpgrade(Upgrade upgrade) {
        if (upgrade.isBreakthrough) {
            player.levelUpBreakthrough(upgrade);
            boolean hasSkill = false;
            for (PassiveSkill s : activeSkills) {
                if ((upgrade == Upgrade.ORBITING_ORBS && s instanceof OrbitingOrbsSkill) ||
                        (upgrade == Upgrade.TRAIL_OF_FIRE && s instanceof TrailOfFireSkill) ||
                        (upgrade == Upgrade.FROST_AURA && s instanceof FrostAuraSkill) ||
                        (upgrade == Upgrade.EXPLOSIVE_CORPSE && s instanceof ExplosiveCorpseSkill)) {
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
            }
        } else {
            switch (upgrade) {
                case SHIELD -> player.addHeart();
                case DAMAGE -> playerDamage += 5;
                case FIRE_RATE -> currentWeapon.cooldown = (long) (currentWeapon.cooldown * 0.85);
                case MOVE_SPEED -> player.upgradeSpeed(0.5f);
                case DASH_COOLDOWN -> player.upgradeDashCooldown(400);
                case BULLET_SPEED -> bulletSpeedMulti += 0.2f;
                default -> {
                }
            }
        }
        currentState = GameState.PLAYING;
    }

    @Override
    public void run() {
        double timePerFrame = 1000000000.0 / FPS;
        long lastFrame = System.nanoTime();
        while (true) {
            if (System.nanoTime() - lastFrame >= timePerFrame) {
                if (currentState == GameState.PLAYING)
                    updateGame();
                repaint();
                lastFrame = System.nanoTime();
            }
        }
    }

    private void updateGame() {
        long currentTime = System.currentTimeMillis();
        surviveTimeSeconds = (int) ((currentTime - startTime) / 500);

        player.update(screenWidth, screenHeight);
        vfxManager.update(currentTime);
        entityManager.update(player, vfxManager, activeSkills, screenWidth, screenHeight, currentTime,
                surviveTimeSeconds, this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (currentState == GameState.MENU) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, screenWidth, screenHeight);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 60));
            g.drawString("PIXEL SURVIVOR", screenWidth / 2 - 250, screenHeight / 2 - 200);

            g.setFont(new Font("Arial", Font.BOLD, 30));
            int btnX = screenWidth / 2 - 100;
            g.drawRect(btnX, screenHeight / 2 - 100, 200, 50);
            g.drawString("START", btnX + 50, screenHeight / 2 - 65);
            g.drawRect(btnX, screenHeight / 2 - 30, 200, 50);
            g.drawString("SETTINGS", btnX + 25, screenHeight / 2 + 5);
            g.drawRect(btnX, screenHeight / 2 + 40, 200, 50);
            g.drawString("QUIT", btnX + 65, screenHeight / 2 + 75);
            return;
        }

        if (currentState == GameState.SETTINGS) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, screenWidth, screenHeight);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("SETTINGS", screenWidth / 2 - 120, screenHeight / 2 - 150);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Tính năng đang phát triển...", screenWidth / 2 - 130, screenHeight / 2);
            g.drawString("Bấm ESC để quay lại Menu", screenWidth / 2 - 120, screenHeight / 2 + 200);
            return;
        }

        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, screenWidth, screenHeight);

        Graphics2D g2d = (Graphics2D) g;
        vfxManager.applyScreenShake(g2d);

        // Lớp dưới: Frost Aura
        for (PassiveSkill skill : activeSkills) {
            if (skill instanceof FrostAuraSkill)
                skill.draw(g, player);
        }

        vfxManager.draw(g, player);
        entityManager.draw(g);
        player.draw(g);

        // Lớp trên: Orbiting Orbs
        for (PassiveSkill skill : activeSkills) {
            if (skill instanceof OrbitingOrbsSkill)
                skill.draw(g, player);
        }

        vfxManager.resetScreenShake(g2d);

        // --- UI CHỈ SỐ ---
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);
        g.drawString("Wave: " + entityManager.waveCount, 150, 20);
        g.drawString("ATK: " + playerDamage, screenWidth - 120, 20);
        g.drawString("Fire Rate: " + currentWeapon.cooldown + "ms", screenWidth - 170, 45);

        g.drawString("HP:", 10, 95);
        for (int i = 0; i < player.getHearts(); i++) {
            g.setColor(Color.RED);
            int hX = 40 + (i * 25);
            int hY = 82;
            g.fillOval(hX, hY, 10, 10);
            g.fillOval(hX + 7, hY, 10, 10);
            g.fillRect(hX + 3, hY + 5, 11, 10);
        }

        long timeSinceLastDash = System.currentTimeMillis() - player.getLastDashTime();
        g.drawString("Dash:", 10, 120);
        if (timeSinceLastDash >= player.getDashCooldown()) {
            g.setColor(Color.GREEN);
            g.drawString("READY", 60, 120);
        } else {
            g.setColor(Color.RED);
            g.drawString("Wait", 60, 120);
        }

        int barHeight = 20;
        int barWidth = screenWidth - 40;
        int barX = 20;
        int barY = screenHeight - 40;
        g.setColor(Color.BLACK);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(new Color(0, 200, 255));
        g.fillRect(barX, barY, (int) (((float) currentExp / expToNextLevel) * barWidth), barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Lv." + playerLevel + "  [" + currentExp + " / " + expToNextLevel + " EXP]", screenWidth / 2 - 100,
                barY + 15);

        if (currentState == GameState.LEVEL_UP) {
            g.setColor(new Color(0, 0, 0, 210));
            g.fillRect(0, 0, screenWidth, screenHeight);
            g.setColor(playerLevel % 3 == 0 ? Color.MAGENTA : Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString(playerLevel % 3 == 0 ? "NÂNG CẤP ĐỘT PHÁ" : "CHỌN CHỈ SỐ", screenWidth / 2 - 200,
                    screenHeight / 2 - 200);

            int boxWidth = 200;
            int boxHeight = 250;
            int spacing = 40;
            int startX = (screenWidth - (3 * boxWidth + 2 * spacing)) / 2;
            int by = (screenHeight - boxHeight) / 2;

            for (int i = 0; i < 3; i++) {
                int bx = startX + i * (boxWidth + spacing);
                Upgrade u = currentUpgradeOptions[i];

                g.setColor(Color.DARK_GRAY);
                g.fillRect(bx, by, boxWidth, boxHeight);
                g.setColor(Color.WHITE);
                g.drawRect(bx, by, boxWidth, boxHeight);
                g.drawRect(bx, by, boxWidth, 100);

                g.setFont(new Font("Arial", Font.ITALIC, 16));
                g.drawString("[IMAGE 2/5]", bx + 55, by + 55);

                g.setFont(new Font("Arial", Font.BOLD, 18));
                g.drawString("Click để chọn", bx + 10, by + 130);

                g.setFont(new Font("Arial", Font.PLAIN, 14));
                String[] parts = u.description.split("\\(");
                g.drawString(parts[0], bx + 10, by + 160);
                if (parts.length > 1) {
                    g.setColor(Color.CYAN);
                    g.drawString("(" + parts[1], bx + 10, by + 185);
                }

                if (u.isBreakthrough) {
                    g.setColor(Color.YELLOW);
                    g.drawString("Level hiện tại: " + player.getBreakthroughLevel(u), bx + 10, by + 225);
                }
            }
        }

        if (currentState == GameState.PAUSED) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, screenWidth, screenHeight);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 60));
            g.drawString("PAUSED", screenWidth / 2 - 120, screenHeight / 2 - 150);

            g.setFont(new Font("Arial", Font.BOLD, 30));
            int btnX = screenWidth / 2 - 100;

            g.drawRect(btnX, screenHeight / 2 - 50, 200, 50);
            g.drawString("RESUME", btnX + 35, screenHeight / 2 - 15);
            g.drawRect(btnX, screenHeight / 2 + 20, 200, 50);
            g.drawString("MAIN MENU", btnX + 10, screenHeight / 2 + 55);
            g.drawRect(btnX, screenHeight / 2 + 90, 200, 50);
            g.drawString("QUIT", btnX + 65, screenHeight / 2 + 125);
        }

        if (currentState == GameState.GAME_OVER) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, screenWidth, screenHeight);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 60));
            g.drawString("GAME OVER", screenWidth / 2 - 170, screenHeight / 2 - 50);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 30));
            g.drawString("Press 'R' to Restart", screenWidth / 2 - 130, screenHeight / 2 + 50);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (currentState == GameState.SETTINGS && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            currentState = GameState.MENU;
            return;
        }
        if (currentState == GameState.GAME_OVER && e.getKeyCode() == KeyEvent.VK_R) {
            startNewGame();
            return;
        }
        if (currentState == GameState.PLAYING && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            player.resetMovement();
            currentState = GameState.PAUSED;
            return;
        }
        if (currentState == GameState.PAUSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            currentState = GameState.PLAYING;
            return;
        }
        if (currentState == GameState.PLAYING)
            player.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (currentState == GameState.PLAYING)
            player.keyReleased(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        if (currentState == GameState.MENU) {
            int btnX = screenWidth / 2 - 100;
            if (mx >= btnX && mx <= btnX + 200) {
                if (my >= screenHeight / 2 - 100 && my <= screenHeight / 2 - 50)
                    startNewGame();
                else if (my >= screenHeight / 2 - 30 && my <= screenHeight / 2 + 20)
                    currentState = GameState.SETTINGS;
                else if (my >= screenHeight / 2 + 40 && my <= screenHeight / 2 + 90)
                    System.exit(0);
            }
            return;
        }

        if (currentState == GameState.LEVEL_UP) {
            if (System.currentTimeMillis() - menuOpenTime < 500)
                return;
            int boxWidth = 200, spacing = 40;
            int startX = (screenWidth - (3 * boxWidth + 2 * spacing)) / 2;
            int by = (screenHeight - 250) / 2;

            for (int i = 0; i < 3; i++) {
                int bx = startX + i * (boxWidth + spacing);
                if (mx >= bx && mx <= bx + boxWidth && my >= by && my <= by + 250) {
                    applyUpgrade(currentUpgradeOptions[i]);
                    return;
                }
            }
            return;
        }

        if (currentState == GameState.PAUSED) {
            int btnX = screenWidth / 2 - 100;
            if (mx >= btnX && mx <= btnX + 200) {
                if (my >= screenHeight / 2 - 50 && my <= screenHeight / 2)
                    currentState = GameState.PLAYING;
                else if (my >= screenHeight / 2 + 20 && my <= screenHeight / 2 + 70)
                    currentState = GameState.MENU;
                else if (my >= screenHeight / 2 + 90 && my <= screenHeight / 2 + 140)
                    System.exit(0);
            }
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1 && currentState == GameState.PLAYING) {
            if (currentWeapon.canShoot()) {
                currentWeapon.shoot(player.getX(), player.getY(), mx, my, bulletSpeedMulti, playerDamage - 10,
                        player.getBreakthroughLevel(Upgrade.CHAIN_LIGHTNING), entityManager.projectiles);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}