package main;

import entity.Monster;
import entity.Player;
import entity.ShootingMonster;
import entity.StunMonster;
import entity.WrapMonster;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import system.Door;
import system.Level;
import system.Level.DoorHit;
import system.Lighting;
import system.Lighting.LightSource;
import system.Puzzle;

/**
 * GamePanel ดูแลการอัปเดตและวาดผลทุกเฟรมของเกม
 */
public class GamePanel extends JPanel implements ActionListener {

    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;

    private static final int PLAYER_SIZE = 10;
    private static final int MONSTER_SIZE = 32;
    private static final int DOOR_SIZE = 48;
    private static final int PLAYER_SPEED = 4;
    private static final int MONSTER_SPEED = 2;

    private static final int PLAYER_LIGHT_RADIUS = 160;
    private static final int MONSTER_LIGHT_RADIUS = 120;

    static final int TOTAL_LEVELS = 6;
    private static final int DOORS_PER_LEVEL = 6;

    private final Timer timer;
    private final Random random = new Random();

    private final Player player = new Player(PLAYER_SIZE, PLAYER_SPEED, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private final List<Monster> monsters = new ArrayList<>();
    private final List<Level> levels = new ArrayList<>();

    private final Puzzle puzzle = new Puzzle();
    private final Color monsterColor = new Color(0xEE82EE);

    private BufferedImage backgroundImage;

    private int panelWidth = DEFAULT_WIDTH;
    private int panelHeight = DEFAULT_HEIGHT;
    private int currentLevelIndex = 0;
    private Integer pendingLevelReset = null;
    private boolean doorInteractionActive = false;
    private DoorHit activeDoorHit = null;

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        loadBackground();
        initMonsters();
        initLevels();
        resetForLevel(0);
        setupKeyboard();

        timer = new Timer(16, this);
        timer.start();
    }

    private void loadBackground() {
        try {
            backgroundImage = ImageIO.read(new File("Pic/Background.png"));
        } catch (IOException e) {
            System.err.println("ไม่สามารถโหลดภาพพื้นหลังได้: " + e.getMessage());
            backgroundImage = null;
        }
    }

    private void setupKeyboard() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (doorInteractionActive && activeDoorHit != null && activeDoorHit.getType() == Door.Type.PUZZLE) {
                    if (isMovementKey(keyCode)) {
                        closePuzzleOverlay();
                    } else {
                        return;
                    }
                }
                player.handleKeyPressed(keyCode);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.handleKeyReleased(e.getKeyCode());
            }
        });
    }

    public void setGameSize(int width, int height) {
        int minWidth = DOOR_SIZE + 100;
        int minHeight = DOOR_SIZE + 150;
        int newWidth = Math.max(minWidth, width);
        int newHeight = Math.max(minHeight, height);

        if (newWidth == panelWidth && newHeight == panelHeight) {
            return;
        }

        panelWidth = newWidth;
        panelHeight = newHeight;
        resetForLevel(currentLevelIndex);
        revalidate();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    private void initMonsters() {
        StunMonster stunMonster = new StunMonster(MONSTER_SIZE, MONSTER_SPEED);
        stunMonster.setActiveLevels(TOTAL_LEVELS, new int[] { 0, 3 });
        monsters.add(stunMonster);

        WrapMonster wrapMonster = new WrapMonster(MONSTER_SIZE, MONSTER_SPEED);
        wrapMonster.setActiveLevels(TOTAL_LEVELS, new int[] { 1, 4 });
        monsters.add(wrapMonster);

        ShootingMonster shootingMonster = new ShootingMonster(MONSTER_SIZE, MONSTER_SPEED + 1);
        shootingMonster.setActiveLevels(TOTAL_LEVELS, new int[] { 2, 5 });
        monsters.add(shootingMonster);
    }

    private void initLevels() {
        for (int i = 0; i < TOTAL_LEVELS; i++) {
            levels.add(new Level(DOORS_PER_LEVEL, DOOR_SIZE));
        }
    }

    private void resetForLevel(int levelIndex) {
        currentLevelIndex = levelIndex;
        pendingLevelReset = null;
        doorInteractionActive = false;
        activeDoorHit = null;
        puzzle.clearActivePuzzle();

        player.updateBounds(panelWidth, panelHeight);
        player.spawn();

        Level level = getCurrentLevel();
        level.reset(random, panelWidth, panelHeight);

        for (Monster monster : monsters) {
            monster.prepareForLevel(levelIndex, random, panelWidth, panelHeight);
        }
    }

    private Level getCurrentLevel() {
        return levels.get(currentLevelIndex);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<LightSource> lights = collectLightSources();

        drawBackground(g2d);
        drawDoors(g2d, lights);
        drawPlayer(g2d);
        drawMonsters(g2d);
        drawLighting(g2d, lights);
        drawStunEffects(g2d);
        drawLevelInfo(g2d);
        drawPuzzleOverlay(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, panelWidth, panelHeight, null);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, panelWidth, panelHeight);
        }
    }

    private void drawDoors(Graphics2D g2d, List<LightSource> lights) {
        Level level = getCurrentLevel();
        for (Door door : level.getDoors()) {
            int doorCenterX = door.getX(panelWidth) + DOOR_SIZE / 2;
            int doorCenterY = door.getY(panelHeight) + DOOR_SIZE / 2;
            if (!Lighting.isPointLit(doorCenterX, doorCenterY, lights)) {
                continue;
            }
            door.draw(g2d, panelWidth, panelHeight);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        player.draw(g2d);
    }

    private void drawMonsters(Graphics2D g2d) {
        for (Monster monster : monsters) {
            if (monster.isActive()) {
                monster.drawAsBox(g2d, monsterColor);
            }
        }
    }

    private void drawLighting(Graphics2D g2d, List<LightSource> lights) {
        BufferedImage darkness = Lighting.createLightingMask(panelWidth, panelHeight, lights);
        g2d.drawImage(darkness, 0, 0, null);
    }

    private void drawStunEffects(Graphics2D g2d) {
        for (Monster monster : monsters) {
            if (monster.isActive() && monster instanceof StunMonster) {
                ((StunMonster) monster).drawStun(g2d);
            }
        }
    }

    private void drawLevelInfo(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("Level " + (currentLevelIndex + 1), 20, 30);
    }

    private void drawPuzzleOverlay(Graphics2D g2d) {
        if (!doorInteractionActive || activeDoorHit == null || activeDoorHit.getType() != Door.Type.PUZZLE) {
            return;
        }
        puzzle.drawOverlay(g2d, panelWidth, panelHeight);
    }

    private List<LightSource> collectLightSources() {
        List<LightSource> lights = new ArrayList<>();
        lights.add(new LightSource(player.getCenterX(), player.getCenterY(), PLAYER_LIGHT_RADIUS));
        for (Monster monster : monsters) {
            if (monster.isActive()) {
                lights.add(new LightSource(monster.getCenterX(), monster.getCenterY(), MONSTER_LIGHT_RADIUS));
            }
        }
        return lights;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Level level = getCurrentLevel();
        level.updateDoorAnimations();

        if (!doorInteractionActive || player.isDead()) {
            player.update();
        }

        if (pendingLevelReset != null && player.isDeathAnimationFinished()) {
            resetForLevel(pendingLevelReset);
            pendingLevelReset = null;
            repaint();
            return;
        }

        if (player.isDead()) {
            repaint();
            return;
        }

        if (doorInteractionActive) {
            repaint();
            return;
        }

        for (Monster monster : monsters) {
            monster.update(player.getX(), player.getY());
        }

        updateStunEffects();
        checkDoorCollisions();
        checkMonsterCollisions();

        repaint();
    }

    private void updateStunEffects() {
        if (player.isDead()) {
            return;
        }
        for (Monster monster : monsters) {
            if (!monster.isActive()) {
                continue;
            }
            if (monster instanceof StunMonster) {
                StunMonster stunMonster = (StunMonster) monster;
                if (stunMonster.isPlayerInStun(player.getX(), player.getY(), PLAYER_SIZE) && !player.isStunned()) {
                    player.applyStun(stunMonster.getStunDurationTicks());
                }
            }
        }
    }

    private void checkDoorCollisions() {
        if (player.isDead() || doorInteractionActive) {
            return;
        }
        Level level = getCurrentLevel();
        DoorHit hit = level.detectDoorCollision(player);
        if (hit != null) {
            handleDoorCollision(level, hit);
        }
    }

    private void handleDoorCollision(Level level, DoorHit hit) {
        doorInteractionActive = true;
        activeDoorHit = hit;
        player.stopImmediately();

        switch (hit.getType()) {
            case PUZZLE:
                puzzle.showPuzzle(hit.getPuzzleNumber());
                break;
            case ADVANCE:
            case BACK:
                puzzle.clearActivePuzzle();
                promptDoorPassword(level, hit);
                break;
        }
        repaint();
    }

    private void promptDoorPassword(Level level, DoorHit hit) {
        String message = "Please enter password";
        String input = JOptionPane.showInputDialog(this, message, "รหัสประตู", JOptionPane.QUESTION_MESSAGE);

        if (level.validatePassword(input)) {
            if (hit.getType() == Door.Type.ADVANCE) {
                int nextLevel = (currentLevelIndex + 1) % TOTAL_LEVELS;
                resetForLevel(nextLevel);
            } else {
                int prevLevel = (currentLevelIndex - 1 + TOTAL_LEVELS) % TOTAL_LEVELS;
                resetForLevel(prevLevel);
            }
            requestFocusInWindow();
            return;
        }

        if (input != null) {
            JOptionPane.showMessageDialog(this,
                    "รหัสไม่ถูกต้อง ลองสำรวจรูปภาพให้ครบทั้ง 4 บานก่อนนะครับ",
                    "รหัสผิดพลาด",
                    JOptionPane.WARNING_MESSAGE);
        }

        level.pushSpriteAwayFromDoor(player, hit.getDoor());
        clearDoorInteractionState();
        repaint();
    }

    private void closePuzzleOverlay() {
        Level level = getCurrentLevel();
        level.pushSpriteAwayFromDoor(player, activeDoorHit != null ? activeDoorHit.getDoor() : null);
        clearDoorInteractionState();
        repaint();
    }

    private void clearDoorInteractionState() {
        doorInteractionActive = false;
        activeDoorHit = null;
        puzzle.clearActivePuzzle();
        requestFocusInWindow();
    }

    private void checkMonsterCollisions() {
        if (player.isDead()) {
            return;
        }
        for (Monster monster : monsters) {
            if (monster.isActive() && player.intersects(monster)) {
                player.die();
                pendingLevelReset = 0;
                break;
            }
        }
    }

    private boolean isMovementKey(int keyCode) {
        return keyCode == KeyEvent.VK_LEFT
                || keyCode == KeyEvent.VK_RIGHT
                || keyCode == KeyEvent.VK_UP
                || keyCode == KeyEvent.VK_DOWN
                || keyCode == KeyEvent.VK_A
                || keyCode == KeyEvent.VK_D
                || keyCode == KeyEvent.VK_W
                || keyCode == KeyEvent.VK_S;
    }
}