package entity;

import java.util.Random;

/**
/**
 * คลาส abstract พื้นฐานของมอนสเตอร์ทุกประเภท
 * รวมระบบเปิด/ปิดตามด่านแทน MonsterController เดิม
 */
public abstract class Monster extends Sprite {

    private boolean[] activeLevels = new boolean[0]; // แผนที่ว่ามอนสเตอร์โผล่ในด่านไหนบ้าง
    private boolean active;                          // มอนสเตอร์ทำงานอยู่ไหมในด่านปัจจุบัน

    // เลือกมุมแบบสุ่ม
    protected Monster(int size, int speed) {
        super(size, speed);
    }

    /**
     * กำหนดว่ามอนสเตอร์ตัวนี้จะปรากฏในด่านใดบ้าง
     * 
     * @param totalLevels จำนวนด่านทั้งหมดของเกม
     * @param levelGroups รายการ index ของด่าน (กำหนดหลายชุดได้)
     */
    public void setActiveLevels(int totalLevels, int[]... levelGroups) {
        this.activeLevels = buildActiveLevelMap(totalLevels, levelGroups);
    }

    /** เรียกทุกครั้งเมื่อเข้าสู่ด่านใหม่เพื่อเตรียมมอนสเตอร์ให้พร้อม */
    public void prepareForLevel(int levelIndex, Random random, int panelWidth, int panelHeight) {
        updateBounds(panelWidth, panelHeight);
        active = levelIndex >= 0 && levelIndex < activeLevels.length && activeLevels[levelIndex];
        if (!active) {
            return;
        }
        spawnAtRandomCorner(random);
        onLevelActivated(random);
    }

    /** Hook สำหรับ subclass ใช้ตั้งค่าพิเศษหลัง spawn (เช่น สั่งปล่อยวงสตันทันที) */
    protected void onLevelActivated(Random random) {
        // ค่าเริ่มต้นไม่ทำอะไร
    }

    /** อัปเดตพฤติกรรมของมอนสเตอร์ในแต่ละเฟรม (เรียกเฉพาะเมื่อ active) */
    public final void update(int playerX, int playerY) {
        if (!active) {
            return;
        }
        behave(playerX, playerY);
    }

    /** ให้ subclass ระบุพฤติกรรมการเคลื่อนที่จริง ๆ */
    protected abstract void behave(int playerX, int playerY);

    /** มอนสเตอร์ทำงานอยู่ในด่านปัจจุบันหรือไม่ */
    public boolean isActive() {
        return active;
    }

    private static boolean[] buildActiveLevelMap(int totalLevels, int[]... levelGroups) {
        int length = Math.max(0, totalLevels);
        if (length == 0) {
            for (int[] group : levelGroups) {
                if (group == null) {
                    continue;
                }
                for (int idx : group) {
                    length = Math.max(length, idx + 1);
                }
            }
        }

        boolean[] result = new boolean[length];
        for (int[] group : levelGroups) {
            if (group == null) {
                continue;
            }
            for (int idx : group) {
                if (idx >= 0 && idx < result.length) {
                    result[idx] = true;
                }
            }
        }
        return result;
    }
}