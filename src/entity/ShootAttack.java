package entity;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import system.Level;

// ShootAttack — มอนสเตอร์ยิงพลังงานกระจายสามทิศพร้อมเอฟเฟกต์กระสุน
public class ShootAttack extends BaseAttack<ShootAttack.State> {
    private static final int SUMMON_FRAMES = Math.max(1, Monster.gMonsterAnimator().get("summon").length);
    private static final long COOLDOWN_MS = SHOOT_COOLDOWN_TICKS * TIMER_DELAY_MS;
    
    static class State extends BaseAttack.State{
        boolean attacking;
        boolean fired;
        final List<Projectile> projectiles = new ArrayList<>();
    }

    @Override
    public void attack(Monster self, Player player, Level level) {
        Data data = state(self);

        // อัปเดตกระสุนก่อนเสมอ
        updateProjectiles(data, player, self);

        if (player == null) {
            switchAnimation(self, data, "idle");
            data.attacking = false;
            return;
        }

        if (data.attacking) {
            // ระหว่างร่ายให้ยืนกับที่
            self.setVelocity(0, 0);
            handleCasting(self, player, data);
            return;
        }

        switchAnimation(self, data, "idle");

        // เคลื่อนเข้าหาผู้เล่น
        self.follow(player.getX(), player.getY());

        long now = System.currentTimeMillis();
        if (now - data.lastShotTime < SHOOT_COOLDOWN_TICKS * TIMER_DELAY_MS) return;

        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        if (dx * dx + dy * dy > WARP_RANGE * WARP_RANGE) return;

        // เริ่มเล่นแอนิเมชัน summon.png
        data.attacking = true;
        data.fired = false;
        data.frameIndex = data.frameTimer = 0;
        data.animationFinished = false;
        switchAnimation(self, data, "summon");
    }

    @Override
    public void afterUpdate(Monster self) {
        // บังคับไม่ให้ออกนอกกรอบจอ
        self.clamp();
    }

    @Override
    public void render(Graphics2D g, Monster self) {
        // วาดกระสุนทั้งหมด
        for (Projectile projectile : state(self).projectiles) {
            projectile.draw(g);
        }
    }

    @Override
    public void reset(Monster self) {
        Data data = state(self);
        data.attacking = false;
        data.fired = false;
        data.frameIndex = data.frameTimer = 0;
        data.animationFinished = false;
        data.projectiles.clear();
        data.lastShotTime = System.currentTimeMillis();
        switchAnimation(self, data, "idle");
    }

    // ===== ระหว่างกำลังร่าย summon =====
    private void handleCasting(Monster self, Player player, Data data) {
        if (!data.fired && data.frameIndex >= SUMMON_FRAMES - 1) {
            // ยิงกระสุน 3 ทิศเมื่อถึงเฟรมสุดท้าย
            fireProjectiles(self, player, data);
            data.fired = true;
            data.lastShotTime = System.currentTimeMillis();
        }

        if (advanceAnimation(data, SUMMON_FRAMES)) {
            // เมื่อเล่นครบ → กลับไป idle
            data.attacking = false;
            switchAnimation(self, data, "idle");
        }
    }

    private Data state(Monster self) {
        return states.computeIfAbsent(self, s -> new Data());
    }

    private void switchAnimation(Monster self, Data data, String animation) {
        if (!animation.equals(data.currentAnim)) {
            self.setAnimation(animation);
            data.currentAnim = animation;
        }
    }

    private boolean advanceAnimation(Data data, int totalFrames) {
        if (data.animationFinished) return true;

        if (++data.frameTimer >= FRAME_DELAY_MONSTER) {
            data.frameTimer = 0;
            data.frameIndex++;
            if (data.frameIndex >= totalFrames) {
                data.frameIndex = totalFrames - 1;
                data.animationFinished = true;
            }
        }
        return data.animationFinished;
    }

    private void updateProjectiles(Data data, Player player, Monster self) {
        Iterator<Projectile> it = data.projectiles.iterator();
        while (it.hasNext()) {
            Projectile projectile = it.next();
            projectile.update(player, self.panelWidth, self.panelHeight);
            if (!projectile.isActive()) {
                it.remove();
            }
        }
    }

    private void fireProjectiles(Monster self, Player player, Data data) {
        double baseAngle = Math.atan2(
                player.getCenterY() - self.getCenterY(),
                player.getCenterX() - self.getCenterX());

        for (int angleOffset : new int[]{-15, 0, 15}) {
            double angle = baseAngle + Math.toRadians(angleOffset);
            Projectile projectile = new Projectile(
                    self.getCenterX(),
                    self.getCenterY(),
                    Math.cos(angle),
                    Math.sin(angle));
            data.projectiles.add(projectile);
        }
    }
}
