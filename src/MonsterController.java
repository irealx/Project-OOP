import java.util.Random;
/**
 * คลาสควบคุมการทำงานของมอนสเตอร์แต่ละตัว
  * ทำหน้าที่เปิด/ปิดการทำงานตามด่านและสั่งให้มอนสเตอร์สุ่มจุดเกิด
 */
public class MonsterController {
    private final Monster monster;              // อินสแตนซ์ของมอนสเตอร์ที่ควบคุม
    private final boolean[] activeLevels;       // แผนที่ว่าด่านไหนเปิดการใช้งานมอนสเตอร์ตัวนี้
    private boolean active;                     // สถานะมอนสเตอร์ในด่านปัจจุบัน

    /**
     * สร้างคอนโทรลเลอร์พร้อมระบุจำนวนด่านทั้งหมด
     * @param monster มอนสเตอร์ที่ต้องการควบคุม
     * @param activeLevelIndices รายการด่านที่มอนสเตอร์จะปรากฏ
     * @param totalLevels จำนวนด่านทั้งหมดของเกม เพื่อเตรียมอาร์เรย์สถานะ
     */
    public MonsterController(Monster monster, int[] activeLevelIndices, int totalLevels) {
        this(monster, buildActiveLevelMap(totalLevels, activeLevelIndices));
    }

    /**
     * สร้างคอนโทรลเลอร์โดยรวมด่านจากหลายชุดเข้าไว้ด้วยกัน
     * กรณีนี้ถูกใช้กับมอนสเตอร์ที่ถูกกำหนดด่านซ้ำเป็นหลายกลุ่มใน GamePanel
     *
     * @param monster มอนสเตอร์ที่ต้องการควบคุม
     * @param primaryActiveLevels ชุดด่านหลักที่ต้องการเปิดใช้งาน
     * @param additionalActiveLevels ชุดด่านเสริมที่จะถูกรวมเข้าด้วยกัน
     * @param totalLevels จำนวนด่านทั้งหมดของเกม
     */
    public MonsterController(Monster monster, int[] primaryActiveLevels,
                             int[] additionalActiveLevels, int totalLevels) {
        this(monster, buildActiveLevelMap(totalLevels, primaryActiveLevels, additionalActiveLevels));
    }

    /**
     * สร้างคอนโทรลเลอร์โดยคำนวณจำนวนด่านจากค่าที่รับมาอัตโนมัติ
     * เหมาะสำหรับกรณีที่ไม่ต้องการระบุ totalLevels โดยตรง
     *
     * @param monster มอนสเตอร์ที่ต้องการควบคุม
     * @param activeLevelIndices รายการด่านที่มอนสเตอร์จะทำงาน
     */
    public MonsterController(Monster monster, int[] activeLevelIndices) {
        this(monster, buildActiveLevelMap(0, activeLevelIndices));
    }

    /**
     * คอนสตรัคเตอร์ภายในที่รับแผนที่สถานะสำเร็จรูป
     */
    private MonsterController(Monster monster, boolean[] activeLevels) {
        this.monster = monster;
        this.activeLevels = activeLevels;
    }

    /**
     * เตรียมมอนสเตอร์ให้พร้อมสำหรับด่านปัจจุบัน
     * จะเปิดใช้งานก็ต่อเมื่อด่านนั้นถูกระบุไว้ว่าให้แสดงมอนสเตอร์ตัวนี้
     */
    public void prepareForLevel(int levelIndex, Random random, int panelWidth, int panelHeight) {
        active = levelIndex >= 0 && levelIndex < activeLevels.length && activeLevels[levelIndex];
        if (active) {
            monster.spawn(random, panelWidth, panelHeight);
            if (monster instanceof StunMonster) {
                ((StunMonster) monster).triggerStun();
            }
        }
    }

    public void update(int playerX, int playerY, int panelWidth, int panelHeight) {
        if (!active) {
            return;
        }
        monster.update(playerX, playerY, panelWidth, panelHeight);
    }

    public boolean isActive() {
        return active;
    }

    public Monster getMonster() {
        return monster;
    }
    /**
     * ยูทิลิตี้รวมรายการด่านให้เป็นอาร์เรย์สถานะ พร้อมตรวจสอบความถูกต้องของ index
     */
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
