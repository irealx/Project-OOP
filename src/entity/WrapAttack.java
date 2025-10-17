package entity;

import java.util.WeakHashMap;
import system.Level;
import system.Utils;

import static system.Config.*;

/**
 * ===== 🧩 WrapAttack — มอนสเตอร์วาร์ปครบ 20 เฟรมพร้อมหน่วงพัก =====
 * 
 * ทำงานโดย:
 * 1. ตรวจจับผู้เล่นในระยะ WARP_RANGE
 * 2. เล่น death.png 20 เฟรม (หายตัว)
 * 3. รอ 10 เฟรม (นิ่ง)
 * 4. เล่น death_reverse 20 เฟรม (โผล่กลับ)
 * 5. เดินตามผู้เล่นต่อ
 */
public class WrapAttack implements Monster.AttackBehavior {

    // 🧭 สถานะการวาร์ป
    private enum State { IDLE, CHARGE, WAIT, RECOVER }

    // 🧠 ข้อมูลสถานะของมอนสเตอร์แต่ละตัว (ใช้ record ย่อโค้ดให้กระชับ)
    private record Data(State state, int frame, int timer, long lastWarp,
                        boolean hasTarget, String anim,
                        int targetX, int targetY, int targetCenterX, int targetCenterY) {

        static Data fresh() {
            long now = System.currentTimeMillis();
            return new Data(State.IDLE, 0, 0, now, false, "idle", 0, 0, 0, 0);
        }

        Data withFrameTimer(int frame, int timer) {
            return new Data(state, frame, timer, lastWarp, hasTarget, anim, targetX, targetY, targetCenterX, targetCenterY);
        }

        Data withTimer(int timer) {
            return new Data(state, frame, timer, lastWarp, hasTarget, anim, targetX, targetY, targetCenterX, targetCenterY);
        }

        Data withLast(long last) {
            return new Data(state, frame, timer, last, hasTarget, anim, targetX, targetY, targetCenterX, targetCenterY);
        }

        Data withTarget(boolean hasTarget, int x, int y, int cx, int cy) {
            return new Data(state, frame, timer, lastWarp, hasTarget, anim, x, y, cx, cy);
        }
    }

    // 🧩 ค่าคงที่เฟรมและเวลา
    private static final int WARP_FRAMES = Math.max(1, Monster.gMonsterAnimator().get("death").length);
    private static final int RECOVER_FRAMES = Math.max(1, Monster.gMonsterAnimator().get("death_reverse").length);
    private static final int WAIT_TICKS = 10 * FRAME_DELAY_MONSTER;

    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    // 🎯 Logic หลัก: ควบคุมสถานะการวาร์ปของมอนสเตอร์
    @Override
    public void attack(Monster self, Player player, Level level) {
        Data data = data(self);
        if (player == null) {
            states.put(self, toIdle(self, data));
            return;
        }

        Data next = switch (data.state()) {
            case IDLE -> handleIdle(self, player, data);
            case CHARGE -> handleCharge(self, player, data);
            case WAIT -> handleWait(self, data);
            case RECOVER -> handleRecover(self, data);
        };
        states.put(self, next);
    }

    @Override
    public void afterUpdate(Monster self) {
        // 🔹 ป้องกันมอนสเตอร์ออกนอกขอบจอหลังวาร์ป
        self.clamp();
    }

    // 🔄 รีเซ็ตสถานะมอนสเตอร์
    @Override
    public void reset(Monster self) {
        self.unlockAnimation();
        self.setAnimation("idle");
        states.put(self, Data.fresh());
    }

    // ===== 💤 สถานะ Idle =====
    private Data handleIdle(Monster self, Player player, Data data) {
        Data next = toIdle(self, data);
        self.follow(player.getX(), player.getY()); // เดินตามผู้เล่น

        long now = System.currentTimeMillis();
        if (now - next.lastWarp < WARP_COOLDOWN_MS) return next;

        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        if (dx * dx + dy * dy > WARP_RANGE * WARP_RANGE) return next;

        // 🔹 เริ่มวาร์ป
        next = changeState(self, next, State.CHARGE, "death", true, true);
        return warpToBehindPlayer(self, player, next, false);
    }

    // ===== 🌀 เริ่มวาร์ป (เล่น death.png เดินหน้า) =====
    private Data handleCharge(Monster self, Player player, Data data) {
        self.setVelocity(0, 0);
        Data next = warpToBehindPlayer(self, player, data, false);
        next = updateAnim(self, next, WARP_FRAMES);

        if (isFinished(next, WARP_FRAMES)) {
            next = warpToBehindPlayer(self, player, next, true);
            return changeState(self, next, State.WAIT, "death", true, false);
        }
        return next;
    }

    // ===== ⏳ หน่วงช่วงนิ่งก่อนโผล่ =====
    private Data handleWait(Monster self, Data data) {
        self.setVelocity(0, 0);
        int timer = data.timer + 1;
        if (timer >= WAIT_TICKS) {
            return changeState(self, data, State.RECOVER, "death_reverse", true, true);
        }
        return data.withTimer(timer);
    }

    // ===== 🔁 เล่น death_reverse เพื่อโผล่กลับ =====
    private Data handleRecover(Monster self, Data data) {
        self.setVelocity(0, 0);
        Data next = updateAnim(self, data, RECOVER_FRAMES);
        if (isFinished(next, RECOVER_FRAMES)) {
            next = next.withLast(System.currentTimeMillis());
            return changeState(self, next, State.IDLE, "idle", false, true);
        }
        return next;
    }

    // ===== 🧠 ฟังก์ชันช่วยจัดการ =====

    // 🔸 คำนวณตำแหน่งหลังผู้เล่น และย้ายมอนสเตอร์ไปที่นั่นถ้า teleportNow=true
    private Data warpToBehindPlayer(Monster self, Player player, Data data, boolean teleportNow) {
        if (player == null) return data;

        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        double len = Math.hypot(dx, dy);
        double nx = len < 1e-3 ? 1 : dx / len;
        double ny = len < 1e-3 ? 0 : dy / len;

        int distance = player.getSize() + self.getSize() + SAFE_OFFSET;
        int newX = Utils.clamp(player.getCenterX() - (int) (nx * distance) - self.getSize() / 2,
                0, self.panelWidth - self.getSize());
        int newY = Utils.clamp(player.getCenterY() - (int) (ny * distance) - self.getSize() / 2,
                0, self.panelHeight - self.getSize());

        int centerX = newX + self.getSize() / 2;
        int centerY = newY + self.getSize() / 2;

        if (teleportNow) self.setPosition(newX, newY);
        return data.withTarget(true, newX, newY, centerX, centerY);
    }

    // 🔧 เปลี่ยนสถานะ + ตั้งอนิเมชัน
    private Data changeState(Monster self, Data data, State nextState, String anim, boolean lock, boolean resetFrame) {
        if (!anim.equals(data.anim)) self.setAnimation(anim);
        if (lock) self.lockAnimation(); else self.unlockAnimation();
        int frame = resetFrame ? 0 : data.frame;
        self.setAnimationFrame(frame);
        return new Data(nextState, frame, 0, data.lastWarp, false, anim,
                data.targetX, data.targetY, data.targetCenterX, data.targetCenterY);
    }

    // 🎞 เดินเฟรมตามดีเลย์มอนสเตอร์
    private Data updateAnim(Monster self, Data data, int totalFrames) {
        if (totalFrames <= 0) return data;
        int frame = data.frame;
        int timer = data.timer + 1;
        if (timer >= FRAME_DELAY_MONSTER) {
            timer = 0;
            frame = Math.min(frame + 1, totalFrames - 1);
            self.setAnimationFrame(frame);
        }
        return data.withFrameTimer(frame, timer);
    }

    // ✅ ตรวจว่าเล่นครบเฟรมหรือยัง
    private boolean isFinished(Data data, int totalFrames) {
        return totalFrames <= 0 || (data.frame >= totalFrames - 1 && data.timer == 0);
    }

    // 🌿 รีเซ็ตเป็นสถานะ idle
    private Data toIdle(Monster self, Data data) {
        self.unlockAnimation();
        if (!"idle".equals(data.anim) || data.state != State.IDLE) {
            self.setAnimation("idle");
            self.setAnimationFrame(0);
            return new Data(State.IDLE, 0, 0, data.lastWarp, false, "idle",
                    data.targetX, data.targetY, data.targetCenterX, data.targetCenterY);
        }
        return data;
    }

    // 📦 ดึงหรือสร้างข้อมูลสถานะใหม่
    private Data data(Monster self) {
        return states.computeIfAbsent(self, s -> Data.fresh());
    }
}
