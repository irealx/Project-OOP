package system;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

// จัดการระบบภาพ Puzzle ที่แสดงเมื่อผู้เล่นชนประตู
public class Puzzle {

    private static final int TOTAL = 9; // จำนวนภาพ Puzzle ทั้งหมด (1–9)
    private final BufferedImage[] images = new BufferedImage[TOTAL];
    private BufferedImage active; // ภาพที่กำลังแสดงอยู่ในขณะนั้น

    public Puzzle() {
        loadImages(); // โหลดภาพทั้งหมดตั้งแต่เริ่มต้น
    }

    // แสดงภาพ Puzzle ตามหมายเลขที่ระบุ (1–9)
    public void show(Integer number) {
        if (number == null || number < 1 || number > TOTAL) {
            active = null;
            return;
        }
        active = images[number - 1];
    }

    // ล้างภาพที่กำลังแสดงอยู่
    public void clear() {
        active = null;
    }

    // วาดภาพ Puzzle บนหน้าจอโดยมีพื้นมืดโปร่งแสง
    public void draw(Graphics2D g, int width, int height) {
        if (active == null) return;

        var oldClip = g.getClip();          // เก็บ clip เดิมไว้ก่อน
        g.setClip(0, 0, width, height);     // ตั้ง clip ครอบพื้นที่ทั้งหมด

        // วาดภาพ Puzzle ด้วย overlay โปร่งแสง
        EffectRenderer.drawPuzzleOverlay(g, active, Config.PUZZLE_SHOW_ALPHA / 255f);

        g.setClip(oldClip);                 // คืนค่า clip เดิม
    }

    // โหลดภาพ Puzzle ทั้งหมดจากโฟลเดอร์
    private void loadImages() {
        for (int i = 1; i <= TOTAL; i++) {
            try {
                // โหลดภาพ pz1.png ถึง pz9.png จากโฟลเดอร์ Pic/character/puzzle
                images[i - 1] = ImageIO.read(
                        new File(String.format("Pic/character/puzzle/pz%d.png", i))
                );
            } catch (IOException ex) {
                images[i - 1] = null; // ถ้าโหลดไม่ได้ให้เป็น null เพื่อป้องกัน error ตอนวาด
            }
        }
    }
}
