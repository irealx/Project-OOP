package entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.WeakHashMap;

import javax.xml.crypto.Data;

import system.Config;
import system.Utils;

// StunAttack — มอนสเตอร์ช็อตกระจายเป็นวงเพื่อหยุดผู้เล่นชั่วคราว
class StunAttack extends BaseAttack<StunAttack.State> {
    private static final int CAST_DELAY = 6;
    private static final int SKILL_FRAMES = Math.max(1, Monster.gMonsterAnimator().get("skill1").length);
    private static final long COOLDOWN_MS = Config.STUN_COOLDOWN * Config.TIMER_DELAY_MS;

    static class State extends BaseAttack.State {
        int stunTick;
        boolean casting;
    }

    @Override
    protected State createState() {
        return new State();
    }

    @Override
    public void reset(Monster self) {
        State data = state(self);
        data.stunTick = 0;
        data.casting = false;
        markCooldownWithDelay(data, COOLDOWN_MS, Config.MONSTER_INITIAL_DELAY_MS);
        switchAnimation(self, data, "idle");
    }

    @Override
    public void attack(Monster self, Player player, system.Level level) {
        State data = state(self);
        if (handleWave(self, player, data)) return;
        if (player == null) { cancelCasting(self, data); return; }
        if (handleCasting(self, player, data)) return;
        switchAnimation(self, data, "idle");
        if (cooldownReady(data, COOLDOWN_MS)) {
            startCasting(self, data);
            return;
        }
        self.follow(player.getX(), player.getY());
    }

    @Override
    public void render(Graphics2D g, Monster self) {
        State data = state(self);
        if (data.stunTick <= 0) return;
        drawRing(g, self, data);
    }

    private boolean handleWave(Monster self, Player player, State data) {
        if (data.stunTick <= 0) return false;
        data.stunTick--;
        applyStun(self, player, data);
        return true;
    }

    private boolean handleCasting(Monster self, Player player, State data) {
        if (!data.casting) return false;
        self.setVelocity(0, 0);
        switchAnimation(self, data, "skill1");
        if (!advanceAnimation(data, SKILL_FRAMES, CAST_DELAY)) return true;
        data.casting = false;
        data.stunTick = Config.STUN_DURATION;
        markCooldown(data);
        switchAnimation(self, data, "idle");
        return true;
    }

    private void startCasting(Monster self, State data) {
        data.casting = true;
        data.stunTick = 0;
        resetAnimationState(data);
        switchAnimation(self, data, "skill1");
    }

    private void cancelCasting(Monster self, State data) {
        if (!data.casting) return;
        data.casting = false;
        switchAnimation(self, data, "idle");
    }
    private void applyStun(Monster self, Player player, State data) {
        if (player == null || player.isStunned()) return;
        // คำนวณการขยายของวงแหวนตามเวลาที่ผ่านไป
        float progress = 1f - (data.stunTick / (float) Config.STUN_DURATION);
        int base = self.getSize() / 2 + 6;
        int radius = (int) (base + (Config.STUN_RING_RADIUS - base) * progress);
        int inner = Math.max(0, radius - Config.STUN_RING_THICKNESS);
        int outer = radius + Config.STUN_RING_THICKNESS;
        int dist2 = self.distanceSquaredTo(player.getCenterX(), player.getCenterY());
        if (dist2 < inner * inner || dist2 > outer * outer) return;
        player.applyStun(Config.STUN_DURATION);
    }

        // ความโปร่งแสงของวงแหวนจะลดลงเมื่อใกล้จบ
    private void drawRing(Graphics2D g, Monster self, State data) {
        float progress = 1f - (data.stunTick / (float) Config.STUN_DURATION);
        int base = self.getSize() / 2 + 6;
        int radius = (int) (base + (Config.STUN_RING_RADIUS - base) * progress);
        float alpha = Utils.clamp(0.6f * (1f - progress), 0f, 0.6f);

        // วาดวงแหวนสีน้ำเงินรอบมอนสเตอร์
        var oldStroke = g.getStroke();
        var oldComposite = g.getComposite();
        g.setColor(new Color(100, 200, 255));
        system.EffectRenderer.setAlpha(g, alpha);
        g.setStroke(new BasicStroke(8));
        int cx = self.getCenterX();
        int cy = self.getCenterY();
        int d = radius * 2;
        g.drawOval(cx - radius, cy - radius, d, d);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }
}
