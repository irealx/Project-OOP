package entity;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import system.Config;
import system.Level;
import system.Utils;

// REFACTOR: รวมการจัดการมอนสเตอร์ไว้ในคลาสเดียว ลดความซ้ำของโค้ดควบคุม
public class Monster extends Sprite {
    public enum AttackType { STUN, WRAP, SHOOT }
    public interface AttackBehavior {
        void attack(Monster self, Player player, Level level);
        default void render(Graphics2D g, Monster self) {}
        default void reset(Monster self) {}
        default void afterUpdate(Monster self) {}
    }
    private static final Map<AttackType, AttackBehavior> BEHAVIOURS = new EnumMap<>(AttackType.class);

    static {
        BEHAVIOURS.put(AttackType.STUN, new StunAttack());
        BEHAVIOURS.put(AttackType.WRAP, new WrapAttack());
        BEHAVIOURS.put(AttackType.SHOOT, new ShootAttack());
    }
    private final AttackType type;
    private AttackBehavior attackBehavior;
    private final boolean[] activeLevels;
    private boolean active;
    public Monster(AttackType type) {
        super(Config.MONSTER_SIZE, Config.MONSTER_SPEED[type.ordinal()]);
        this.type = type;
        this.activeLevels = new boolean[Config.TOTAL_LEVELS];
        setAttackBehavior(type);
        setFrame(null, Config.MONSTER_COLOR);
    }
    private void setAttackBehavior(AttackType type) {
        attackBehavior = BEHAVIOURS.get(type);
    }
    public void setActiveLevels(int... levels) {
        for (int level : levels) {
            if (Utils.withinBounds(level, 0, activeLevels.length - 1)) {
                activeLevels[level] = true;
            }
        }
    }
    public void prepareForLevel(int index, Random random, int width, int height) {
        updateBounds(width, height);
        active = Utils.withinBounds(index, 0, activeLevels.length - 1) && activeLevels[index];
        if (!active) {
            return;
        }
        int spawnX = random.nextBoolean() ? 16 : width - size - 16;
        int spawnY = random.nextBoolean() ? 16 : height - size - 16;
        setPosition(spawnX, spawnY);
        if (attackBehavior != null) {
            attackBehavior.reset(this);
        }
    }
    public void update(Player player, Level level) {
        if (!active || player == null || attackBehavior == null) {
            return;
        }
        attackBehavior.attack(this, player, level);
        updateBase();
        attackBehavior.afterUpdate(this);
    }
    public void draw(Graphics2D g) {
        if (!active) {
            return;
        }
        drawBase(g);
        attackBehavior.render(g, this);
    }
    public boolean isActive() {
        return active;
    }

    public AttackType getAttackType() {
        return type;
    }
    public static void updateAll(List<Monster> monsters, Player player, Level level) {
        for (Monster monster : monsters) {
            monster.update(player, level);
        }
    }
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