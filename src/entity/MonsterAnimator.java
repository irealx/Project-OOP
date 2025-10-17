package entity;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * โหลดและจัดเก็บภาพอนิเมชันของมอนสเตอร์ทั้งหมดในเกม
 * - รองรับ sprite sheet ที่แต่ละเฟรมมีขนาด 100x100
 * - ใช้ร่วมกันทุกตัว ไม่ต้องโหลดซ้ำ
 */
public class MonsterAnimator {

    private static final int FRAME_WIDTH = 100;   // ความกว้างของเฟรม
    private static final int FRAME_HEIGHT = 100;  // ความสูงของเฟรม

    private final Map<String, BufferedImage[]> animations = new HashMap<>();

    public MonsterAnimator() {
        // โหลด sprite ทั้งหมดที่ใช้ในเกม (เพิ่มหรือแก้ path ได้ง่ายในอนาคต)
        load("idle",        "Pic/character/Mon/idle.png",        4);
        load("death",       "Pic/character/Mon/death.png",      20);
        load("skill1",      "Pic/character/Mon/skill1.png",     10);
        load("summon",      "Pic/character/Mon/summon.png",     10);
        load("summonIdle",  "Pic/character/Mon/summonIdle.png",  4);

        mirrorFrom("death_reverse", "death");
    }

    /**
     * โหลดภาพ sprite sheet และตัดออกเป็นเฟรม
     */
    private void load(String name, String path, int frames) {
        try {
            BufferedImage sheet = ImageIO.read(new File(path));
            int cols = Math.max(1, sheet.getWidth() / FRAME_WIDTH);
            int rows = Math.max(1, sheet.getHeight() / FRAME_HEIGHT);
            int available = cols * rows;
            int frameCount = Math.min(frames, available);
            BufferedImage[] arr = new BufferedImage[frameCount];

            for (int i = 0; i < frameCount; i++) {
                int col = i % cols;
                int row = i / cols;
                if (row >= rows) break; // ป้องกัน index เกินภาพจริง

                int x = col * FRAME_WIDTH;
                int y = row * FRAME_HEIGHT;
                arr[i] = sheet.getSubimage(x, y, FRAME_WIDTH, FRAME_HEIGHT);
            }

            animations.put(name, arr);
        } catch (IOException e) {
            System.err.println("❌ Cannot load: " + path);
            animations.put(name, new BufferedImage[0]); // ป้องกัน NullPointer
        } catch (Exception e) {
            System.err.println("⚠ Unexpected error loading " + name + ": " + e.getMessage());
            animations.put(name, new BufferedImage[0]);
        }
    }

    /**
     * ดึงภาพตามชื่ออนิเมชัน เช่น "idle", "death"
     */
    public BufferedImage[] get(String name) {
        return animations.getOrDefault(name, new BufferedImage[0]);
    }

    /**
     * สร้างชุดเฟรมแบบย้อนกลับจากอนิเมชันเดิม เช่น death -> death_reverse
     */
    private void mirrorFrom(String target, String source) {
        BufferedImage[] origin = animations.get(source);
        if (origin == null || origin.length == 0) {
            animations.put(target, new BufferedImage[0]);
            return;
        }

        BufferedImage[] reversed = new BufferedImage[origin.length];
        for (int i = 0; i < origin.length; i++) {
            reversed[i] = origin[origin.length - 1 - i];
        }
        animations.put(target, reversed);
    }
}
