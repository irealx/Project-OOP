package system;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

// จัดการข้อมูลและการแสดงผลของประตูในเกม
public class Door {

    public enum Type { PUZZLE, ADVANCE, BACK }

    private static final int FRAME_SIZE = 128;     // ขนาดของแต่ละเฟรมใน sprite sheet
    private static final int FRAMES = 8;           // จำนวนเฟรมทั้งหมด
    private static final int FRAME_DELAY = 6;      // หน่วงเวลาเปลี่ยนเฟรม
    private static final BufferedImage[] SPRITES = loadSprites(); // โหลดภาพประตูทั้งหมด

    private final Type type;
    private final double xRatio; // ตำแหน่งแนวนอน (เก็บเป็นอัตราส่วน เพื่อปรับตามขนาดจอ)
    private final double yRatio; // ตำแหน่งแนวตั้ง (อัตราส่วนเช่นกัน)
    private final int size;

    private int tick;
    private int frame;
    private Integer puzzleNumber; // หมายเลข puzzle ของประตู (ถ้ามี)

    public Door(Type type, int x, int y, int panelWidth, int panelHeight, int size) {
        this.type = type;
        this.size = size;

        // เก็บตำแหน่งในรูปแบบอัตราส่วนเพื่อให้ยืดหยุ่นตามขนาดหน้าจอ
        this.xRatio = Math.max(0, Math.min(1, x / (double) Math.max(1, panelWidth - size)));
        this.yRatio = Math.max(0, Math.min(1, y / (double) Math.max(1, panelHeight - size)));
    }

    public void setPuzzleNumber(Integer puzzleNumber) {
        this.puzzleNumber = puzzleNumber;
    }

    public Integer getPuzzleNumber() {
        return puzzleNumber;
    }

    public Type getType() {
        return type;
    }

    // อัปเดตอนิเมชันของประตู (วนเฟรมตามเวลา)
    public void updateAnimation() {
        if (SPRITES.length == 0) return;

        if (++tick >= FRAME_DELAY) {
            tick = 0;
            frame = (frame + 1) % SPRITES.length;
        }
    }

    // วาดภาพประตูตามตำแหน่งและขนาด
    public void draw(Graphics2D g, int width, int height) {
        int drawX = getX(width);
        int drawY = getY(height);

        if (SPRITES.length > 0) {
            // มี sprite — ใช้ภาพตามเฟรมปัจจุบัน
            g.drawImage(SPRITES[frame], drawX, drawY, size, size, null);
        } else {
            // กรณีไม่มีภาพ — วาดสี่เหลี่ยมแทนโดยใช้สีตามประเภทประตู
            g.setColor(switch (type) {
                case ADVANCE -> new Color(0x3CB371); // เขียว — ประตูไปต่อ
                case BACK -> new Color(0xCD5C5C);    // แดง — ประตูย้อนกลับ
                default -> new Color(0x4682B4);      // น้ำเงิน — ประตู puzzle
            });
            g.fillRect(drawX, drawY, size, size);
        }
    }

    // คำนวณตำแหน่ง X ตามอัตราส่วนของจอ
    public int getX(int width) {
        int available = Math.max(0, width - size);
        return (int) Math.round(xRatio * available);
    }

    // คำนวณตำแหน่ง Y ตามอัตราส่วนของจอ
    public int getY(int height) {
        int available = Math.max(0, height - size);
        return (int) Math.round(yRatio * available);
    }

    // โหลดภาพ sprite ของประตูจากไฟล์เดียว (door.png)
    private static BufferedImage[] loadSprites() {
        try {
            BufferedImage sheet = ImageIO.read(new File("Pic/door.png"));
            int count = Math.min(FRAMES, sheet.getWidth() / FRAME_SIZE);
            BufferedImage[] frames = new BufferedImage[count];

            // แบ่งภาพออกเป็นเฟรมย่อยตามจำนวนที่กำหนด
            for (int i = 0; i < count; i++) {
                frames[i] = sheet.getSubimage(i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
            }
            return frames;
        } catch (IOException ex) {
            return new BufferedImage[0]; // กรณีโหลดภาพไม่ได้ ให้คืนอาร์เรย์ว่าง
        }
    }
}
