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
        load("idle",   "Pic/character/Mon/idle.png",   4);
        load("death",  "Pic/character/Mon/death.png", 10);
        load("skill1", "Pic/character/Mon/skill1.png",10);
        load("summon", "Pic/character/Mon/summon.png", 4);
    }

    /**
     * โหลดภาพ sprite sheet และตัดออกเป็นเฟรม
     */
    private void load(String name, String path, int frames) {
        try {
            BufferedImage sheet = ImageIO.read(new File(path));
            int frameCount = Math.min(frames, sheet.getWidth() / FRAME_WIDTH);
            BufferedImage[] arr = new BufferedImage[frameCount];

            for (int i = 0; i < frameCount; i++) {
                arr[i] = sheet.getSubimage(i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
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
}
