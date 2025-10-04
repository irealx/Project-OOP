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


// ---------- คลาสหลักของเกม ----------
public class GamePanel extends JPanel implements ActionListener {

    // ขนาดหน้าจอเกม
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

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
    private final Player player = new Player(PLAYER_SIZE, PLAYER_SPEED, WIDTH, HEIGHT); // ผู้เล่น
    private final List<MonsterController> monsters = new ArrayList<>(); // รายชื่อมอนสเตอร์
    private final List<Level> levels = new ArrayList<>();      // รายชื่อด่านทั้งหมด

    // ภาพพื้นหลังที่ใช้กับทุกเลเวล
    private final BufferedImage backgroundImage;

    private int currentLevelIndex = 0; // ด่านปัจจุบัน
    private Integer pendingLevelReset = null; // เก็บด่านที่จะรีเซ็ตหลังเล่นแอนิเมชันตาย

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
                player.handleKeyPressed(e.getKeyCode()); // กดปุ่ม → ส่งให้ Player จัดการ
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

    // ---------- รีเซ็ตสถานะเมื่อเริ่มหรือเปลี่ยนด่าน ----------
    private void resetForLevel(int levelIndex) {
        currentLevelIndex = levelIndex; // ตั้งค่าด่านปัจจุบัน
        pendingLevelReset = null; // เคลียร์สถานะรีเซ็ตที่ค้างอยู่
        Level level = levels.get(levelIndex); // ดึงด่านที่เลือกมา
        level.randomizeDoors(random); // สุ่มตำแหน่งและชนิดของประตูใหม่ทุกครั้ง
        player.spawn(); // วางผู้เล่นที่จุดเริ่มต้นกลางจอ

        // ให้มอนสเตอร์แต่ละตัว spawn ถ้าด่านนั้นเป็นด่านที่มันใช้งาน
        for (MonsterController controller : monsters) {
            controller.prepareForLevel(levelIndex, random, WIDTH, HEIGHT);
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
        drawLevelInfo(g2d);
    }

    // วาดพื้นหลังให้เต็มหน้าจอ ถ้ามีไฟล์ภาพที่โหลดได้
    private void drawBackground(Graphics2D g2d) {
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, null); // ขยายภาพให้เต็มพื้นที่เกม
        } else {
            g2d.setColor(Color.BLACK); // สำรอง: ถ้าโหลดภาพไม่ได้ให้พื้นหลังเป็นสีดำ
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    // วาดข้อความแสดงข้อมูลของเลเวล
    private void drawLevelInfo(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("Level " + (currentLevelIndex + 1), 20, 30); // แสดงชื่อด่านมุมซ้ายบน
    }

    // วาดประตูทั้งหมดในด่าน
    private void drawDoors(Graphics2D g2d) {
        for (Door door : levels.get(currentLevelIndex).doors) {
            if (!isDoorVisible(door)) {
                continue; // ไม่มีไฟส่องถึงก็ไม่วาดประตู ทำให้ผู้เล่นมองไม่เห็น
            }
            // เปลี่ยนสีตามประเภทของประตู
            switch (door.type) {
                case ADVANCE: // ไปด่านต่อไป
                    g2d.setColor(new Color(0x3CB371)); // เขียว
                    break;
                case BACK: // ย้อนกลับ
                    g2d.setColor(new Color(0xCD5C5C)); // แดง
                    break;
                default: // NEUTRAL ไม่มีอะไรเกิดขึ้น
                    g2d.setColor(new Color(0x4682B4)); // น้ำเงิน
            }
            g2d.fillRect(door.x, door.y, DOOR_SIZE, DOOR_SIZE); // วาดสี่เหลี่ยมแทนประตู
        }
    }

    // วาดผู้เล่น
    private void drawPlayer(Graphics2D g2d) {
        player.draw(g2d);
    }

    // วาดมอนสเตอร์        punchLight(gDark, player.x + PLAYER_SIZE / 2, player.y + PLAYER_SIZE / 2, PLAYER_LIGHT_RADIUS);
    private void drawMonsters(Graphics2D g2d) {
        g2d.setColor(new Color(0xEE82EE)); // สีม่วง
        for (MonsterController controller : monsters) {
            if (controller.isActive()) { // วาดเฉพาะมอนสเตอร์ที่เปิดใช้งานในเลเวลนี้
                Monster monster = controller.getMonster();
                g2d.fillRect(monster.getX(), monster.getY(), monster.getSize(), monster.getSize());
            }
        }
    }

    // สร้างเลเยอร์ความมืดครอบทั้งแผนที่แล้วเจาะรูให้เห็นรอบตัวละคร
    private void drawLighting(Graphics2D g2d) {
        BufferedImage darkness = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gDark = darkness.createGraphics();

        gDark.setComposite(AlphaComposite.Src); // เริ่มด้วยการวาดทับทั้งหมด
        gDark.setColor(new Color(0, 0, 0, 230)); // สีดำโปร่งใสบางส่วนเพื่อให้รู้สึกว่ามืด
        gDark.fillRect(0, 0, WIDTH, HEIGHT);

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
        int centerX = door.x + DOOR_SIZE / 2;
        int centerY = door.y + DOOR_SIZE / 2;

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

    // ---------- ส่วนอัปเดตเกมแต่ละเฟรม ----------
    @Override
    public void actionPerformed(ActionEvent e) {
        player.update(); // อัปเดตตำแหน่งของผู้เล่นจากการกดปุ่ม

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
        // อัปเดตมอนสเตอร์แต่ละตัว
        for (MonsterController controller : monsters) {
            controller.update(player.getX(), player.getY(), WIDTH, HEIGHT);
        }

        // ตรวจการชนกับประตูและมอนสเตอร์
        checkDoorCollisions();
        checkMonsterCollisions();

        repaint(); // วาดเฟรมใหม่
    }

    // ---------- ตรวจการชนกับประตู ----------
    private void checkDoorCollisions() {
        if (player.isDead()) {
            return;
        }
        Level level = levels.get(currentLevelIndex);
        for (Door door : level.doors) {
            // ถ้าผู้เล่นชนประตู
            if (player.intersects(door.x, door.y, DOOR_SIZE, DOOR_SIZE)) {
                switch (door.type) {
                    case ADVANCE:
                        // ไปด่านถัดไป (วนกลับถ้าถึงด่านสุดท้าย)
                        int nextLevel = (currentLevelIndex + 1) % TOTAL_LEVELS;
                        resetForLevel(nextLevel);
                        break;
                    case BACK:
                        // ย้อนกลับด่านก่อนหน้า (วนไปด่านสุดท้ายถ้าอยู่ด่านแรก)
                        int prevLevel = (currentLevelIndex - 1 + TOTAL_LEVELS) % TOTAL_LEVELS;
                        resetForLevel(prevLevel);
                        break;
                    default:
                        // NEUTRAL ไม่เกิดอะไรขึ้น
                        break;
                }
                break; // ออกจาก loop หลังชนประตูหนึ่งบาน
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

    // ---------- Enum ประเภทของประตู ----------
    private enum DoorType {
        NEUTRAL, // ไม่มีผล
        ADVANCE, // ไปด่านต่อไป
        BACK     // ย้อนกลับด่าน
    }

    // ---------- คลาสย่อย Door ----------
    private static class Door {
        private final DoorType type; // ประเภทของประตู
        private final int x;         // ตำแหน่ง X
        private final int y;         // ตำแหน่ง Y

        private Door(DoorType type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    // ---------- คลาสย่อย Level ----------
    private static class Level {
        private List<Door> doors = new ArrayList<>();

        private Level() {}

        // สุ่มประตูแต่ละบาน (ชนิด + ตำแหน่ง)
        private void randomizeDoors(Random random) {
            List<DoorType> types = new ArrayList<>();
            types.add(DoorType.ADVANCE);
            types.add(DoorType.BACK);
            // ที่เหลือเป็น NEUTRAL
            while (types.size() < DOORS_PER_LEVEL) {
                types.add(DoorType.NEUTRAL);
            }
            // สุ่มเรียงลำดับใหม่
            Collections.shuffle(types, random);

            // สุ่มตำแหน่งไม่ให้ซ้ำ
            List<Point> points = generateDistinctDoorPositions(random);
            doors = new ArrayList<>();
            for (int i = 0; i < DOORS_PER_LEVEL; i++) {
                Point p = points.get(i);
                doors.add(new Door(types.get(i), p.x, p.y));
            }
        }

        // ฟังก์ชันสร้างตำแหน่งประตูไม่ให้ทับกัน
        private List<Point> generateDistinctDoorPositions(Random random) {
            List<Point> points = new ArrayList<>();
            int attempts = 0;

            // สุ่มพิกัดจนกว่าจะได้ครบหรือครบจำนวนครั้งสูงสุด
            while (points.size() < DOORS_PER_LEVEL && attempts < 10_000) {
                attempts++;
                int x = 50 + random.nextInt(WIDTH - 100 - DOOR_SIZE);
                int y = 80 + random.nextInt(HEIGHT - 150 - DOOR_SIZE);
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
                points.add(new Point(60 * points.size(), 100));
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