package entity;

import system.Level;
import system.Utils;

import static system.Config.*;

import java.util.WeakHashMap;

import javax.xml.crypto.Data;

// WrapAttack — มอนสเตอร์วาร์ปอ้อมหลังผู้เล่นก่อนโผล่มาไล่ต่อ
public class WrapAttack extends BaseAttack<WrapAttack.State> {
    private enum Stage { IDLE, CHARGE, WAIT, RECOVER }


    static class State extends BaseAttack.State {
        Stage stage = Stage.IDLE;
        int timer;
    }

    // 🧩 ค่าคงที่เฟรมและเวลา
    private static final int WARP_FRAMES = Math.max(1, Monster.gMonsterAnimator().get("death").length);
    private static final int RECOVER_FRAMES = Math.max(1, Monster.gMonsterAnimator().get("death_reverse").length);
    private static final int WAIT_TICKS = 10 * FRAME_DELAY_MONSTER;

    @Override
    protected State createState() {
        return new State();
    }

    // 🎯 Logic หลัก: ควบคุมสถานะการวาร์ปของมอนสเตอร์
    @Override
    public void attack(Monster self, Player player, Level level) {
        State data = state(self);
        if (player == null) { goIdle(self, data); return; }
        switch (data.stage) {
            case IDLE -> handleIdle(self, player, data);
            case CHARGE -> handleCharge(self, player, data);
            case WAIT -> handleWait(self, data);
            case RECOVER -> handleRecover(self, data);
        }
    }

    @Override
    public void afterUpdate(Monster self) {
        // 🔹 ป้องกันมอนสเตอร์ออกนอกขอบจอหลังวาร์ป
        self.clamp();
    }

    // 🔄 รีเซ็ตสถานะมอนสเตอร์
    @Override
    public void reset(Monster self) {
        State data = state(self);
        goIdle(self, data);
        markCooldownWithDelay(data, WARP_COOLDOWN_MS, 2000);
    }

    // ===== 💤 สถานะ Idle =====
    private void handleIdle(Monster self, Player player, State data) {
        if (data.stage != Stage.IDLE) goIdle(self, data);
        self.follow(player.getX(), player.getY());
        if (!cooldownReady(data, WARP_COOLDOWN_MS)) return;
        if (!withinWarpRange(self, player)) return;
        startCharge(self, data);
        updateTarget(self, player, data, false);
    }

    // ===== 🌀 เริ่มวาร์ป (เล่น death.png เดินหน้า) =====
    private void handleCharge(Monster self, Player player, State data) {
        self.setVelocity(0, 0);
        updateTarget(self, player, data, false);
        if (!advanceAnimation(data, WARP_FRAMES)) {
            self.setAnimationFrame(data.frameIndex);
            return;
        }
        updateTarget(self, player, data, true);
        self.setAnimationFrame(data.frameIndex);
        startWait(data);
    }

    // ===== ⏳ หน่วงช่วงนิ่งก่อนโผล่ =====
    private void handleWait(Monster self, State data) {
        self.setVelocity(0, 0);
        if (++data.timer < WAIT_TICKS) return;
        startRecover(self, data);
    }

    // ===== 🔁 เล่น death_reverse เพื่อโผล่กลับ =====
    private void handleRecover(Monster self, State data) {
        self.setVelocity(0, 0);
        if (!advanceAnimation(data, RECOVER_FRAMES)) {
            self.setAnimationFrame(data.frameIndex);
            return;
        }
        markCooldown(data);
        goIdle(self, data);
    }

    // ===== ฟังก์ชันช่วยจัดการ =====
    private void startCharge(Monster self, State data) {
        data.stage = Stage.CHARGE;
        data.timer = 0;
        resetAnimationState(data);
        self.lockAnimation();
        switchAnimation(self, data, "death");
        self.setAnimationFrame(0);
    }

    //  เปลี่ยนสถานะ + ตั้งอนิเมชัน
    private void startWait(State data) {
        data.stage = Stage.WAIT;
        data.timer = 0;
    }

    // เดินเฟรมตามดีเลย์มอนสเตอร์
    private void startRecover(Monster self, State data) {
        data.stage = Stage.RECOVER;
        data.timer = 0;
        resetAnimationState(data);
        switchAnimation(self, data, "death_reverse");
        self.setAnimationFrame(0);
    }

    //  ตรวจว่าเล่นครบเฟรมหรือยัง
    private void goIdle(Monster self, State data) {
        data.stage = Stage.IDLE;
        data.timer = 0;
        resetAnimationState(data);
        self.unlockAnimation();
        switchAnimation(self, data, "idle");
        self.setAnimationFrame(0);
    }

    //  รีเซ็ตเป็นสถานะ idle
    private boolean withinWarpRange(Monster self, Player player) {
        return self.distanceSquaredTo(player.getCenterX(), player.getCenterY()) <= WARP_RANGE * WARP_RANGE;
    }

    //  ดึงหรือสร้างข้อมูลสถานะใหม่
    private void updateTarget(Monster self, Player player, State data, boolean teleportNow) {
        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        double len = Math.hypot(dx, dy);
        double nx = len < 1e-3 ? 1 : dx / len;
        double ny = len < 1e-3 ? 0 : dy / len;
        int distance = player.getSize() + self.getSize() + SAFE_OFFSET;
        int newX = Utils.clamp(player.getCenterX() - (int) (nx * distance) - self.getSize() / 2, 0, self.panelWidth - self.getSize());
        int newY = Utils.clamp(player.getCenterY() - (int) (ny * distance) - self.getSize() / 2, 0, self.panelHeight - self.getSize());
        if (teleportNow) self.setPosition(newX, newY);
    }
}
