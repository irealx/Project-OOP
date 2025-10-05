package system;

import java.awt.Color;
import java.util.List;

// REFACTOR: รวมค่าคงที่ไว้ที่เดียวเพื่อแก้ไขง่ายและป้องกันค่าซ้ำซ้อนในหลายไฟล์
public final class Config {
    private Config() {}

    // REFACTOR: กำหนดขนาดหลักของเกมให้ใช้ร่วมกันทั้งระบบ
    public static final int PANEL_WIDTH = 800;
    public static final int PANEL_HEIGHT = 600;
    public static final int DOOR_SIZE = 48;
    public static final int PLAYER_SIZE = 24;

    // REFACTOR: ความเร็วและคูลดาวน์ของวัตถุถูกเก็บในที่เดียวเพื่อลดตัวเลขกระจัดกระจาย
    public static final int PLAYER_SPEED = 4;
    public static final int PLAYER_LIGHT_RADIUS = 160;
    public static final int[] MONSTER_SPEED = { 2, 3, 2 };
    public static final int[] MONSTER_LIGHT = { 120, 120, 140 };
    public static final int MONSTER_SIZE = 32;
    public static final int TOTAL_LEVELS = 6;
    public static final int DOOR_PER_LEVEL = 6;

    // REFACTOR: ค่าพิเศษของกลยุทธ์โจมตีแต่ละแบบรวมไว้เพื่อแก้ไขง่าย
    public static final int STUN_DURATION = 60;
    public static final int STUN_COOLDOWN = 180;
    public static final int STUN_RING_RADIUS = 480;
    public static final int STUN_RING_THICKNESS = 10;
    public static final int WRAP_MARGIN = 8;
    public static final int SHOOT_DASH_MULTIPLIER = 3;
    public static final int SHOOT_DASH_INTERVAL = 45;

    // REFACTOR: กำหนดสีหลักของเกมไว้ตรงกลางเพื่อปรับธีมได้ง่าย
    public static final Color BACKGROUND_COLOR = Color.BLACK;
    public static final Color PLAYER_FALLBACK = new Color(0xFFD700);
    public static final Color MONSTER_COLOR = new Color(0xEE82EE);
    public static final Color DOOR_GLOW = new Color(0x66CCFF);

    // REFACTOR: กำหนดค่าพัซเซิลให้เปลี่ยนพร้อมกันทุกด่าน
    public static final List<Integer> PUZZLE_POOL = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
    public static final int PUZZLE_SHOW_ALPHA = 200;
    public static final int TIMER_DELAY_MS = 16;
}