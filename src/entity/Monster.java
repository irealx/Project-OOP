package entity;

import java.awt.Graphics2D;
import java.util.*;
import system.Config;
import system.Level;
import system.Utils;

/**
 * จัดการมอนสเตอร์ทั้งหมดในเกม รวมระบบการโจมตีและพฤติกรรมไว้ในคลาสเดียว
 */
public class Monster extends Sprite {

    // ประเภทของการโจมตี
    public enum AttackType { STUN, WRAP, SHOOT }

    // อินเทอร์เฟซสำหรับกำหนดพฤติกรรมของมอนสเตอร์แต่ละแบบ
    public interface AttackBehavior {
        void attack(Monster self, Player player, Level level);  // การโจมตีหลัก
        default void render(Graphics2D g, Monster self) {}      // การวาดเอฟเฟกต์เฉพาะตัว
        default void reset(Monster self) {}                     // รีเซ็ตสถานะเมื่อเริ่มเลเวลใหม่
        default void afterUpdate(Monster self) {}               // อัปเดตเพิ่มเติมหลังเคลื่อนไหว
    }

    // เก็บ mapping ระหว่างประเภทการโจมตีกับพฤติกรรมจริง
    private static final Map<AttackType, AttackBehavior> BEHAVIOURS = new EnumMap<>(AttackType.class);

    static {
        BEHAVIOURS.put(AttackType.STUN, new StunAttack());
        BEHAVIOURS.put(AttackType.WRAP, new WrapAttack());
        BEHAVIOURS.put(AttackType.SHOOT, new ShootAttack());
    }

    private final AttackType type;          // ประเภทของมอนสเตอร์
    private AttackBehavior attackBehavior;  // พฤติกรรมของมอนสเตอร์
    private final boolean[] activeLevels;   // เก็บว่าแต่ละเลเวลมอนสเตอร์นี้จะปรากฏหรือไม่
    private boolean active;                 // บอกว่าตอนนี้มอนสเตอร์ถูกเปิดใช้งานไหม

    public Monster(AttackType type) {
        super(Config.MONSTER_SIZE, Config.MONSTER_SPEED[type.ordinal()]);
        this.type = type;
        this.activeLevels = new boolean[Config.TOTAL_LEVELS];
        setAttackBehavior(type);
        setFrame(null, Config.MONSTER_COLOR);
    }

    // กำหนดพฤติกรรมตามประเภทการโจมตี
    private void setAttackBehavior(AttackType type) {
        attackBehavior = BEHAVIOURS.get(type);
    }

    // กำหนดว่าในเลเวลใดบ้างที่มอนสเตอร์นี้จะถูกใช้งาน
    public void setActiveLevels(int... levels) {
        for (int level : levels) {
            if (Utils.withinBounds(level, 0, activeLevels.length - 1)) {
                activeLevels[level] = true;
            }
        }
    }

    // เตรียมมอนสเตอร์ก่อนเริ่มเลเวลใหม่
    public void prepareForLevel(int index, Random random, int width, int height) {
        updateBounds(width, height);
        active = Utils.withinBounds(index, 0, activeLevels.length - 1) && activeLevels[index];
        if (!active) return;

        // สุ่มจุดเกิดตามมุมต่าง ๆ ของฉาก
        int spawnX = random.nextBoolean() ? 16 : width - size - 16;
        int spawnY = random.nextBoolean() ? 16 : height - size - 16;
        setPosition(spawnX, spawnY);

        if (attackBehavior != null) {
            attackBehavior.reset(this);
        }
    }

    // อัปเดตการเคลื่อนไหวและการโจมตีของมอนสเตอร์
    public void update(Player player, Level level) {
        if (!active || player == null || attackBehavior == null) return;
        attackBehavior.attack(this, player, level);
        updateBase(); // การอัปเดตพื้นฐานจาก Sprite (ตำแหน่ง ความเร็ว ฯลฯ)
        attackBehavior.afterUpdate(this);
    }

    // วาดมอนสเตอร์และเอฟเฟกต์เพิ่มเติม
    public void draw(Graphics2D g) {
        if (!active) return;
        drawBase(g);
        attackBehavior.render(g, this);
    }

    public boolean isActive() {
        return active;
    }

    public AttackType getAttackType() {
        return type;
    }

    // อัปเดตมอนสเตอร์ทุกตัวพร้อมกัน
    public static void updateAll(List<Monster> monsters, Player player, Level level) {
        for (Monster monster : monsters) {
            monster.update(player, level);
        }
    }

    // สร้างมอนสเตอร์เริ่มต้นสามประเภทสำหรับแต่ละด่าน
    public static List<Monster> createDefaultMonsters() {
        List<Monster> list = new ArrayList<>();

        Monster stun = new Monster(AttackType.STUN);
        stun.setActiveLevels(0, 3); // มอนสเตอร์สตันจะออกในด่าน 1 และ 4

        Monster wrap = new Monster(AttackType.WRAP);
        wrap.setActiveLevels(1, 4); // มอนสเตอร์วาร์ปออกในด่าน 2 และ 5

        Monster shoot = new Monster(AttackType.SHOOT);
        shoot.setActiveLevels(2, 5); // มอนสเตอร์ยิงออกในด่าน 3 และ 6

        list.add(stun);
        list.add(wrap);
        list.add(shoot);
        return list;
    }
}
