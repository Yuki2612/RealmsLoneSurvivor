package gameproject;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import gameproject.entity.EntityManager;
import gameproject.weapon.Weapon;
import gameproject.weapon.Pistol;
import gameproject.skill.PassiveSkill;
import gameproject.state.State;
import gameproject.state.MenuState;
import gameproject.state.GameOverState;
import gameproject.state.WeaponSelectState;
import gameproject.state.CharacterSelectState;
import gameproject.state.SettingsState;
import gameproject.state.GuideState;
import gameproject.state.SkillsState;
import gameproject.state.StatsState;
import gameproject.state.VictoryState;
import gameproject.meta.PlayerData;
import gameproject.meta.CharacterClass;
import gameproject.environment.MapManager;
import gameproject.environment.Building;

public class GamePanel extends JPanel implements Runnable {
    public static GamePanel instance;
    private Thread gameThread;
    private final int FPS = 60;
    public static boolean showHitboxes = false;

    public int screenWidth = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    public int screenHeight = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    public static final int WORLD_WIDTH = 6000;
    public static final int WORLD_HEIGHT = 6000;

    public float cameraX, cameraY;

    public InputManager input;
    public UpgradeManager upgradeManager;
    public EntityManager entityManager;
    public VFXManager vfxManager;

    public Player player;
    public Weapon currentWeapon;
    public List<PassiveSkill> activeSkills;

    public int score;
    public long startTime;
    public int surviveTimeSeconds;

    public String currentBgKey = "background1";
    public int totalBackgrounds = 0;

    public int activeBossCount = 0;
    public int camIntX, camIntY;

    public MapManager mapManager;
    public List<Building> buildings;

    private State currentState;
    public int currentFPS = 0;

    // --- Hệ thống Thời gian Game (Managed Game Clock) ---
    private static long totalPausedTime = 0;
    private static long pauseStartTime = 0;
    private static boolean isPaused = false;

    public static long getTickTime() {
        if (isPaused)
            return pauseStartTime - totalPausedTime;
        return System.currentTimeMillis() - totalPausedTime;
    }

    public static void pauseGame() {
        if (!isPaused) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();
        }
    }

    public static void resumeGame() {
        if (isPaused) {
            totalPausedTime += (System.currentTimeMillis() - pauseStartTime);
            isPaused = false;
        }
    }

    public static void resetTime() {
        isPaused = false;
        totalPausedTime = 0;
        pauseStartTime = 0;
    }

    public GamePanel() {
        instance = this;
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setFocusable(true);

        input = new InputManager(this);
        addKeyListener(input);
        addMouseListener(input);
        addMouseMotionListener(input);

        upgradeManager = new UpgradeManager();
        entityManager = new EntityManager();
        vfxManager = new VFXManager();
        activeSkills = new ArrayList<>();
        buildings = new ArrayList<>();
        currentWeapon = new Pistol();

        // --- Ưu tiên nạp và phát nhạc Menu ngay lập tức để tránh delay ---
        SoundManager.loadMusic("menubgm", "app/res/menubgm.wav");
        SoundManager.playMusic("menubgm");

        // --- Sau đó nạp các tài nguyên khác ---
        SoundManager.load("shoot", "app/res/shoot.wav");
        SoundManager.load("hit", "app/res/hit.wav");
        SoundManager.load("explosion", "app/res/explosion.wav");
        SoundManager.load("levelup", "app/res/levelup.wav");
        SoundManager.load("laser", "app/res/laser.wav");
        SoundManager.load("shield", "app/res/shield.wav");
        SoundManager.load("pickup", "app/res/pickup.wav");
        SoundManager.load("achievement", "app/res/achievement.wav");

        // Load player hurt sounds (giả định có 3 file)
        for (int i = 1; i <= 3; i++) {
            SoundManager.load("playerhurt" + i, "app/res/player" + i + "hurt.wav");
        }

        SoundManager.loadMusic("gamebgm1", "app/res/gamebgm1.wav");
        SoundManager.loadMusic("gamebgm2", "app/res/gamebgm2.wav");
        SoundManager.loadMusic("bossbgm", "app/res/bossbgm.wav");

        FontManager.load("app/res/pixel_font.ttf");
        ImageManager.load("heart", "app/res/heart.png");

        int bgIndex = 1;
        while (true) {
            String path = "app/res/background" + bgIndex + ".png";
            if (new java.io.File(path).exists()) {
                ImageManager.load("background" + bgIndex, path);
                totalBackgrounds++;
                bgIndex++;
            } else
                break;
        }

        ImageManager.load("skill_explosive", "app/res/skill_explosive.png");
        ImageManager.load("skill_explosive_bullets", "app/res/skill_explosive.png");
        ImageManager.load("player", "app/res/player.png");
        for (int i = 1; i <= 5; i++) {
            ImageManager.load("player" + i, "app/res/player" + i + ".png");
            ImageManager.load("enemy" + i, "app/res/enemy" + i + ".png");

            String prefix = "player" + i;
            // NẠP ANIMATION LINH HOẠT: Ưu tiên đọc số frame từ tên file (ví dụ: _f15.png)
            // Nếu không có, sẽ tự động dùng số frame mặc định bên dưới.
            ImageManager.loadAnimation(prefix + "_idle_side", "app/res/" + prefix + "_idle_side.png", 16);
            ImageManager.loadAnimation(prefix + "_run_side", "app/res/" + prefix + "_run_side.png", 10);
            ImageManager.loadAnimation(prefix + "_idle_down", "app/res/" + prefix + "_idle_down.png", 10);
            ImageManager.loadAnimation(prefix + "_run_down", "app/res/" + prefix + "_run_down.png", 12);
            ImageManager.loadAnimation(prefix + "_idle_up", "app/res/" + prefix + "_idle_up.png", 10);
            ImageManager.loadAnimation(prefix + "_run_up", "app/res/" + prefix + "_run_up.png", 12);
        }
        for (int i = 1; i <= 3; i++) {
            String bKey = "boss" + i;
            ImageManager.load(bKey, "app/res/" + bKey + ".png");
            // Tự động thử nạp các bộ animation nếu tồn tại (Smart Search sẽ lo phần _f8)
            ImageManager.loadAnimation(bKey + "_idle", "app/res/" + bKey + "_idle.png");
            ImageManager.loadAnimation(bKey + "_attack", "app/res/" + bKey + "_attack.png");
            ImageManager.loadAnimation(bKey + "_dash", "app/res/" + bKey + "_dash.png");
            ImageManager.loadAnimation(bKey + "_death", "app/res/" + bKey + "_death.png");
        }
        // Boss 4 - Dark Fairy (có 2 phase, animation riêng cho mỗi phase)
        ImageManager.loadAnimation("boss4_move", "app/res/boss4_move.png");
        ImageManager.loadAnimation("boss4_cast", "app/res/boss4_cast.png");
        ImageManager.loadAnimation("boss4_death", "app/res/boss4_death.png");
        ImageManager.loadAnimation("boss4_move2", "app/res/boss4_move2.png");
        ImageManager.loadAnimation("boss4_cast2", "app/res/boss4_cast2.png");
        ImageManager.loadAnimation("boss4_teleport", "app/res/boss4_teleport.png");
        ImageManager.loadAnimation("boss4_transform", "app/res/boss4_transform.png");


        ImageManager.load("chest1", "app/res/chest1.png");
        ImageManager.load("chest2", "app/res/chest2.png");
        ImageManager.load("gold", "app/res/gold.png");
        ImageManager.load("soul", "app/res/soul.png");
        ImageManager.load("wall", "app/res/wall.png");
        ImageManager.load("tree", "app/res/tree.png");
        ImageManager.load("rock", "app/res/rock.png");
        ImageManager.load("woodencrate", "app/res/woodencrate.png");
        ImageManager.load("roof", "app/res/roof.png");
        ImageManager.load("floor", "app/res/floor.png");
        ImageManager.load("treasure", "app/res/treasure.png");
        ImageManager.load("mimic", "app/res/mimic.png");
        ImageManager.load("boss_hud", "app/res/boss_hud.png");
        ImageManager.load("enemy_wizard", "app/res/enemy_wizard.png");
        ImageManager.load("enemy_assassin", "app/res/enemy_assassin.png");
        ImageManager.load("enemy_shooter", "app/res/enemy_shooter.png");
        
        ImageManager.load("shotgun", "app/res/shotgun.png");
        ImageManager.load("sniper_rifle", "app/res/sniper_rifle.png");
        ImageManager.load("assault_rifle", "app/res/assault_rifle.png");

        // Load Skill Icons
        for (gameproject.skill.Upgrade u : gameproject.skill.Upgrade.values()) {
            String fileName = u.name().toLowerCase();
            if (u == gameproject.skill.Upgrade.SHIELD)
                fileName = "hp";
            else if (u == gameproject.skill.Upgrade.OPTICAL_SCOPE)
                fileName = "range";

            ImageManager.load("skill_" + u.name().toLowerCase(), "app/res/skill_" + fileName + ".png");
        }

        PlayerData.load();

        buildings = new ArrayList<>();
        mapManager = new MapManager(WORLD_WIDTH, WORLD_HEIGHT, buildings);

        changeState(new MenuState());

        gameThread = new Thread(this);
        gameThread.start();
    }

    public void changeState(State state) {
        // Quản lý dừng/tiếp tục thời gian game dựa trên State
        if (state instanceof gameproject.state.PlayingState) {
            resumeGame();
        } else if (state instanceof gameproject.state.LevelUpState ||
                state instanceof gameproject.state.WeaponSelectState ||
                state instanceof gameproject.state.PauseState) {
            pauseGame();
        }

        this.currentState = state;
        updateMusic();
    }

    private void updateMusic() {
        if (currentState instanceof MenuState || currentState instanceof CharacterSelectState ||
                currentState instanceof SettingsState || currentState instanceof GuideState ||
                currentState instanceof StatsState || currentState instanceof SkillsState) {
            SoundManager.playMusic("menubgm");
        } else if (currentState instanceof gameproject.state.PlayingState ||
                currentState instanceof gameproject.state.LevelUpState ||
                currentState instanceof gameproject.state.WeaponSelectState) {
            // Logic nhạc trong gameplay
            if (entityManager != null && entityManager.activeBossCount > 0) {
                SoundManager.playMusic("bossbgm");
            } else if (entityManager != null && entityManager.waveCount >= 7) {
                SoundManager.playMusic("gamebgm2");
            } else {
                SoundManager.playMusic("gamebgm1");
            }
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public void startNewGame() {
        resetTime();
        gameproject.state.PlayingState.resetEvents();
        CharacterClass charClass = PlayerData.selectedClass;

        // Tạo mới bản đồ cho mỗi lượt chơi để tăng tính ngẫu nhiên (Roguelike
        // experience)
        synchronized (buildings) {
            buildings.clear();
            mapManager = new MapManager(WORLD_WIDTH, WORLD_HEIGHT, buildings);
        }

        player = new Player(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, charClass);
        cameraX = player.getX() - screenWidth / 2f;
        cameraY = player.getY() - screenHeight / 2f;
        score = 0;
        activeSkills.clear();
        currentWeapon = new Pistol();
        vfxManager.clearAll();

        long currentTime = getTickTime();
        startTime = currentTime;
        // Adjust survive time based on wave (approx 15s per wave) for scaling
        surviveTimeSeconds = (PlayerData.debugStartWave - 1) * 15;

        upgradeManager.startNewGame(PlayerData.debugStartLevel);
        upgradeManager.playerDamage = (int) ((10 + gameproject.meta.PlayerData.statDamageLevel)
                * charClass.damageMulti);
        entityManager.startNewGame(currentTime, PlayerData.debugStartWave);

        if (totalBackgrounds > 0) {
            currentBgKey = "background" + (new java.util.Random().nextInt(totalBackgrounds) + 1);
        }

        if (charClass.startingUpgrade != null) {
            upgradeManager.applyUpgrade(charClass.startingUpgrade, player, activeSkills, currentWeapon);
        }

        changeState(new gameproject.state.PlayingState());
    }

    public void triggerVictory(boolean noHit, boolean noDash) {
        PlayerData.save();
        gameproject.meta.AchievementManager.getInstance().onVictory(surviveTimeSeconds, noHit, noDash);
        changeState(new VictoryState(score, entityManager.waveCount, surviveTimeSeconds, currentWeapon.name, player, activeSkills));
    }

    public void triggerGameOver() {
        PlayerData.save();
        gameproject.meta.AchievementManager.getInstance().addDeath();
        changeState(new GameOverState(score, entityManager.waveCount, currentWeapon.name, player, activeSkills));
    }

    public void openWeaponSelect() {
        player.resetMovement();
        input.isMouseHolding = false;
        changeState(new WeaponSelectState());
    }

    public void triggerBreakthroughUpgrade() {
        upgradeManager.generateBreakthroughOptions(player);
        player.resetMovement();
        input.isMouseHolding = false;
        gameproject.SoundManager.play("levelup");
        changeState(new gameproject.state.LevelUpState());
    }

    public void triggerNormalUpgrade() {
        upgradeManager.generateNormalOptions(player);
        player.resetMovement();
        input.isMouseHolding = false;
        gameproject.SoundManager.play("levelup");
        changeState(new gameproject.state.LevelUpState());
    }

    public void addScoreAndExp(int amount) {
        score += amount;
        upgradeManager.addExp(amount);
    }

    @Override
    public void run() {
        double timePerFrame = 1000000000.0 / FPS;
        long lastFrame = System.nanoTime();
        long lastFPSCheck = System.currentTimeMillis();
        int frameCount = 0;

        while (true) {
            if (System.nanoTime() - lastFrame >= timePerFrame) {
                SoundManager.updateLoopFading();
                updateMusic();
                if (currentState != null) {
                    currentState.update(this);
                }
                input.clearClickAndKey();

                repaint();
                lastFrame = System.nanoTime();
                frameCount++;

                if (System.currentTimeMillis() - lastFPSCheck >= 1000) {
                    currentFPS = frameCount;
                    frameCount = 0;
                    lastFPSCheck = System.currentTimeMillis();
                }

                // Cập nhật Thành tựu toàn cục (cho Popup thông báo)
                gameproject.meta.AchievementManager.getInstance().update(System.currentTimeMillis());
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentState != null) {
            currentState.render(this, g);
        }
        
        // Vẽ thông báo Thành tựu ở lớp trên cùng của mọi màn hình
        gameproject.meta.AchievementManager.getInstance().drawToast(g, screenWidth, screenHeight);
    }
}