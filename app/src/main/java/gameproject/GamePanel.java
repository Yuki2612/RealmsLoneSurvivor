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
import gameproject.meta.PlayerData;
import gameproject.meta.CharacterClass;

public class GamePanel extends JPanel implements Runnable {
    private Thread gameThread;
    private final int FPS = 60;
    
    public int screenWidth = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    public int screenHeight = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    public static final int WORLD_WIDTH = 1800;
    public static final int WORLD_HEIGHT = 1800;
    
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

    private State currentState;

    public GamePanel() {
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
        currentWeapon = new Pistol();

        SoundManager.load("shoot", "app/res/shoot.wav");
        SoundManager.load("hit", "app/res/hit.wav");
        SoundManager.load("explosion", "app/res/explosion.wav");
        SoundManager.load("levelup", "app/res/levelup.wav");
        SoundManager.load("laser", "app/res/laser.wav");
        SoundManager.load("shield", "app/res/shield.wav");

        FontManager.load("app/res/pixel_font.ttf");
        ImageManager.load("heart", "app/res/heart.png");

        int bgIndex = 1;
        while (true) {
            String path = "app/res/background" + bgIndex + ".png";
            if (new java.io.File(path).exists()) {
                ImageManager.load("background" + bgIndex, path);
                totalBackgrounds++;
                bgIndex++;
            } else break;
        }

        ImageManager.load("player", "app/res/player.png");
        for (int i = 1; i <= 5; i++) {
            ImageManager.load("player" + i, "app/res/player" + i + ".png");
            ImageManager.load("enemy" + i, "app/res/enemy" + i + ".png");
            
            // Load Animations (Cập nhật thông số chính xác từ thuộc tính ảnh)
            String prefix = "player" + i;
            ImageManager.loadAnimation(prefix + "_idle_side", "app/res/" + prefix + "_idle_side.png", 16);
            ImageManager.loadAnimation(prefix + "_run_side", "app/res/" + prefix + "_run_side.png", 10);
            ImageManager.loadAnimation(prefix + "_idle_down", "app/res/" + prefix + "_idle_down.png", 10);
            ImageManager.loadAnimation(prefix + "_run_down", "app/res/" + prefix + "_run_down.png", 12);
            ImageManager.loadAnimation(prefix + "_idle_up", "app/res/" + prefix + "_idle_up.png", 10);
            ImageManager.loadAnimation(prefix + "_run_up", "app/res/" + prefix + "_run_up.png", 12);
        }
        ImageManager.load("boss1", "app/res/boss1.png");
        ImageManager.load("boss2", "app/res/boss2.png");
        ImageManager.load("boss3", "app/res/boss3.png");
        ImageManager.load("chest1", "app/res/chest1.png");
        ImageManager.load("chest2", "app/res/chest2.png");

        PlayerData.load();

        changeState(new MenuState());

        gameThread = new Thread(this);
        gameThread.start();
    }

    public void changeState(State state) {
        this.currentState = state;
    }
    
    public State getCurrentState() {
        return currentState;
    }

    public void startNewGame() {
        CharacterClass charClass = PlayerData.selectedClass;
        player = new Player(screenWidth / 2, screenHeight / 2, charClass);
        score = 0;
        activeSkills.clear();
        currentWeapon = new Pistol();
        vfxManager.fireZones.clear();
        
        long currentTime = System.currentTimeMillis();
        startTime = currentTime;
        // Adjust survive time based on wave (approx 15s per wave) for scaling
        surviveTimeSeconds = (PlayerData.debugStartWave - 1) * 15;
        
        upgradeManager.startNewGame(PlayerData.debugStartLevel);
        upgradeManager.playerDamage = (int)((10 + gameproject.meta.PlayerData.statDamageLevel) * charClass.damageMulti);
        entityManager.startNewGame(currentTime, PlayerData.debugStartWave);
        
        if (totalBackgrounds > 0) {
            currentBgKey = "background" + (new java.util.Random().nextInt(totalBackgrounds) + 1);
        }
        
        if (charClass.startingUpgrade != null) {
            upgradeManager.applyUpgrade(charClass.startingUpgrade, player, activeSkills, currentWeapon);
        }

        changeState(new gameproject.state.PlayingState());
    }
    
    public void triggerGameOver() {
        PlayerData.save();
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

    public void addScoreAndExp(int amount) {
        score += amount;
        upgradeManager.addExp(amount);
    }

    @Override
    public void run() {
        double timePerFrame = 1000000000.0 / FPS;
        long lastFrame = System.nanoTime();
        while (true) {
            if (System.nanoTime() - lastFrame >= timePerFrame) {
                if (currentState != null) {
                    currentState.update(this);
                }
                // Dọn dẹp trạng thái click và phím nhất thời
                input.clearClickAndKey();
                
                repaint();
                lastFrame = System.nanoTime();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentState != null) {
            currentState.render(this, g);
        }
    }
}