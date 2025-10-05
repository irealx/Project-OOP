package main;

import entity.Monster;
import entity.Player;
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
import system.Config;
import system.Door;
import system.Level;
import system.Level.DoorHit;
import system.Lighting;
import system.Puzzle;

// REFACTOR: ทำให้ลำดับการวาดภาพเป็นระบบเดียวกัน แก้ไขง่ายและลดความซับซ้อน
public class GamePanel extends JPanel implements ActionListener {

    private final Random random = new Random();

    private final Player player = new Player();
    private final List<Monster> monsters = Monster.createDefaultMonsters();
    private final List<Level> levels = new ArrayList<>();
    private final Puzzle puzzle = new Puzzle();
    private BufferedImage background;
    private int width = Config.PANEL_WIDTH;
    private int height = Config.PANEL_HEIGHT;
    private int levelIndex;
    private Integer pendingReset;
    private DoorHit activeDoor;
    public GamePanel() {
        setFocusable(true);
        setBackground(Config.BACKGROUND_COLOR);
        background = load("Pic/Background.png");
        for (int i = 0; i < Config.TOTAL_LEVELS; i++) {
            levels.add(new Level(Config.DOOR_PER_LEVEL, Config.DOOR_SIZE));
        }
        setupKeyboard();
        resetLevel(0);
        new Timer(Config.TIMER_DELAY_MS, this).start();
    }
    private BufferedImage load(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException ex) {
            return null;
        }
    }
    private void setupKeyboard() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (activeDoor != null && activeDoor.type() == Door.Type.PUZZLE && isMovementKey(e.getKeyCode())) {
                    closeInteraction(levels.get(levelIndex));
                } else {
                    player.handleKeyPressed(e.getKeyCode());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.handleKeyReleased(e.getKeyCode());
            }
        });
    }
    public void setGameSize(int width, int height) {
        this.width = Math.max(Config.DOOR_SIZE + 100, width);
        this.height = Math.max(Config.DOOR_SIZE + 150, height);
        resetLevel(levelIndex);
        revalidate();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }
    private void resetLevel(int index) {
        levelIndex = index;
        pendingReset = null;
        activeDoor = null;
        puzzle.clear();
        Level level = levels.get(levelIndex);
        level.reset(width, height);
        player.updateBounds(width, height);
        player.spawn();

        for (Monster monster : monsters) {
            monster.prepareForLevel(levelIndex, random, width, height);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        List<Lighting.LightSource> lights = Lighting.collect(player, monsters);
        renderBaseSprites(g2, lights);
        renderLighting(g2, lights);
        renderOverlays(g2);
        renderUI(g2);
    }
    private void renderBaseSprites(Graphics2D g2, List<Lighting.LightSource> lights) {
        if (background != null) {
            g2.drawImage(background, 0, 0, width, height, null);
        } else {
            g2.setColor(Config.BACKGROUND_COLOR);
            g2.fillRect(0, 0, width, height);
        }
        Level level = levels.get(levelIndex);
        for (Door door : level.getDoors()) {
            int cx = door.getX(width) + Config.DOOR_SIZE / 2;
            int cy = door.getY(height) + Config.DOOR_SIZE / 2;
            if (Lighting.isPointLit(cx, cy, lights)) {
                door.draw(g2, width, height);
            }
        }
        player.draw(g2);
        for (Monster monster : monsters) {
            monster.draw(g2);
        }
    }
    private void renderLighting(Graphics2D g2, List<Lighting.LightSource> lights) {
        g2.drawImage(Lighting.createMask(width, height, lights), 0, 0, null);
    }
    private void renderOverlays(Graphics2D g2) {
        if (activeDoor != null && activeDoor.type() == Door.Type.PUZZLE) {
            puzzle.draw(g2, width, height);
        }
    }
    private void renderUI(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.drawString("Level " + (levelIndex + 1), 20, 30);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Level level = levels.get(levelIndex);
        level.getDoors().forEach(Door::updateAnimation);
        if (activeDoor == null || player.isDead()) {
            player.update();
        }
        if (pendingReset != null && player.isDeathAnimationFinished()) {
            resetLevel(pendingReset);
            repaint();
            return;
        }
        if (player.isDead()) {
            repaint();
            return;
        }
        if (activeDoor != null) {
            repaint();
            return;
        }
        Monster.updateAll(monsters, player, level);
        handleCollisions(level);
        repaint();
    }
    private void handleCollisions(Level level) {
        DoorHit hit = level.detectDoorCollision(player);
        if (hit != null) {
             activeDoor = hit;
            player.stopImmediately();
            if (hit.type() == Door.Type.PUZZLE) {
                puzzle.show(hit.puzzleNumber());
                return;
            }
            puzzle.clear();
            promptPassword(level, hit);
            return;
        }
        for (Monster monster : monsters) {
            if (monster.isActive() && player.intersects(monster)) {
                player.die();
                pendingReset = 0;
                break;
        }
    }
    private void promptPassword(Level level, DoorHit hit) {
        String input = JOptionPane.showInputDialog(this, "Please enter password", "รหัสประตู", JOptionPane.QUESTION_MESSAGE);
        if (level.validatePassword(input)) {
            int next = hit.type() == Door.Type.ADVANCE
                    ? (levelIndex + 1) % Config.TOTAL_LEVELS
                    : (levelIndex - 1 + Config.TOTAL_LEVELS) % Config.TOTAL_LEVELS;
            resetLevel(next);
            requestFocusInWindow();
            return;
        }
        if (input != null) {
            JOptionPane.showMessageDialog(this, "รหัสไม่ถูกต้อง ลองสำรวจรูปภาพให้ครบทั้ง 4 บานก่อนนะครับ", "รหัสผิดพลาด", JOptionPane.WARNING_MESSAGE);
        }
        closeInteraction(level);
    }
    private void closeInteraction(Level level) {
        if (activeDoor != null) {
            level.pushAway(player, activeDoor.door());
        }
        activeDoor = null;
        puzzle.clear();
        requestFocusInWindow();
    }
    private boolean isMovementKey(int code) {
        return switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
                 KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S -> true;
            default -> false;
        };
    }
}