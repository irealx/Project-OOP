package entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.WeakHashMap;
import system.EffectRenderer;
import system.Level;
import system.Utils;

/**
 * WrapAttack — พฤติกรรม "วาร์ป" ของมอนสเตอร์ใน Six Door Maze
 * ใช้ death.png (20 เฟรม) เพื่อทำแอนิเมชันวาร์ป → ย้อนแอนิเมชันกลับและออกไล่ผู้เล่นต่อ
 */
public class WrapAttack implements Monster.AttackBehavior {

    // ===== 🧩 ค่าคงที่ =====
    private static final long WARP_COOLDOWN_MS = 5000L;     // เวลาพักระหว่างวาร์ปรอบใหม่
    private static final int FRAME_DELAY = 3;                // ความหน่วงเฟรมตรงกับระบบ Monster
    private static final int WARP_RANGE = 320;               // ระยะเริ่มวาร์ป
    private static final int SAFE_OFFSET = 12;               // ระยะห่างจากผู้เล่นตอนโผล่
    private static final int WARP_FRAMES =
            Math.max(1, Monster.gMonsterAnimator().get("death").length); // จำนวนเฟรม death.png เต็ม 20 เฟรม

    // ===== 🧭 สถานะการวาร์ป =====
    private enum State { IDLE, WARP_CHARGE, WARP_RECOVER }

    // เก็บสถานะแยกสำหรับมอนแต่ละตัว (ใช้ WeakHashMap เพื่อ auto clear)
    private static class Data {
        State state = State.IDLE;
        boolean animationFinished, hasTarget;
        long lastWarpTime = System.currentTimeMillis();
        String currentAnim = "";
        int frameIndex, frameTimer;
        int targetX, targetY, targetCenterX, targetCenterY;
    }

    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    // ===== 🎯 Logic หลัก =====
    @Override
    public void attack(Monster self, Player player, Level level) {
        Data data = state(self);
        if (player == null) {
            idle(self, data);
            return;
        }

        switch (data.state) {
            case IDLE -> handleIdle(self, player, data);
            case WARP_CHARGE -> handleWarpCharge(self, player, data);
            case WARP_RECOVER -> handleWarpRecover(self, data);
        }
    }

    @Override
    public void afterUpdate(Monster self) {
        // 🔹 ป้องกันไม่ให้มอนออกนอกขอบจอหลังวาร์ป
        self.clamp();
    }

    // ===== 🎨 เอฟเฟกต์วงก่อนวาร์ป =====
    @Override
    public void render(Graphics2D g, Monster self) {
        Data data = states.get(self);
        if (data == null || !data.hasTarget || data.state != State.WARP_CHARGE) return;

        float frameProgress = (data.frameIndex + data.frameTimer / (float) FRAME_DELAY) /
                Math.max(1f, WARP_FRAMES);
        frameProgress = Utils.clamp(frameProgress, 0f, 1f);

        int baseRadius = Math.max(self.getSize(), self.getSize() + SAFE_OFFSET * 2);
        int radius = (int) (baseRadius * (0.6f + 0.4f * frameProgress));

        var oldStroke = g.getStroke();
        var oldComposite = g.getComposite();

        EffectRenderer.setAlpha(g, 0.65f);
        g.setColor(new Color(120, 255, 200));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int diameter = radius * 2;
        g.drawOval(data.targetCenterX - radius, data.targetCenterY - radius, diameter, diameter);

        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    @Override
    public void reset(Monster self) {
        Data data = state(self);
        data.state = State.IDLE;
        int frameIndex, frameTimer;
        data.animationFinished = false;
        data.lastWarpTime = System.currentTimeMillis();
        data.hasTarget = false;
        self.unlockAnimation();
        switchAnimation(self, data, "idle");
    }

    // ===== 💤 สถานะ Idle =====
    private void handleIdle(Monster self, Player player, Data data) {
        self.unlockAnimation();
        switchAnimation(self, data, "idle");
        self.follow(player.getX(), player.getY()); // เดินตามผู้เล่นปกติ

        long now = System.currentTimeMillis();
        if (now - data.lastWarpTime < WARP_COOLDOWN_MS) return; // ยังไม่ถึงคูลดาวน์

        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        if (dx * dx + dy * dy > WARP_RANGE * WARP_RANGE) return; // ผู้เล่นอยู่ไกลเกิน

        // 🔹 เริ่มเตรียมวาร์ป
        enterState(self, data, State.WARP_CHARGE, "death");
        if (player != null) {
            lockWarpTarget(self, player, data);
        }
    }

    // ===== 🌀 เริ่มวาร์ป (death.png เดินหน้า) =====
    private void handleWarpCharge(Monster self, Player player, Data data) {
        self.setVelocity(0, 0); // 🔸 ล็อกมอนให้นิ่ง

        if (!data.hasTarget && player != null) {
            lockWarpTarget(self, player, data);
        }

        // 🔹 เล่น death.png ครบ 20 เฟรมก่อนจะวาร์ป
        if (advanceAnimation(self, data, WARP_FRAMES)) {
            teleportBehind(self, player, data);
            data.hasTarget = false;
            
            // ✅ เมื่อจบให้เล่น death_reverse ย้อนกลับ 20 เฟรม
            enterState(self, data, State.WARP_RECOVER, "death_reverse");
        }
    }

    // ===== 🔁 เล่น death.png แบบย้อนกลับ =====
    private void handleWarpRecover(Monster self, Data data) {
        self.setVelocity(0, 0);
        int reverseFrames = Math.max(1, Monster.gMonsterAnimator().get("death_reverse").length);
        if (advanceAnimation(self, data, reverseFrames)) {
            data.lastWarpTime = System.currentTimeMillis();
            enterState(self, data, State.IDLE, "idle");
        }
    }

    // ===== 🧠 ฟังก์ชันช่วยจัดการสถานะ =====
    private void idle(Monster self, Data data) {
        self.unlockAnimation();
        switchAnimation(self, data, "idle");
        data.state = State.IDLE;
    }

    private Data state(Monster self) {
        return states.computeIfAbsent(self, s -> new Data());
    }

    private void enterState(Monster self, Data data, State next, String anim) {
        data.state = next;
        resetAnim(data);
        switchAnimation(self, data, anim);
        if (next == State.IDLE) {
            self.unlockAnimation();
        } else {
            self.lockAnimation();
            self.setAnimationFrame(data.frameIndex);
        }
    }

    private void resetAnim(Data data) {
        data.frameIndex = 0;
        data.frameTimer = 0;
        data.animationFinished = false;
        data.hasTarget = false;
    }

    private void switchAnimation(Monster self, Data data, String anim) {
        if (!anim.equals(data.currentAnim)) {
            self.setAnimation(anim);
            data.currentAnim = anim;
        }
    }

    private boolean advanceAnimation(Monster self, Data data, int totalFrames) {
        if (data.animationFinished || totalFrames <= 0) {
            return true;
        }

        if (++data.frameTimer >= FRAME_DELAY) {
            data.frameTimer = 0;

            data.frameIndex++;
            if (data.frameIndex >= totalFrames) {
                data.frameIndex = totalFrames - 1;
                data.animationFinished = true;
            }
            self.setAnimationFrame(data.frameIndex);
        }

        return data.animationFinished;
    }

    // ===== 📍 คำนวณตำแหน่งวาร์ป =====
    private void lockWarpTarget(Monster self, Player player, Data data) {
        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        double len = Math.hypot(dx, dy);
        double vx = dx, vy = dy;
        if (len < 1e-3) { vx = 1; vy = 0; len = 1; }

        double nx = vx / len, ny = vy / len;
        int dist = player.getSize() + self.getSize() + SAFE_OFFSET;

        int newX = Utils.clamp(player.getCenterX() - (int) (nx * dist) - self.getSize() / 2, 0, self.panelWidth - self.getSize());
        int newY = Utils.clamp(player.getCenterY() - (int) (ny * dist) - self.getSize() / 2, 0, self.panelHeight - self.getSize());

        data.targetX = newX;
        data.targetY = newY;
        data.targetCenterX = newX + self.getSize() / 2;
        data.targetCenterY = newY + self.getSize() / 2;
        data.hasTarget = true;
    }

    private void teleportBehind(Monster self, Player player, Data data) {
        if (!data.hasTarget && player != null) {
            lockWarpTarget(self, player, data);
        }
        self.setPosition(data.targetX, data.targetY);
    }
}
