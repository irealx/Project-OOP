// ---------- ส่วน import ----------
import java.awt.AlphaComposite;    // ใช้ควบคุมความโปร่งใสของเลเยอร์มืด
import java.awt.Color;             // ใช้จัดการสีในกราฟิก
import java.awt.Font;              // ใช้กำหนดฟอนต์ตัวอักษร
import java.awt.Graphics;          // สำหรับวาดภาพพื้นฐาน
import java.awt.Graphics2D;        // สำหรับกราฟิกขั้นสูง เช่น Anti-Aliasing
import java.awt.Paint;             // เก็บสถานะสีหรือไล่สีระหว่างสร้างเอฟเฟกต์ไฟ
import java.awt.RadialGradientPaint; // ใช้สร้างไล่ระดับแสงแบบวงกลม
import java.awt.RenderingHints;    // สำหรับปรับคุณภาพการเรนเดอร์
import java.awt.Composite;         // ใช้เก็บคอมโพสิตก่อนเปลี่ยนค่า
import java.awt.geom.Point2D;      // เก็บพิกัดจุดศูนย์กลางของวงไฟ
import java.awt.image.BufferedImage; // สำหรับเก็บภาพพื้นหลัง
import java.io.File;               // ระบุไฟล์ภาพพื้นหลัง
import java.io.IOException;        // จับข้อผิดพลาดเมื่อโหลดภาพ
import java.awt.event.ActionEvent; // สำหรับตรวจเหตุการณ์ของ Timer
import java.awt.event.ActionListener; // สำหรับรับฟัง event จาก Timer
import java.awt.event.KeyAdapter;  // สำหรับจับปุ่มกด
import java.awt.event.KeyEvent;    // สำหรับรหัสปุ่มคีย์บอร์ด
import java.util.ArrayList;        // ใช้เก็บข้อมูลในลิสต์แบบ dynamic
import java.util.Collections;      // ใช้สำหรับสุ่มและจัดการ list
import java.util.List;             // interface ของ ArrayList
import java.util.Random;           // ใช้สุ่มตำแหน่งและค่าอื่น ๆ
import javax.swing.JPanel;         // พื้นที่หลักของเกม (Canvas)
import javax.swing.Timer;          // ตัวจับเวลาในการอัปเดตเกมทุกเฟรม
import javax.imageio.ImageIO;      // โหลดไฟล์ภาพจากดิสก์
import javax.swing.JOptionPane;    // กล่องโต้ตอบสำหรับใส่รหัสผ่านประตู


// ---------- คลาสหลักของเกม ----------
public class GamePanel extends JPanel implements ActionListener {

    // ขนาดหน้าจอเกมแบบเริ่มต้น (ใช้ในโหมดหน้าต่าง)
    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;

    // ขนาดจริงของพื้นที่เกม (เปลี่ยนตามโหมดหน้าต่าง/เต็มจอ)
    private int panelWidth = DEFAULT_WIDTH;
    private int panelHeight = DEFAULT_HEIGHT;

    // ขนาดและความเร็วของวัตถุต่าง ๆ
    private static final int PLAYER_SIZE = 10;
    private static final int MONSTER_SIZE = 32;
    private static final int DOOR_SIZE = 48;
    private static final int PLAYER_SPEED = 4;
    private static final int MONSTER_SPEED = 2;

    // รัศมีไฟที่ใช้เปิดเผยพื้นที่
    private static final int PLAYER_LIGHT_RADIUS = 160;   // ผู้เล่นมองเห็นได้กว้างกว่า
    private static final int MONSTER_LIGHT_RADIUS = 120;  // แสงรอบตัวมอนสเตอร์

    // จำนวนด่านทั้งหมด
    static final int TOTAL_LEVELS = 6;
    // จำนวนประตูต่อด่าน
    private static final int DOORS_PER_LEVEL = 6;

    // ---------- ตัวแปรของเกม ----------
    private final Timer timer; // ตัวจับเวลาอัปเดตเกมทุก 16ms (≈60 FPS)
    private final Random random = new Random(); // ใช้สุ่มค่าต่าง ๆ เช่น ประตู

    // วัตถุหลักในเกม
    private final Player player = new Player(PLAYER_SIZE, PLAYER_SPEED, panelWidth, panelHeight); // ผู้เล่น
    private final List<MonsterController> monsters = new ArrayList<>(); // รายชื่อมอนสเตอร์
    private final List<Level> levels = new ArrayList<>();      // รายชื่อด่านทั้งหมด

    private final BufferedImage backgroundImage; // ภาพพื้นหลังที่ใช้กับทุกเลเวล
    private final BufferedImage[] puzzleImages = loadPuzzleImages(); // ภาพ Puzzle ทั้ง 9 รูป

    private int currentLevelIndex = 0; // ด่านปัจจุบัน
    private Integer pendingLevelReset = null; // เก็บด่านที่จะรีเซ็ตหลังเล่นแอนิเมชันตาย
    private boolean doorInteractionActive = false; // สถานะหยุดเกมเมื่อชนประตู
    private Door activeDoor = null;                 // ประตูที่ผู้เล่นชนอยู่
    private BufferedImage activePuzzleImage = null; // ภาพ Puzzle ที่กำลังเปิดให้ดู
    private Integer activePuzzleNumber = null;      // เลขของภาพ Puzzle ที่กำลังโชว์

    // ---------- Constructor ----------
    public GamePanel() {
        setBackground(Color.BLACK);  // ตั้งพื้นหลังเป็นสีดำ (โทนเกม)
        setFocusable(true);          // ให้รับคีย์บอร์ดได้

        // พยายามโหลดภาพพื้นหลัง ถ้าไม่สำเร็จจะเก็บค่า null เพื่อใช้สีพื้นแทน
        BufferedImage loadedBackground = null;
        try {
            loadedBackground = ImageIO.read(new File("Pic/Background.png"));
        } catch (IOException e) {
            System.err.println("ไม่สามารถโหลดภาพพื้นหลังได้: " + e.getMessage());
        }
        backgroundImage = loadedBackground;

        // สร้างมอนสเตอร์ทั้งหมดที่ใช้ในเกม
        initMonsters();
        // สร้างทุกด่านที่มี
        initLevels();
        // รีเซ็ตสถานะเริ่มต้นของเกมที่ด่าน 0
        resetForLevel(0);

        // จับเหตุการณ์การกดปุ่มของผู้เล่น
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (doorInteractionActive && activeDoor != null && activeDoor.getType() == Door.Type.PUZZLE) {
                    if (isMovementKey(keyCode)) {
                        closePuzzleOverlay();
                    } else {
                        return; // กดปุ่มอื่นระหว่างดูภาพไม่ให้มีผล
                    }
                }
                player.handleKeyPressed(keyCode); // กดปุ่ม → ส่งให้ Player จัดการ
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.handleKeyReleased(e.getKeyCode()); // ปล่อยปุ่ม → ส่งให้ Player จัดการ
            }
        });

        // สร้าง Timer ให้อัปเดตทุก 16ms (ประมาณ 60 เฟรมต่อวินาที)
        timer = new Timer(16, this);
        timer.start(); // เริ่มเกม
    }

    // ปรับขนาดของพื้นที่เกมให้ตรงกับหน้าจอที่ใช้งานอยู่
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

    // เมื่อ JPanel ถูกเพิ่มเข้า JFrame แล้ว จะเรียกเมธอดนี้อัตโนมัติ
    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow(); // ขอ focus เพื่อให้รับคีย์บอร์ดได้ทันที
    }

    // ---------- ฟังก์ชันสร้างมอนสเตอร์ ----------
    private void initMonsters() {
        // เพิ่มมอนสเตอร์แต่ละชนิดพร้อมระบุว่าอยู่ในด่านใด
        monsters.add(new MonsterController(
            new StunMonster(MONSTER_SIZE, MONSTER_SPEED),
            new int[] { 0, 3 }  // มอนสเตอร์นี้โผล่ในด่าน 0 และ 3
        ));
        monsters.add(new MonsterController(
            new WrapMonster(MONSTER_SIZE, MONSTER_SPEED),
            new int[] { 1, 4 }  // โผล่ในด่าน 1 และ 4
        ));
        monsters.add(new MonsterController(
            new ShootingMonster(MONSTER_SIZE, MONSTER_SPEED + 1),
            new int[] { 2, 5 }  // โผล่ในด่าน 2 และ 5
        ));
    }

    // ---------- ฟังก์ชันสร้างด่าน ----------
    private void initLevels() {
        for (int i = 0; i < TOTAL_LEVELS; i++) {
            levels.add(new Level()); // สร้าง Level ว่างไว้ทั้งหมด 6 ด่าน
        }
    }

    // โหลดภาพ Puzzle ทั้ง 9 รูปเก็บไว้เพื่อนำมาแสดงเมื่อชนประตูพิเศษ
    private BufferedImage[] loadPuzzleImages() {
        BufferedImage[] images = new BufferedImage[9];
        for (int i = 1; i <= 9; i++) {
            try {
                images[i - 1] = ImageIO.read(new File(String.format("Pic/character/puzzle/pz%d.png", i)));
            } catch (IOException e) {
                System.err.println("ไม่สามารถโหลดภาพ puzzle หมายเลข " + i + ": " + e.getMessage());
                images[i - 1] = null;
            }
        }
        return images;
    }

    // ---------- รีเซ็ตสถานะเมื่อเริ่มหรือเปลี่ยนด่าน ----------
    private void resetForLevel(int levelIndex) {
        currentLevelIndex = levelIndex; // ตั้งค่าด่านปัจจุบัน
        pendingLevelReset = null; // เคลียร์สถานะรีเซ็ตที่ค้างอยู่
        doorInteractionActive = false; // เคลียร์สถานะหยุดเกมจากการชนประตู
        activeDoor = null;
        activePuzzleImage = null;
        activePuzzleNumber = null;
        player.updateBounds(panelWidth, panelHeight); // ปรับขอบเขตของผู้เล่นให้ตรงกับขนาดจอใหม่
        Level level = levels.get(levelIndex); // ดึงด่านที่เลือกมา
        level.randomizeDoors(random, panelWidth, panelHeight); // สุ่มตำแหน่งและชนิดของประตูใหม่ทุกครั้ง
        player.spawn(); // วางผู้เล่นที่จุดเริ่มต้นกลางจอ

        // เตรียมมอนสเตอร์ทั้งหมดให้พร้อมสำหรับด่านใหม่
        for (MonsterController controller : monsters) {
            controller.prepareForLevel(levelIndex, random, panelWidth, panelHeight);
            }
        }


    // ---------- ส่วนวาดกราฟิก ----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // ล้างหน้าจอก่อนวาดใหม่ทุกเฟรม

        Graphics2D g2d = (Graphics2D) g; // แปลงเป็น Graphics2D เพื่อใช้เรนเดอร์คุณภาพสูง
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // เปิด anti-aliasing

        // เรียกวาดองค์ประกอบทั้งหมด
        drawBackground(g2d);
        drawDoors(g2d);
        drawPlayer(g2d);
        drawMonsters(g2d);
        drawLighting(g2d);

        // วาดวงสตั้นของ StunMonster ทับเลเยอร์ความมืด เพื่อให้เห็นเอฟเฟกต์ชัดเจน
        for (MonsterController controller : monsters) {
            if (controller.isActive()) {
                Monster monster = controller.getMonster();
                if (monster instanceof StunMonster) {
                    ((StunMonster) monster).drawStun(g2d);
                }
            }
        }

        drawLevelInfo(g2d);
        drawPuzzleOverlay(g2d); // วาดภาพ Puzzle ถ้ามีการเปิดดูอยู่
    }

    // วาดพื้นหลังให้เต็มหน้าจอ ถ้ามีไฟล์ภาพที่โหลดได้
    private void drawBackground(Graphics2D g2d) {
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, panelWidth, panelHeight, null); // ขยายภาพให้เต็มพื้นที่เกม
        } else {
            g2d.setColor(Color.BLACK); // สำรอง: ถ้าโหลดภาพไม่ได้ให้พื้นหลังเป็นสีดำ
            g2d.fillRect(0, 0, panelWidth, panelHeight);
        }
    }

    // วาดข้อความแสดงข้อมูลของเลเวล
    private void drawLevelInfo(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("Level " + (currentLevelIndex + 1), 20, 30); // แสดงชื่อด่านมุมซ้ายบน
    }

    // วาดหน้าต่างภาพ Puzzle ตรงกลางจอเมื่อผู้เล่นกำลังตรวจสอบประตู
    private void drawPuzzleOverlay(Graphics2D g2d) {
        if (!doorInteractionActive || activeDoor == null || activeDoor.getType() != Door.Type.PUZZLE) {
            return;
        }
        // สร้างพื้นหลังทึบแสงเล็กน้อยเพื่อให้ภาพ Puzzle เด่นขึ้น
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        int drawY = panelHeight / 2 - 20;
        int drawX = panelWidth / 2;
        int drawWidth = 0;
        int drawHeight = 0;

        if (activePuzzleImage != null) {
            int imgWidth = activePuzzleImage.getWidth();
            int imgHeight = activePuzzleImage.getHeight();

            // ปรับขนาดภาพให้ไม่ใหญ่เกินพื้นที่จอ โดยคงอัตราส่วนเดิม
            double scale = Math.min((panelWidth - 120) / (double) imgWidth,
                                    (panelHeight - 160) / (double) imgHeight);
            scale = Math.min(1.0, Math.max(0.1, scale)); // ป้องกัน scale ผิดปกติ

            drawWidth = (int) Math.round(imgWidth * scale);
            drawHeight = (int) Math.round(imgHeight * scale);
            drawX = (panelWidth - drawWidth) / 2;
            drawY = (panelHeight - drawHeight) / 2 - 20;

            g2d.drawImage(activePuzzleImage, drawX, drawY, drawWidth, drawHeight, null);
        }
    }

    // วาดประตูทั้งหมดในด่าน
    private void drawDoors(Graphics2D g2d) {
        for (Door door : levels.get(currentLevelIndex).doors) {
            if (!isDoorVisible(door)) {
                continue; // ไม่มีไฟส่องถึงก็ไม่วาดประตู ทำให้ผู้เล่นมองไม่เห็น
            }
            door.draw(g2d, panelWidth, panelHeight); // ใช้สไปรต์ door.png วาดประตู
        }
    }

    // วาดผู้เล่น
    private void drawPlayer(Graphics2D g2d) {
        player.draw(g2d);
    }

    // วาดมอนสเตอร์
    private void drawMonsters(Graphics2D g2d) {
        for (MonsterController controller : monsters) {
            if (controller.isActive()) { // วาดเฉพาะมอนสเตอร์ที่เปิดใช้งานในเลเวลนี้
                    Monster monster = controller.getMonster();

                    // วาดมอนสเตอร์เป็นสี่เหลี่ยมสีม่วง
                    g2d.setColor(new Color(0xEE82EE));
                    g2d.fillRect(monster.getX(), monster.getY(), monster.getSize(), monster.getSize());
                }
            }
        }
        

    // สร้างเลเยอร์ความมืดครอบทั้งแผนที่แล้วเจาะรูให้เห็นรอบตัวละคร
    private void drawLighting(Graphics2D g2d) {
        BufferedImage darkness = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gDark = darkness.createGraphics();

        gDark.setComposite(AlphaComposite.Src); // เริ่มด้วยการวาดทับทั้งหมด
        gDark.setColor(new Color(0, 0, 0, 230)); // สีดำโปร่งใสบางส่วนเพื่อให้รู้สึกว่ามืด
        gDark.fillRect(0, 0, panelWidth, panelHeight);

        // เปิดไฟรอบผู้เล่น
        punchLight(gDark, player.getCenterX(), player.getCenterY(), PLAYER_LIGHT_RADIUS);

        // เปิดไฟรอบมอนสเตอร์ที่ยังทำงานอยู่
        for (MonsterController controller : monsters) {
            if (controller.isActive()) {
                Monster monster = controller.getMonster();
                punchLight(gDark,
                        monster.getX() + monster.getSize() / 2,
                        monster.getY() + monster.getSize() / 2,
                        MONSTER_LIGHT_RADIUS);
            }
        }

        gDark.dispose();
        g2d.drawImage(darkness, 0, 0, null); // ซ้อนเลเยอร์ความมืดทับลงไป
    }

    // เจาะรูให้เลเยอร์มืดกลายเป็นวงแสงแบบค่อย ๆ จางออก
    private void punchLight(Graphics2D gDark, int centerX, int centerY, int radius) {
        Paint oldPaint = gDark.getPaint();
        Composite oldComposite = gDark.getComposite();

        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(centerX, centerY),
                radius,
                new float[] { 0f, 1f },
                new Color[] { new Color(1f, 1f, 1f, 1f), new Color(1f, 1f, 1f, 0f) }
        );

        gDark.setComposite(AlphaComposite.DstOut); // ลบอัลฟาของเลเยอร์มืดตามไล่สี
        gDark.setPaint(paint);
        gDark.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        gDark.setPaint(oldPaint);
        gDark.setComposite(oldComposite);
    }

    // ตรวจว่าประตูอยู่ในรัศมีไฟของผู้เล่นหรือมอนสเตอร์หรือไม่
    private boolean isDoorVisible(Door door) {
        int centerX = door.getX(panelWidth) + DOOR_SIZE / 2;
        int centerY = door.getY(panelHeight) + DOOR_SIZE / 2;

        if (isPointLit(centerX, centerY, player.getCenterX(),
                player.getCenterY(), PLAYER_LIGHT_RADIUS)) {
            return true; // ไฟจากผู้เล่นถึง
        }

        for (MonsterController controller : monsters) {
            if (controller.isActive()) {
                Monster monster = controller.getMonster();
                if (isPointLit(centerX, centerY,
                        monster.getX() + monster.getSize() / 2,
                        monster.getY() + monster.getSize() / 2,
                        MONSTER_LIGHT_RADIUS)) {
                    return true; // ไฟจากมอนสเตอร์ถึง
                }
            }
        }

        return false; // ไม่มีไฟส่องถึงประตูเลย
    }

    // ฟังก์ชันคำนวณระยะระหว่างจุดกับแหล่งกำเนิดไฟ
    private boolean isPointLit(int px, int py, int lx, int ly, int radius) {
        int dx = px - lx;
        int dy = py - ly;
        return dx * dx + dy * dy <= radius * radius;
    }

    // อัปเดตเฟรมแอนิเมชันของประตูทุกบานในด่านปัจจุบัน
    private void updateDoorAnimations() {
        for (Door door : levels.get(currentLevelIndex).doors) {
            door.updateAnimation();
        }
    }


    // ---------- ส่วนอัปเดตเกมแต่ละเฟรม ----------
    @Override
    public void actionPerformed(ActionEvent e) {
        updateDoorAnimations(); // ทำให้ประตูมีการเปลี่ยนเฟรมอยู่ตลอด

        if (!doorInteractionActive || player.isDead()) {
            player.update(); // อัปเดตตำแหน่งของผู้เล่นจากการกดปุ่ม
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
            return; // หยุดการคำนวณอื่น ๆ ขณะเปิดภาพหรือกล่องรหัสประตู
        }

        // อัปเดตมอนสเตอร์แต่ละตัว
        for (MonsterController controller : monsters) {
            controller.update(player.getX(), player.getY(), panelWidth, panelHeight);
        }

        // หลังอัปเดตมอนสเตอร์ เช็กว่าผู้เล่นโดนวงสตันหรือไม่
        updateStunEffects();

        // ตรวจการชนกับประตูและมอนสเตอร์
        checkDoorCollisions();
        checkMonsterCollisions();

        repaint(); // วาดเฟรมใหม่
    }

    // ---------- ตรวจการชนกับประตู ----------
    private void checkDoorCollisions() {
        if (player.isDead() || doorInteractionActive) {
            return;
        }
        Level level = levels.get(currentLevelIndex);
        for (Door door : level.doors) {
            int doorX = door.getX(panelWidth);
            int doorY = door.getY(panelHeight);
            if (player.intersects(doorX, doorY, DOOR_SIZE, DOOR_SIZE)) {
                handleDoorCollision(level, door);
                break;
            }
        }
    }

    // จัดการเหตุการณ์เมื่อผู้เล่นชนประตูแต่ละประเภท
    private void handleDoorCollision(Level level, Door door) {
        doorInteractionActive = true;
        activeDoor = door;
        player.stopImmediately();

        switch (door.getType()) {
            case PUZZLE:
                Integer number = door.getPuzzleNumber();
                if (number != null && number >= 1 && number <= puzzleImages.length) {
                    activePuzzleImage = puzzleImages[number - 1];
                    activePuzzleNumber = number;
                } else {
                    activePuzzleImage = null;
                    activePuzzleNumber = null;
                }
                break;
            case ADVANCE:
            case BACK:
                activePuzzleImage = null;
                activePuzzleNumber = null;
                promptDoorPassword(level, door);
                break;
        }
        repaint();
    }

    // แสดงกล่องให้ใส่รหัสผ่าน 2 หลัก ก่อนจะข้ามด่านหรือย้อนกลับ
    private void promptDoorPassword(Level level, Door door) {
        int password = level.getPasswordCode();
        String formatted = String.format("%02d", password);
        String message = "Please enter password";
        String input = JOptionPane.showInputDialog(this, message, "รหัสประตู", JOptionPane.QUESTION_MESSAGE);

        if (input != null) {
            input = input.trim();
        }

        boolean correct = input != null && (input.equals(formatted) || parsePassword(input) == password);

        if (correct) {
            if (door.getType() == Door.Type.ADVANCE) {
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

        pushPlayerAwayFromDoor(door);
        clearDoorInteractionState();
        repaint();
    }

    // แปลงสตริงเป็นตัวเลข ถ้าแปลงไม่ได้จะคืนค่า -1
    private int parsePassword(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    // ปิดหน้าต่างภาพ Puzzle และให้มอนสเตอร์กลับมาเคลื่อนไหวได้
    private void closePuzzleOverlay() {
        // ดันผู้เล่นออกจากประตูเพื่อไม่ให้ชนซ้ำทันทีเมื่อกลับมาเดินต่อ
        pushPlayerAwayFromDoor(activeDoor);
        clearDoorInteractionState();
        repaint();
    }

    private void clearDoorInteractionState() {
        doorInteractionActive = false;
        activeDoor = null;
        activePuzzleImage = null;
        activePuzzleNumber = null;
        requestFocusInWindow();
    }

    // ดันผู้เล่นออกจากประตูเล็กน้อยเพื่อป้องกันการชนติด
    private void pushPlayerAwayFromDoor(Door door) {
        if (door == null) {
            return;
        }
        int doorX = door.getX(panelWidth);
        int doorY = door.getY(panelHeight);
        player.pushOutsideSquare(doorX, doorY, DOOR_SIZE);
    }

    // ตรวจว่าปุ่มที่กดเป็นปุ่มทิศทางหรือปุ่ม WASD หรือไม่
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

    // ---------- ตรวจเอฟเฟกต์สตันจาก StunMonster ----------
    private void updateStunEffects() {
        if (player.isDead()) {
            return;
        }

        for (MonsterController controller : monsters) {
            if (!controller.isActive()) {
                continue;
            }

            Monster monster = controller.getMonster();
            if (monster instanceof StunMonster) {
                StunMonster stunMonster = (StunMonster) monster;

                // ถ้าผู้เล่นยืนอยู่ในวงสตันและยังไม่โดนสตันมาก่อน ให้ล็อกการเคลื่อนไหวทันที
                if (stunMonster.isPlayerInStun(player.getX(), player.getY(), PLAYER_SIZE) && !player.isStunned()) {
                    player.applyStun(stunMonster.getStunDurationTicks());
                }
            }
        }
    }

    // ---------- ตรวจการชนกับมอนสเตอร์ ----------
    private void checkMonsterCollisions() {
        if (player.isDead()) {
            return;
        }
        for (MonsterController controller : monsters) {
            if (controller.isActive()) {
                Monster monster = controller.getMonster();
                if (player.intersects(monster.getX(), monster.getY(),
                                      monster.getSize(), monster.getSize())) {
                    // ถ้าโดนมอนสเตอร์ → เล่นแอนิเมชันตายก่อนรีเซ็ต
                    player.die();
                    pendingLevelReset = 0;
                    break;
                }
            }
        }
    }

    // ---------- คลาสย่อย Level ----------
    private static class Level {
        private List<Door> doors = new ArrayList<>();
        private int passwordCode = 0; // ผลรวมของเลข Puzzle ทั้ง 4 บาน

        private Level() {}

        // สุ่มประตูแต่ละบาน (ชนิด + ตำแหน่ง) และกำหนดเลข Puzzle แบบไม่ซ้ำ
        private void randomizeDoors(Random random, int panelWidth, int panelHeight) {
            List<Door.Type> types = new ArrayList<>();
            types.add(Door.Type.ADVANCE);
            types.add(Door.Type.BACK);
            // ที่เหลือเป็น NEUTRAL
            while (types.size() < DOORS_PER_LEVEL) {
                types.add(Door.Type.PUZZLE);
            }

            Collections.shuffle(types, random);

            List<Point> points = generateDistinctDoorPositions(random, panelWidth, panelHeight);
            doors = new ArrayList<>();
            passwordCode = 0;

            List<Integer> puzzlePool = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                puzzlePool.add(i);
            }
            Collections.shuffle(puzzlePool, random);
            List<Integer> selectedNumbers = new ArrayList<>(puzzlePool.subList(0, 4));
            int sum = 0;
            for (int value : selectedNumbers) {
                sum += value;
            }
            passwordCode = sum;
            Collections.shuffle(selectedNumbers, random); // สุ่มลำดับการปรากฏของภาพ
            int puzzleIndex = 0;

            for (int i = 0; i < DOORS_PER_LEVEL; i++) {
                Point p = points.get(i);
                Door door = new Door(types.get(i), p.x, p.y, panelWidth, panelHeight, DOOR_SIZE);
                if (door.getType() == Door.Type.PUZZLE && puzzleIndex < selectedNumbers.size()) {
                    int number = selectedNumbers.get(puzzleIndex++);
                    door.setPuzzleNumber(number);
                }
                doors.add(door);
            }
        }
        
        private int getPasswordCode() {
            return passwordCode;
        }


        // ฟังก์ชันสร้างตำแหน่งประตูไม่ให้ทับกัน
        private List<Point> generateDistinctDoorPositions(Random random, int panelWidth, int panelHeight) {
            List<Point> points = new ArrayList<>();
            int attempts = 0;

            // สุ่มพิกัดจนกว่าจะได้ครบหรือครบจำนวนครั้งสูงสุด
            while (points.size() < DOORS_PER_LEVEL && attempts < 10_000) {
                attempts++;
                int xRange = Math.max(1, panelWidth - 100 - DOOR_SIZE);
                int yRange = Math.max(1, panelHeight - 150 - DOOR_SIZE);

                int x = 50 + random.nextInt(xRange);
                int y = 80 + random.nextInt(yRange);
                Point candidate = new Point(x, y);

                boolean overlaps = false;
                for (Point existing : points) {
                    // ถ้าใกล้กันเกินไปให้ถือว่าทับกัน
                    if (existing.distanceSquared(candidate) < (DOOR_SIZE + 10) * (DOOR_SIZE + 10)) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps) {
                    points.add(candidate);
                }
            }

            // ถ้ายังไม่ครบ ให้เติม dummy door
            while (points.size() < DOORS_PER_LEVEL) {
                int fallbackX = Math.min(panelWidth - DOOR_SIZE, 60 * points.size());
                int fallbackY = Math.min(panelHeight - DOOR_SIZE, 100 + 40 * points.size());
                points.add(new Point(Math.max(0, fallbackX), Math.max(0, fallbackY)));
            }
            return points;
        }
    }

    // ---------- คลาสจุดพิกัด ----------
    private static class Point {
        private final int x, y;
        private Point(int x, int y) { this.x = x; this.y = y; }

        // คำนวณระยะห่างกำลังสอง (เพื่อใช้เช็กว่าทับกันไหม)
        private int distanceSquared(Point other) {
            int dx = x - other.x;
            int dy = y - other.y;
            return dx * dx + dy * dy;
        }
    }

    // ---------- คลาสควบคุมมอนสเตอร์ ----------
    private static class MonsterController {
        private final Monster monster;       // ตัวมอนสเตอร์จริง
        private final boolean[] activeLevels; // ด่านที่มอนสเตอร์นี้จะปรากฏ
        private boolean active;               // สถานะเปิดใช้งานในด่านปัจจุบัน

        // กำหนดมอนสเตอร์และด่านที่มันจะออก
        private MonsterController(Monster monster, int[] activeLevelIndices) {
            this.monster = monster;
            this.activeLevels = new boolean[TOTAL_LEVELS];
            for (int idx : activeLevelIndices) {
                if (idx >= 0 && idx < TOTAL_LEVELS) {
                    this.activeLevels[idx] = true;
                }
            }
        }

        // เตรียมมอนสเตอร์สำหรับด่านที่กำหนด
        private void prepareForLevel(int levelIndex, Random random, int panelWidth, int panelHeight) {
            active = levelIndex >= 0 && levelIndex < activeLevels.length && activeLevels[levelIndex];
            if (active) {
                monster.spawn(random, panelWidth, panelHeight);
                // ให้เริ่มวงสตั้นทันทีหลังสปอว์น เพื่อให้ผู้เล่นเห็นการทำงาน
                if (monster instanceof StunMonster) {
                    ((StunMonster) monster).triggerStun();
                }
            }
        }
        

        // อัปเดตมอนสเตอร์แต่ละเฟรม
        private void update(int playerX, int playerY, int panelWidth, int panelHeight) {
            if (!active) return;
            monster.update(playerX, playerY, panelWidth, panelHeight);
        }

        private boolean isActive() { return active; }
        private Monster getMonster() { return monster; }
    }
}