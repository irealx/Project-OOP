package system;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * คลาส Door แยกออกมาเพื่อดูแลข้อมูลของประตูแต่ละบานในแผนที่
 * - เก็บประเภท ตำแหน่ง และเลขภาพ Puzzle
 * - รับผิดชอบการวาดประตูจากสไปรต์ชีต door.png
 */
public class Door {

    /**
     * ประเภทของประตูในเกม
     */
    public enum Type {
        PUZZLE,   // ประตูแสดงภาพ Puzzle
        ADVANCE,  // ประตูไปด่านถัดไป
        BACK      // ประตูย้อนกลับไปด่านก่อนหน้า
    }

    // ค่าคงที่ของสไปรต์ประตู
    private static final int FRAME_COUNT = 8;          // จำนวนเฟรมในภาพ door.png
    private static final int SPRITE_SIZE = 128;        // ขนาดแต่ละเฟรมในสไปรต์ชีต
    private static final int FRAME_DELAY = 6;          // จำนวนการอัปเดตก่อนเปลี่ยนเฟรม (ใช้เวลาวาดแบบ fallback)

    // สีสำรองเมื่อโหลดภาพไม่ได้ (ใช้โทนต่างกันตามประเภท)
    private static final Color COLOR_PUZZLE = new Color(0x4682B4);
    private static final Color COLOR_ADVANCE = new Color(0x3CB371);
    private static final Color COLOR_BACK = new Color(0xCD5C5C);

    private static final BufferedImage[] DOOR_FRAMES = loadDoorFrames(); // โหลดภาพทุกเฟรมไว้ครั้งเดียว

    private final Type type;     // ประเภทของประตู
    private final double xRatio; // ตำแหน่งแกน X แบบอัตราส่วน (เพื่อรองรับการย่อ/ขยายฉาก)
    private final double yRatio; // ตำแหน่งแกน Y แบบอัตราส่วน
    private final int size;      // ขนาดของประตูที่ใช้ในการวาดและคำนวณชน

    private int animationTick = 0; // ตัวจับเวลาของการเปลี่ยนเฟรม
    private int animationFrame = 0; // เฟรมปัจจุบันของประตูนี้
    private Integer puzzleNumber;  // เลขของภาพ Puzzle (1-9) สำหรับประตูประเภท PUZZLE

    /**
     * สร้างประตูพร้อมคำนวณตำแหน่งแบบอัตราส่วนให้ตรงกับขนาดฉากปัจจุบัน
     */
    public Door(Type type, int x, int y, int panelWidth, int panelHeight, int size) {
        this.type = type;
        this.size = size;
        int availableWidth = Math.max(1, panelWidth - size);
        int availableHeight = Math.max(1, panelHeight - size);
        this.xRatio = Math.max(0d, Math.min(1d, x / (double) availableWidth));
        this.yRatio = Math.max(0d, Math.min(1d, y / (double) availableHeight));
    }

    /**
     * อัปเดตเฟรมของแอนิเมชันประตู (สำหรับสร้างความรู้สึกว่ามีการเคลื่อนไหวเล็กน้อย)
     */
    public void updateAnimation() {
        if (DOOR_FRAMES.length == 0) {
            return; // ถ้าไม่มีภาพให้ใช้ ก็ไม่ต้องทำอะไร
        }
        animationTick++;
        if (animationTick >= FRAME_DELAY) {
            animationTick = 0;
            animationFrame = (animationFrame + 1) % DOOR_FRAMES.length;
        }
    }

    /**
     * วาดประตูลงบนฉากจากเฟรมปัจจุบัน ถ้าโหลดภาพไม่ได้จะวาดเป็นสี่เหลี่ยมสีแทน
     */
    public void draw(Graphics2D g2d, int panelWidth, int panelHeight) {
        int drawX = getX(panelWidth);
        int drawY = getY(panelHeight);

        if (DOOR_FRAMES.length > 0) {
            BufferedImage frame = DOOR_FRAMES[Math.min(animationFrame, DOOR_FRAMES.length - 1)];
            g2d.drawImage(frame, drawX, drawY, size, size, null);
            return;
        }

        // กรณีสำรอง: ถ้าโหลดภาพไม่ได้ ให้แสดงผลเป็นสีประจำประเภท
        g2d.setColor(getFallbackColor());
        g2d.fillRect(drawX, drawY, size, size);
    }

    /**
     * คืนค่าตำแหน่ง X ตามขนาดของฉากปัจจุบัน
     */
    public int getX(int panelWidth) {
        int availableWidth = Math.max(0, panelWidth - size);
        return Math.min(availableWidth, (int) Math.round(xRatio * availableWidth));
    }

    /**
     * คืนค่าตำแหน่ง Y ตามขนาดของฉากปัจจุบัน
     */
    public int getY(int panelHeight) {
        int availableHeight = Math.max(0, panelHeight - size);
        return Math.min(availableHeight, (int) Math.round(yRatio * availableHeight));
    }

    /**
     * ตั้งเลข Puzzle ให้ประตูประเภท PUZZLE
     */
    public void setPuzzleNumber(Integer puzzleNumber) {
        this.puzzleNumber = puzzleNumber;
    }

    /**
     * คืนค่าเลข Puzzle (1-9) ถ้าเป็นประตู Puzzle จะไม่เป็น null
     */
    public Integer getPuzzleNumber() {
        return puzzleNumber;
    }

    /**
     * คืนค่าประเภทของประตู
     */
    public Type getType() {
        return type;
    }

    /**
     * โหลดเฟรมทั้งหมดจากไฟล์ door.png หากโหลดไม่สำเร็จจะคืนอาร์เรย์ว่าง
     */
    private static BufferedImage[] loadDoorFrames() {
        try {
            BufferedImage spriteSheet = ImageIO.read(new File("Pic/door.png"));
            int availableFrames = Math.max(1, Math.min(FRAME_COUNT, spriteSheet.getWidth() / SPRITE_SIZE));
            BufferedImage[] frames = new BufferedImage[availableFrames];
            for (int i = 0; i < availableFrames; i++) {
                int x = i * SPRITE_SIZE;
                int width = Math.min(SPRITE_SIZE, spriteSheet.getWidth() - x);
                int height = Math.min(SPRITE_SIZE, spriteSheet.getHeight());
                frames[i] = spriteSheet.getSubimage(x, 0, width, height);
            }
            return frames;
        } catch (IOException e) {
            System.err.println("ไม่สามารถโหลดภาพประตูได้: " + e.getMessage());
            return new BufferedImage[0];
        }
    }

    /**
     * คืนค่าสีสำรองสำหรับใช้วาดสี่เหลี่ยมแทนประตู
     */
    private Color getFallbackColor() {
        switch (type) {
            case ADVANCE:
                return COLOR_ADVANCE;
            case BACK:
                return COLOR_BACK;
            case PUZZLE:
            default:
                return COLOR_PUZZLE;
        }
    }
}