package entity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.*;
import system.Config;
import system.Level;
import system.Utils;

/**
 * จัดการมอนสเตอร์ทั้งหมดในเกม รวมระบบการโจมตี พฤติกรรม และแอนิเมชันไว้ในคลาสเดียว
 */
public class Monster extends Sprite {

    // โหลด sprite sheet ทุกแบบเพียงครั้งเดียว แล้วใช้ร่วมกันทุกตัว
    private static final MonsterAnimator animator = new MonsterAnimator();

    // สำหรับให้ AttackBehavior ใช้เรียก animator ได้
    public static MonsterAnimator gMonsterAnimator() {
        return animator;
    }

    // ประเภทของการโจมตี
    public enum AttackType { STUN, WRAP, SHOOT }

    // พฤติกรรมของมอนสเตอร์แต่ละแบบ (Strategy Pattern)
    public interface AttackBehavior {
        void attack(Monster self, Player player, Level level);   // การโจมตีหลัก
        default void render(Graphics2D g, Monster self) {}       // วาดเอฟเฟกต์เฉพาะตัว (optional)
        default void reset(Monster self) {}                      // รีเซ็ตสถานะเมื่อเริ่มเลเวลใหม่
        default void afterUpdate(Monster self) {}                // ทำงานหลัง update เสร็จ (optional)
    }

    // mapping ประเภทการโจมตี -> พฤติกรรมจริง
    private static final Map<AttackType, AttackBehavior> BEHAVIOURS = new EnumMap<>(AttackType.class);
    static {
        BEHAVIOURS.put(AttackType.STUN, new StunAttack());
        BEHAVIOURS.put(AttackType.WRAP, new WrapAttack());
        BEHAVIOURS.put(AttackType.SHOOT, new ShootAttack());
    }

    private final AttackType type;          // ประเภทของมอนสเตอร์
    private AttackBehavior attackBehavior;  // พฤติกรรมเฉพาะของมอนสเตอร์
    private final boolean[] activeLevels;   // ระบุว่าแต่ละด่านมอนจะโผล่ไหม
    private boolean active;                 // สถานะการเปิดใช้งาน

    // ตัวแปรเกี่ยวกับแอนิเมชัน
    private String currentAnim = "idle";    // แอนิเมชันปัจจุบัน (idle, move, death, skill1, summon)
    private int frameIndex = 0;             // เฟรมปัจจุบัน
    private int frameTimer = 0;             // ตัวนับเวลาเปลี่ยนเฟรม
    private boolean moving = false;         // กำลังเคลื่อนไหวอยู่ไหม

    // สร้างมอนสเตอร์ใหม่
    public Monster(AttackType type) {
        super(Config.MONSTER_SIZE, Config.MONSTER_SPEED[type.ordinal()]);
        this.type = type;
        this.activeLevels = new boolean[Config.TOTAL_LEVELS];
        setAttackBehavior(type);
        setFrame(null, Config.MONSTER_COLOR);
    }

    // กำหนดพฤติกรรมให้ตรงกับประเภทของมอนสเตอร์
    private void setAttackBehavior(AttackType type) {
        attackBehavior = BEHAVIOURS.get(type);
    }

    // กำหนดว่าในเลเวลใดบ้างที่มอนสเตอร์นี้จะถูกเปิดใช้งาน
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

        int spawnX = random.nextBoolean() ? 16 : width - size - 16;
        int spawnY = random.nextBoolean() ? 16 : height - size - 16;
        setPosition(spawnX, spawnY);

        if (attackBehavior != null) attackBehavior.reset(this);
        frameIndex = frameTimer = 0;
        currentAnim = "idle";
    }

    // อัปเดตการเคลื่อนไหวและการโจมตีของมอนสเตอร์
    public void update(Player player, Level level) {
        if (!active || player == null || attackBehavior == null) return;

        int oldX = x, oldY = y;

        attackBehavior.attack(this, player, level);
        updateBase();
        attackBehavior.afterUpdate(this);

        moving = (x != oldX || y != oldY);
        updateAnimation();
    }

    // อัปเดตอนิเมชัน (เปลี่ยนเฟรมทุก 8 ticks)
    private void updateAnimation() {
        BufferedImage[] frames = animator.get(currentAnim);
        if (frames.length == 0) return;

        if (++frameTimer >= 8) {
            frameTimer = 0;
            frameIndex = (frameIndex + 1) % frames.length;
        }
    }

    // วาดมอนสเตอร์และเอฟเฟกต์เพิ่มเติม
    public void draw(Graphics2D g) {
        if (!active) return;

        BufferedImage[] frames = animator.get(currentAnim);
        if (frames.length > 0) {
            int index = Math.min(frameIndex, frames.length - 1);
            g.drawImage(frames[index], x, y, size, size, null);
        } else {
            drawBase(g);
        }

        attackBehavior.render(g, this);
    }

    // เปลี่ยนชื่ออนิเมชันที่กำลังเล่น
    public void setAnimation(String name) {
        this.currentAnim = name;
        this.frameIndex = this.frameTimer = 0;
    }

    public boolean isActive() { return active; }
    public AttackType getAttackType() { return type; }

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
        stun.setActiveLevels(0, 3);

        Monster wrap = new Monster(AttackType.WRAP);
        wrap.setActiveLevels(1, 4);

        Monster shoot = new Monster(AttackType.SHOOT);
        shoot.setActiveLevels(2, 5);

        list.add(stun);
        list.add(wrap);
        list.add(shoot);
        return list;
    }
}
