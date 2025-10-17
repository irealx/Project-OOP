package system;

import java.awt.Color;
import java.util.List;

/**
 * 🔧 Config.java — รวมค่าคงที่ทั้งหมดของเกม Six Door Maze
 * เพื่อให้ง่ายต่อการปรับสมดุลเกม ปรับความเร็ว หรือธีมภาพ/สี ได้ในที่เดียว
 */
public final class Config {
    private Config() {} // ป้องกันไม่ให้มีการสร้าง instance


    // ขนาดและระบบหลักของเกม
    public static final int PANEL_WIDTH = 800;      // ความกว้างหน้าจอเกม
    public static final int PANEL_HEIGHT = 600;     // ความสูงหน้าจอเกม
    public static final int DOOR_SIZE = 48;         // ขนาดของประตู
    public static final int PLAYER_SIZE = 24;       // ขนาดของผู้เล่น
    public static final double PLAYER_SPRITE_SCALE = 4; // scale sprite ผู้เล่น (ถ้ามีหลายขนาด)


    // การเคลื่อนไหวและแสงของผู้เล่น
    public static final int PLAYER_SPEED = 8;          // ความเร็วผู้เล่น
    public static final int PLAYER_LIGHT_RADIUS = 160; // รัศมีแสงรอบตัวผู้เล่น (flashlight effect)

    // มอนสเตอร์และพฤติกรรมทั่วไป
    public static final int[] MONSTER_SPEED = { 2, 3, 2 };   // ความเร็วของมอนแต่ละประเภท
    public static final int[] MONSTER_LIGHT = { 120, 120, 140 }; // รัศมีแสงรอบมอนแต่ละประเภท
    public static final int MONSTER_SIZE = 32;                // ขนาดของมอนสเตอร์
    public static final int FRAME_DELAY_MONSTER = 3;          // ความหน่วงระหว่างแต่ละเฟรมอนิเมชันมอน
    public static final int MONSTER_INITIAL_DELAY_MS = 500;   // ดีเลย์ก่อนเริ่มโจมตีครั้งแรกเมื่อเริ่มด่าน


    // การตั้งค่าด่าน / ระบบประตู
    public static final int TOTAL_LEVELS = 6;        // จำนวนด่านทั้งหมด
    public static final int DOOR_PER_LEVEL = 6;      // จำนวนประตูต่อด่าน
    public static final int WRAP_MARGIN = 8;         // ระยะขอบป้องกันมอนวาร์ปนอกฉาก


    // พฤติกรรมของมอนแต่ละประเภท
    // ----- 🌀 Warp Monster -----
    public static final long WARP_COOLDOWN_MS = 180;  // คูลดาวน์ระหว่างการวาร์ป
    public static final int WARP_RANGE = 320;          // ระยะที่เริ่มวาร์ปได้
    public static final int SAFE_OFFSET = 12;          // ระยะปลอดภัยตอนโผล่หลังผู้เล่น

    // ----- 💫 Stun Monster -----
    public static final int STUN_DURATION = 60;        // ระยะเวลาที่ศัตรูถูกสตัน (เฟรม)
    public static final int STUN_COOLDOWN = 180;       // เวลาคูลดาวน์ก่อนใช้สตันใหม่
    public static final int STUN_RING_RADIUS = 480;    // รัศมีวงสตัน
    public static final int STUN_RING_THICKNESS = 10;  // ความหนาของวงสตัน

    // ----- 🔫 Shooting Monster -----
    public static final int SHOOT_DASH_MULTIPLIER = 3; // ความเร็วที่พุ่งหลังยิง
    public static final int SHOOT_DASH_INTERVAL = 45;  // ระยะเวลาพักระหว่างการพุ่ง

    // ระบบกระสุน (Projectile)
    public static final double PROJECTILE_SPEED = 6.0;       // ความเร็วกระสุน
    public static final double PROJECTILE_RANGE = 640.0;     // ระยะทางสูงสุดที่กระสุนวิ่งได้
    public static final int PROJECTILE_FRAME_DELAY = 5;      // ดีเลย์ระหว่างการเปลี่ยนเฟรมของ sprite กระสุน
    public static final int PROJECTILE_DRAW_SIZE = 12;       // ขนาดจริงของกระสุนบนจอ (pixel)
    public static final int SHOOT_COOLDOWN_TICKS = 90;       // เวลาพักก่อนมอนยิงกระสุนใหม่ (เฟรม)

    // 🎨 สีและธีมหลักของเกม
    public static final Color BACKGROUND_COLOR = Color.BLACK;            // พื้นหลัง
    public static final Color PLAYER_FALLBACK = new Color(0xFFD700);     // สีสำรองของผู้เล่น
    public static final Color MONSTER_COLOR = new Color(0xEE82EE);       // สีสำรองของมอนสเตอร์
    public static final Color DOOR_GLOW = new Color(0x66CCFF);           // สีเรืองแสงของประตู

    // ระบบพัซเซิล (Puzzle)
    public static final List<Integer> PUZZLE_POOL = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
    public static final int PUZZLE_SHOW_ALPHA = 200;  // ความโปร่งแสงของภาพพัซเซิล (0-255)

    // ระบบเกมโดยรวม
    public static final int TIMER_DELAY_MS = 16; // ความถี่ในการอัปเดตเกม (ประมาณ 60 FPS)
}
