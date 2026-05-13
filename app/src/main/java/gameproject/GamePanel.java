package gameproject;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
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
import gameproject.ui.ParallaxBackground;

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
    public gameproject.ui.DialogueManager dialogueManager;

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
    public gameproject.environment.MapConfig currentMapConfig;
    public ParallaxBackground menuParallax;

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

        entityManager = new EntityManager();
        vfxManager = new VFXManager();
        dialogueManager = new gameproject.ui.DialogueManager();
        upgradeManager = new UpgradeManager(this);
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
        SoundManager.load("water_splash", "app/res/water_splash.wav");

        // Load player hurt sounds (giả định có 3 file)
        for (int i = 1; i <= 3; i++) {
            SoundManager.load("playerhurt" + i, "app/res/player" + i + "hurt.wav");
        }

        SoundManager.loadMusic("gamebgm1", "app/res/gamebgm1.wav");
        SoundManager.loadMusic("gamebgm2", "app/res/gamebgm2.wav");
        SoundManager.loadMusic("gamebgm3", "app/res/gamebgm3.wav");
        SoundManager.loadMusic("gamebgm4", "app/res/gamebgm4.wav");
        SoundManager.loadMusic("bossbgm", "app/res/bossbgm.wav");
        SoundManager.loadMusic("bossbgm1", "app/res/bossbgm1.wav");

        FontManager.load("app/res/pixel_font.ttf");
        ImageManager.load("heart", "app/res/heart.png");
        ImageManager.load("thumbnail", "app/res/thumbnail.png");
        ImageManager.load("thumbnail1", "app/res/thumbnail1.png");

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
        
        // Nạp 5 bộ Player
        for (int i = 1; i <= 5; i++) {
            ImageManager.load("player" + i, "app/res/player" + i + ".png");
            String prefix = "player" + i;
            ImageManager.loadAnimation(prefix + "_idle_side", "app/res/" + prefix + "_idle_side.png", 16);
            ImageManager.loadAnimation(prefix + "_run_side", "app/res/" + prefix + "_run_side.png", 10);
            ImageManager.loadAnimation(prefix + "_idle_down", "app/res/" + prefix + "_idle_down.png", 10);
            ImageManager.loadAnimation(prefix + "_run_down", "app/res/" + prefix + "_run_down.png", 12);
            ImageManager.loadAnimation(prefix + "_idle_up", "app/res/" + prefix + "_idle_up.png", 10);
            ImageManager.loadAnimation(prefix + "_run_up", "app/res/" + prefix + "_run_up.png", 12);
        }

        // Nạp 10 bộ Enemy
        for (int i = 1; i <= 10; i++) {
            ImageManager.load("enemy" + i, "app/res/enemy" + i + ".png");
            // Tự động tìm kiếm file _fX để nạp animation cho quái vật
            ImageManager.loadAnimation("enemy" + i, "app/res/enemy" + i + ".png");
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

        // Nạp hoạt ảnh cho các loại quái chuyên biệt
        ImageManager.loadAnimation("enemy_wizard", "app/res/enemy_wizard.png");
        ImageManager.loadAnimation("enemy_assassin", "app/res/enemy_assassin.png");
        ImageManager.loadAnimation("enemy_shooter", "app/res/enemy_shooter.png");
        ImageManager.loadAnimation("enemy_spawner", "app/res/enemy_spawner.png");
        ImageManager.loadAnimation("mimic_f", "app/res/mimic.png");
        ImageManager.load("egg", "app/res/egg.png");
        ImageManager.load("projectile", "app/res/projectile.png");

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
        ImageManager.load("rune", "app/res/rune.png");

        // --- Nạp hoạt ảnh Altar ---
        ImageManager.loadAnimation("altar1", "res/altar1_f4.png");

        // Boss 6 - Swamp Priest
        ImageManager.loadAnimation("boss6_walk1", "app/res/boss6_walk1_f6.png");
        ImageManager.loadAnimation("boss6_walk2", "app/res/boss6_walk2_f6.png");
        ImageManager.loadAnimation("boss6_attack1", "app/res/boss6_attack1_f6.png");
        ImageManager.loadAnimation("boss6_attack2", "app/res/boss6_attack2_f6.png");
        ImageManager.loadAnimation("boss6_attack3", "app/res/boss6_attack3_f6.png");
        ImageManager.loadAnimation("boss6_teleport1", "app/res/boss6_teleport1_f4.png");
        ImageManager.loadAnimation("boss6_teleport2", "app/res/boss6_teleport2_f4.png");
        ImageManager.loadAnimation("boss6_transform1", "app/res/boss6_transform1_f6.png");
        ImageManager.loadAnimation("boss6_transform2", "app/res/boss6_transform2_f2.png");

        // Boss 7 - Swamp Cannon
        ImageManager.loadAnimation("boss7_move", "app/res/boss7_move_f4.png");
        ImageManager.loadAnimation("boss7_dash", "app/res/boss7_dash_f2.png");
        ImageManager.loadAnimation("boss7_death", "app/res/boss7_death_f4.png");
        ImageManager.loadAnimation("boss7_attack1", "app/res/boss7_attack1_f6.png");
        ImageManager.loadAnimation("boss7_attack2", "app/res/boss7_attack2_f4.png");
        ImageManager.loadAnimation("boss7_attack3", "app/res/boss7_attack3_f4.png");

        // --- Tự động nạp các biến thể vật thể môi trường (grass1, tree1, rock1, v.v.) ---
        String[] envTypes = { "grass", "tree", "rock", "woodencrate" };
        for (String type : envTypes) {
            int idx = 1;
            while (true) {
                String path = "app/res/" + type + idx + ".png";
                if (new java.io.File(path).exists()) {
                    ImageManager.load(type + idx, path);
                    idx++;
                } else
                    break;
            }
        }
        
        ImageManager.load("shotgun", "app/res/shotgun.png");
        ImageManager.load("sniper_rifle", "app/res/sniper_rifle.png");
        ImageManager.load("assault_rifle", "app/res/assault_rifle.png");

        // --- Nạp ảnh Parallax cho Menu ---
        ImageManager.load("menu_parallax", "app/res/menu_background.png");
        BufferedImage parallaxImg = ImageManager.get("menu_parallax");
        if (parallaxImg != null) {
            menuParallax = new ParallaxBackground(parallaxImg, screenWidth, screenHeight);
        }

        // --- Tự động nạp tài nguyên đặc trưng cho từng Map ---
        for (gameproject.environment.MapType type : gameproject.environment.MapType.values()) {
            String prefix = type.name().toLowerCase();
            
            // Tìm Background (Ưu tiên _ground sau đó _background)
            String bgPath = "app/res/" + prefix + "_ground.png";
            if (!new java.io.File(bgPath).exists()) bgPath = "app/res/" + prefix + "_background.png";
            
            if (new java.io.File(bgPath).exists()) {
                ImageManager.load(prefix + "_background", bgPath);
            }
            
            // Tìm Water (Ưu tiên _water sau đó _tile_water)
            String waterPath = "app/res/" + prefix + "_water.png";
            if (!new java.io.File(waterPath).exists()) waterPath = "app/res/" + prefix + "_tile_water.png";
            
            if (new java.io.File(waterPath).exists()) {
                ImageManager.load(prefix + "_tile_water", waterPath);
            }
            
            // Tìm Water Borders & Corners (Ưu tiên tên file người dùng: _waterborder)
            String borderPath = "app/res/" + prefix + "_waterborder.png";
            if (!new java.io.File(borderPath).exists()) borderPath = "app/res/" + prefix + "_water_border.png";
            if (new java.io.File(borderPath).exists()) {
                ImageManager.load(prefix + "_water_border", borderPath);
            }
            
            String cornerPath = "app/res/" + prefix + "_watercorner.png";
            if (!new java.io.File(cornerPath).exists()) cornerPath = "app/res/" + prefix + "_water_corner.png";
            if (new java.io.File(cornerPath).exists()) {
                ImageManager.load(prefix + "_water_corner", cornerPath);
            }
        }

        // Load Skill Icons
        for (gameproject.skill.Upgrade u : gameproject.skill.Upgrade.values()) {
            String fileName = u.name().toLowerCase();
            if (u == gameproject.skill.Upgrade.HEALTH)
                fileName = "hp";
            else if (u == gameproject.skill.Upgrade.OPTICAL_SCOPE)
                fileName = "range";

            ImageManager.load("skill_" + u.name().toLowerCase(), "app/res/skill_" + fileName + ".png");
        }

        PlayerData.load();

        buildings = new ArrayList<>();
        mapManager = new MapManager(WORLD_WIDTH, WORLD_HEIGHT, buildings, gameproject.environment.MapConfig.getConfig(gameproject.environment.MapType.OUTSKIRTS));

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
            
            String bgm1 = "gamebgm1";
            String bgm2 = "gamebgm2";
            String bossBgm = "bossbgm";

            // Tùy chỉnh nhạc theo từng loại Map
            if (currentMapConfig != null && currentMapConfig.type == gameproject.environment.MapType.SWAMP) {
                bgm1 = "gamebgm3";
                bgm2 = "gamebgm4"; // Đã có nhạc cho giai đoạn 2
                bossBgm = "bossbgm1";
            }

            // Thực hiện phát nhạc dựa trên logic Wave và Boss
            if (entityManager != null && entityManager.activeBossCount > 0) {
                SoundManager.playMusic(bossBgm);
            } else if (entityManager != null && entityManager.waveCount >= 12) {
                SoundManager.playMusic(bgm2);
            } else {
                SoundManager.playMusic(bgm1);
            }
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public void startNewGame(gameproject.environment.MapConfig mapConfig) {
        this.currentMapConfig = mapConfig;
        resetTime();
        gameproject.state.PlayingState.resetEvents();
        CharacterClass charClass = PlayerData.selectedClass;

        // Tạo mới bản đồ dựa trên cấu hình map được chọn
        synchronized (buildings) {
            buildings.clear();
            mapManager = new MapManager(WORLD_WIDTH, WORLD_HEIGHT, buildings, mapConfig);
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

        currentBgKey = mapConfig.backgroundKey;

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
                
                // Luôn cập nhật Parallax nếu nó tồn tại (chủ yếu dùng cho Menu)
                if (menuParallax != null) {
                    menuParallax.update();
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