package entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.WeakHashMap;
import system.Config;
import system.Utils;

// REFACTOR: กลยุทธ์การโจมตีแต่ละแบบถูกแยกออกจากกันเพื่อให้เพิ่ม/แก้ได้ง่ายและไม่ซ้ำโค้ด
class StunAttack implements Monster.AttackBehavior {
    private static class Data {
        int stunTick;
        int cooldown;
    }
    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    @Override
    public void reset(Monster self) {
        Data data = get(self);
        data.stunTick = Config.STUN_DURATION;
        data.cooldown = Config.STUN_COOLDOWN;
    }

    @Override
    public void attack(Monster self, Player player, system.Level level) {
        Data data = get(self);
        if (data.stunTick > 0) {
            data.stunTick--;
            applyStun(self, player, data);
            return;
        }
        if (data.cooldown > 0) {
            data.cooldown--;
            if (data.cooldown <= 60) {
                if (data.cooldown == 0) {
                    data.stunTick = Config.STUN_DURATION;
                    data.cooldown = Config.STUN_COOLDOWN;
                }
                return;
            }
        }
        self.follow(player.getX(), player.getY(), self.getSpeed());
    }

    @Override
    public void render(Graphics2D g, Monster self) {
        Data data = states.get(self);
        if (data == null || data.stunTick <= 0) {
            return;
        }
        float progress = 1f - (data.stunTick / (float) Config.STUN_DURATION);
        int min = self.getSize() / 2 + 6;
        int radius = (int) (min + (Config.STUN_RING_RADIUS - min) * progress);
        float alpha = Utils.clamp(0.6f * (1f - progress), 0f, 0.6f);
        g.setColor(new Color(100, 200, 255));
        var old = g.getStroke();
        var composite = g.getComposite();
        system.EffectRenderer.setAlpha(g, alpha);
        g.setStroke(new BasicStroke(8));
        int cx = self.getCenterX();
        int cy = self.getCenterY();
        int d = radius * 2;
        g.drawOval(cx - radius, cy - radius, d, d);
        g.setStroke(old);
        g.setComposite(composite);
    }
    private void applyStun(Monster self, Player player, Data data) {
        if (player == null || player.isStunned()) {
            return;
        }
        int min = self.getSize() / 2 + 6;
        float progress = 1f - (data.stunTick / (float) Config.STUN_DURATION);
        int radius = (int) (min + (Config.STUN_RING_RADIUS - min) * progress);
        int inner = Math.max(0, radius - Config.STUN_RING_THICKNESS);
        int outer = radius + Config.STUN_RING_THICKNESS;
        int px = player.getCenterX();
        int py = player.getCenterY();
        int dist2 = Utils.distanceSquared(px, py, self.getCenterX(), self.getCenterY());
        if (dist2 >= inner * inner && dist2 <= outer * outer) {
            player.applyStun(Config.STUN_DURATION);
        }
    }
    private Data get(Monster self) {
        return states.computeIfAbsent(self, s -> new Data());
    }
}