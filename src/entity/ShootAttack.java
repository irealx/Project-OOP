package entity;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.crypto.Data;
import system.Level;
import static system.Config.*;

// ShootAttack — มอนสเตอร์ยิงพลังงานกระจายสามทิศพร้อมเอฟเฟกต์กระสุน
public class ShootAttack extends BaseAttack<ShootAttack.State> {
    private static final int SUMMON_FRAMES = Math.max(1,
        Monster.gMonsterAnimator().get("summon").length);
    private static final long COOLDOWN_MS = SHOOT_COOLDOWN_TICKS * TIMER_DELAY_MS;
    
    static class State extends BaseAttack.State{
        boolean attacking;
        boolean fired;
        final List<Projectile> projectiles = new ArrayList<>();
    }

    @Override
    protected State createState() {
        return new State();
    }

    @Override
    public void attack(Monster self, Player player, Level level) {
        State data = state(self);
        updateProjectiles(data, player, self);
        if (player == null) { stopCasting(self, data); return; }
        if (data.attacking) { handleCasting(self, player, data); return; }
        switchAnimation(self, data, "idle");

        self.follow(player.getX(), player.getY());

        if (!cooldownReady(data, COOLDOWN_MS)) return;
        if (!withinRange(self, player)) return;
        beginSummon(self, data);
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
        State data = state(self);
        stopCasting(self, data);
        data.projectiles.clear();
        markCooldownWithDelay(data, COOLDOWN_MS, 2000);
        switchAnimation(self, data, "idle");
    }

    // ===== ระหว่างกำลังร่าย summon =====
    private void handleCasting(Monster self, Player player, State data) {
        self.setVelocity(0, 0);
        switchAnimation(self, data, "summon");
        if (!data.fired && data.frameIndex >= SUMMON_FRAMES - 1) {
            // ยิงกระสุน 3 ทิศเมื่อถึงเฟรมสุดท้าย
            fireProjectiles(self, player, data);
            data.fired = true;
            markCooldown(data);
        }
        if (!advanceAnimation(data, SUMMON_FRAMES)) return;
        data.attacking = false;
        switchAnimation(self, data, "idle");
    }

    private void beginSummon(Monster self, State data) {
        data.attacking = true;
        data.fired = false;
        resetAnimationState(data);
        switchAnimation(self, data, "summon");
    }

    private void stopCasting(Monster self, State data) {
        data.attacking = false;
        data.fired = false;
        resetAnimationState(data);
        switchAnimation(self, data, "idle");
    }

    private void updateProjectiles(State data, Player player, Monster self) {
        Iterator<Projectile> iterator = data.projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(player, self.panelWidth, self.panelHeight);
            if (projectile.isActive()) continue;
            iterator.remove();
        }
    }

    private boolean withinRange(Monster self, Player player) {
        return self.distanceSquaredTo(player.getCenterX(), player.getCenterY()) <= WARP_RANGE * WARP_RANGE;
    }

    private void fireProjectiles(Monster self, Player player, State data) {
        double baseAngle = Math.atan2(player.getCenterY() - self.getCenterY(), player.getCenterX() - self.getCenterX());
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
