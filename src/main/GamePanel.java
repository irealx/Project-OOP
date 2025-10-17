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
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import system.Config;
import system.Door;
import system.Level;
import system.Level.DoorHit;
import system.Lighting;
import system.Puzzle;

// จัดการลูปเกมหลัก การวาดภาพ และตรรกะการชนทั้งหมด
public class GamePanel extends JPanel implements ActionListener {

    private final Random random = new Random();

    private final Player player = new Player();
    private final List<Monster> monsters = Monster.createDefaultMonsters();
    private final List<Level> levels = new ArrayList<>();
    private final Puzzle puzzle = new Puzzle();
    private final GameMenu menu = new GameMenu();

    private BufferedImage background;
    private int width = Config.PANEL_WIDTH;
    private int height = Config.PANEL_HEIGHT;

    private int levelIndex;
    private Integer pendingReset;
    private Level.DoorHit activeDoor;
    private boolean showMenu = true;

    public GamePanel() {
        setFocusable(true);
        setBackground(Config.BACKGROUND_COLOR);
        background = load("Pic/Background.png");

        // สร้างด่านทั้งหมดตามจำนวนที่กำหนดใน Config
        for (int i = 0; i < Config.TOTAL_LEVELS; i++) {
            levels.add(new Level(Config.DOOR_PER_LEVEL, Config.DOOR_SIZE));
        }

        setupKeyboard();
        resetLevel(0); // เริ่มจากเลเวลแรก
        new Timer(Config.TIMER_DELAY_MS, this).start(); // ลูปเกมหลัก
    }

    private BufferedImage load(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException ex) {
            return null;
        }
    }

    // ตั้งค่าการควบคุมด้วยคีย์บอร์ด
    private void setupKeyboard() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (showMenu) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        showMenu = false;
                        requestFocusInWindow();
                    }
                    return;
                }
                // ถ้าอยู่ใน puzzle และกดปุ่มเคลื่อนที่ จะออกจากหน้าพัซเซิล
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

    // ปรับขนาดพื้นที่เกมเมื่อหน้าต่างเปลี่ยนขนาด
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
        requestFocusInWindow(); // โฟกัสเพื่อรับ input
    }

    // รีเซ็ตสถานะของเลเวลปัจจุบัน
    private void resetLevel(int index) {
        levelIndex = index;
        pendingReset = null;
        activeDoor = null;
        puzzle.clear();

        Level level = levels.get(levelIndex);
        level.reset(width, height);
        player.updateBounds(width, height);
        player.spawn();

        // เตรียมมอนสเตอร์สำหรับเลเวลนี้
        for (Monster monster : monsters) {
            monster.prepareForLevel(levelIndex, random, width, height);
        }
    }

    // ลำดับการวาด: พื้นหลัง → วัตถุ → แสง → UI
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

    // วาดพื้นหลัง ผู้เล่น มอนสเตอร์ และประตู (เฉพาะที่มีแสง)
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
        for (Monster monster : monsters) monster.draw(g2);
    }

    // วาดเอฟเฟกต์แสงซ้อนทับ
    private void renderLighting(Graphics2D g2, List<Lighting.LightSource> lights) {
        g2.drawImage(Lighting.createMask(width, height, lights), 0, 0, null);
    }

    // แสดง overlay ของ Puzzle เมื่อชนประตูพัซเซิล
    private void renderOverlays(Graphics2D g2) {
        if (showMenu) {
            menu.draw(g2, width, height);
            return;
        }
        if (activeDoor != null && activeDoor.type() == Door.Type.PUZZLE) {
            puzzle.draw(g2, width, height);
        }
    }

    // วาด UI ด้านบน เช่น ชื่อเลเวล
    private void renderUI(Graphics2D g2) {
        if (showMenu) return;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.drawString("Level " + (levelIndex + 1), 20, 30);
    }

    // ลูปหลักของเกม เรียกทุกครั้งเมื่อ Timer ทำงาน
    @Override
    public void actionPerformed(ActionEvent e) {
        if (showMenu) {
            repaint();
            return;
        }

        Level level = levels.get(levelIndex);
        level.getDoors().forEach(Door::updateAnimation);

        // อัปเดตผู้เล่น
        if (activeDoor == null || player.isDead()) player.update();

        // จัดการสถานะหลังตาย / รีเซ็ตเลเวล
        if (pendingReset != null && player.isDeathAnimationFinished()) {
            resetLevel(pendingReset);
            repaint();
            return;
        }

        if (player.isDead() || activeDoor != null) {
            repaint();
            return;
        }

        // อัปเดตมอนสเตอร์และตรวจการชน
        Monster.updateAll(monsters, player, level);
        handleCollisions(level);
        repaint();
    }

    // ตรวจการชนของผู้เล่นกับประตูและมอนสเตอร์
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

        // ชนมอนสเตอร์ -> ตายและรีเซ็ต
        for (Monster monster : monsters) {
            if (monster.isActive() && player.intersects(monster)) {
                player.die();
                pendingReset = 0;
                break;
            }
        }
    }

    // กล่องใส่รหัสผ่านเมื่อชนประตูไปต่อ/ย้อนกลับ
    private void promptPassword(Level level, DoorHit hit) {
        String input = showNumericInputDialog("Please enter password", "รหัสประตู");

        if (level.validatePassword(input)) {
            int next = hit.type() == Door.Type.ADVANCE
                    ? (levelIndex + 1) % Config.TOTAL_LEVELS
                    : (levelIndex - 1 + Config.TOTAL_LEVELS) % Config.TOTAL_LEVELS;

            resetLevel(next);
            requestFocusInWindow();
            return;
        }

        if (input != null) {
            JOptionPane.showMessageDialog(
                    this,
                    "รหัสไม่ถูกต้อง ลองสำรวจรูปภาพให้ครบทั้ง 4 บานก่อนนะครับ",
                    "รหัสผิดพลาด",
                    JOptionPane.WARNING_MESSAGE
            );
        }
        closeInteraction(level);
    }

    // แสดงกล่องกรอกข้อมูลที่รับเฉพาะตัวเลขเท่านั้น ถ้า Cancel จะคืนค่าเป็น null
    private String showNumericInputDialog(String message, String title) {
        JTextField field = new JTextField();
        field.setColumns(6);
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DigitsOnlyFilter());

        while (true) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    new Object[]{message, field},
                    title,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                return null;
            }

            String text = field.getText();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }

            JOptionPane.showMessageDialog(
                    this,
                    "กรุณากรอกตัวเลขก่อนกดตกลง",
                    "ข้อมูลไม่ครบถ้วน",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    // DocumentFilter สำหรับยอมให้พิมพ์เฉพาะตัวเลข 0-9
    private static class DigitsOnlyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null || string.isEmpty() || isNumeric(string)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || isNumeric(text)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private boolean isNumeric(String text) {
            for (int i = 0; i < text.length(); i++) {
                if (!Character.isDigit(text.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    // ปิดหน้าพัซเซิล / กลับเข้าสู่การควบคุมปกติ
    private void closeInteraction(Level level) {
        if (activeDoor != null) {
            level.pushAway(player, activeDoor.door());
        }
        activeDoor = null;
        puzzle.clear();
        requestFocusInWindow();
    }

    // ตรวจว่าปุ่มที่กดเป็นปุ่มเคลื่อนที่หรือไม่
    private boolean isMovementKey(int code) {
        return switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
                 KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S -> true;
            default -> false;
        };
    }
}