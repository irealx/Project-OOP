package entity;

import java.awt.Graphics2D;
import java.util.WeakHashMap;
import system.Level;

/**
 * WrapAttack — พฤติกรรม "วาร์ป" ของมอนสเตอร์ใน Six Door Maze (ภาษาไทย)
 * คอยจับจังหวะเพื่อวาร์ปหายไปแล้วโผล่ด้านหลังผู้เล่น พร้อมใช้แอนิเมชัน death.png
 */
public class WrapAttack implements Monster.AttackBehavior {

    // ===== ค่าคงที่หลัก =====
    private static final long WARP_COOLDOWN_MS = 5000L;          // 5 วิพักก่อนวาร์ปรอบใหม่
    private static final int FRAME_DELAY = 8;                     // ให้เฟรมตรงกับระบบ Monster
    private static final int WARP_RANGE = 320;                    // ระยะตรวจจับก่อนเริ่มวาร์ป
    private static final int SAFE_OFFSET = 12;                    // ระยะห่างจากผู้เล่นตอนโผล่
    private static final int WARP_FRAMES = Math.max(1,
            Monster.gMonsterAnimator().get("death").length);     // จำนวนเฟรม death.png

    // ===== สถานะของการวาร์ป =====
    private enum State { IDLE, WARP_START, WARP_END }

    private static class Data {
        State state = State.IDLE;
        int frameIndex;
        int frameTimer;
        boolean animationFinished;
        long lastWarpTime = System.currentTimeMillis();
        double dirX;
        double dirY;
        String currentAnim = "";
    }

    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    @Override
    public void attack(Monster self, Player player, Level level) {
        Data data = state(self);
        if (player == null) {
            // 🔹 ถ้าไม่มีผู้เล่นให้หยุดนิ่งและรอ
            switchAnimation(self, data, "idle");
            data.state = State.IDLE;
            return;
        }

        switch (data.state) {
            case IDLE -> handleIdle(self, player, data);
            case WARP_START -> handleWarpStart(self, player, data);
            case WARP_END -> handleWarpEnd(self, data);
        }
    }

    @Override
    public void afterUpdate(Monster self) {
        // 🔹 ป้องกันตำแหน่งหลังวาร์ปให้อยู่ในกรอบจอ
        self.clamp();
    }

    @Override
    public void render(Graphics2D g, Monster self) {
        // 🔹 ไม่ต้องวาดเอฟเฟกต์เพิ่ม เพราะตัวมอนสเตอร์ใช้ death.png อยู่แล้ว
    }

    @Override
    public void reset(Monster self) {
        Data data = state(self);
        data.state = State.IDLE;
        data.frameIndex = data.frameTimer = 0;
        data.animationFinished = false;
        data.lastWarpTime = System.currentTimeMillis();
        switchAnimation(self, data, "idle");
    }

    // ===== จัดการสถานะ IDLE =====
    private void handleIdle(Monster self, Player player, Data data) {
        switchAnimation(self, data, "idle");

        // 🔹 ให้มอนสเตอร์เดินตามผู้เล่นแบบปกติ
        self.follow(player.getX(), player.getY());

        // 🔹 ตรวจคูลดาวน์และระยะ ถ้าพร้อมให้เริ่มวาร์ป
        long now = System.currentTimeMillis();
        if (now - data.lastWarpTime < WARP_COOLDOWN_MS) return;

        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        if (dx * dx + dy * dy > WARP_RANGE * WARP_RANGE) return;

        data.dirX = dx;
        data.dirY = dy;
        enterState(self, data, State.WARP_START, "death");
    }

    // ===== เริ่มเล่น death.png แบบเดินหน้า =====
    private void handleWarpStart(Monster self, Player player, Data data) {
        self.setVelocity(0, 0); // 🔸 ล็อกตำแหน่งระหว่างวาร์ป

        if (advanceAnimation(data, WARP_FRAMES)) {
            // 🔹 เมื่อแอนิเมชันจบ → ย้ายไปด้านหลังผู้เล่น
            teleportBehind(self, player, data);
            enterState(self, data, State.WARP_END, "death_reverse");
        }
    }

    // ===== เล่น death.png แบบย้อนกลับ =====
    private void handleWarpEnd(Monster self, Data data) {
        self.setVelocity(0, 0);

        if (advanceAnimation(data, WARP_FRAMES)) {
            data.lastWarpTime = System.currentTimeMillis();
            enterState(self, data, State.IDLE, "idle");
        }
    }

    // ===== ฟังก์ชันช่วยเหลือ =====
    private Data state(Monster self) {
        return states.computeIfAbsent(self, s -> new Data());
    }

    private void enterState(Monster self, Data data, State next, String animation) {
        data.state = next;
        data.frameIndex = 0;
        data.frameTimer = 0;
        data.animationFinished = false;
        switchAnimation(self, data, animation);
    }

    private void switchAnimation(Monster self, Data data, String animation) {
        if (!animation.equals(data.currentAnim)) {
            self.setAnimation(animation);
            data.currentAnim = animation;
        }
    }

    private boolean advanceAnimation(Data data, int totalFrames) {
        if (data.animationFinished) return true;

        if (++data.frameTimer >= FRAME_DELAY) {
            data.frameTimer = 0;
            data.frameIndex++;
            if (data.frameIndex >= totalFrames) {
                data.frameIndex = totalFrames - 1;
                data.animationFinished = true;
            }
        }
        return data.animationFinished;
    }

    private void teleportBehind(Monster self, Player player, Data data) {
        double vx = data.dirX;
        double vy = data.dirY;
        double length = Math.hypot(vx, vy);

        if (length < 1e-3) {
            // 🔸 ถ้าอยู่ตำแหน่งเดียวกัน ให้สุ่มทิศหนีเล็กน้อย
            vx = 1;
            vy = 0;
            length = 1;
        }

        double nx = vx / length;
        double ny = vy / length;

        int distance = player.getSize() + self.getSize() + SAFE_OFFSET;
        int centerX = player.getCenterX() - (int) Math.round(nx * distance);
        int centerY = player.getCenterY() - (int) Math.round(ny * distance);

        int newX = centerX - self.getSize() / 2;
        int newY = centerY - self.getSize() / 2;
        self.setPosition(newX, newY);
    }
}
