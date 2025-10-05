package entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.WeakHashMap;
import system.Config;
import system.Utils;

/**
 * StunAttack — พฤติกรรมของมอนสเตอร์ประเภท "ช็อต/สตัน"
 * ปล่อยคลื่นวงแหวนพลังงานออกจากตัว ถ้าผู้เล่นอยู่ในระยะจะถูกสตันชั่วคราว
 */
class StunAttack implements Monster.AttackBehavior {

    // เก็บสถานะเฉพาะของมอนสเตอร์แต่ละตัว เช่น เวลาคูลดาวน์และระยะ stun
    private static class Data {
        int stunTick;  // ตัวนับเวลาคลื่น stun กำลังทำงาน
        int cooldown;  // เวลารอระหว่างการปล่อยคลื่น
    }

    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    @Override
    public void reset(Monster self) {
        // รีเซ็ตสถานะเริ่มต้นเมื่อเข้าสู่เลเวลใหม่
        Data data = get(self);
        data.stunTick = Config.STUN_DURATION;
        data.cooldown = Config.STUN_COOLDOWN;
    }

    @Override
    public void attack(Monster self, Player player, system.Level level) {
        Data data = get(self);

        // ถ้าอยู่ในช่วงปล่อยคลื่น stun → ตรวจและใช้ผล stun กับผู้เล่น
        if (data.stunTick > 0) {
            data.stunTick--;
            applyStun(self, player, data);
            return;
        }

        // ถ้าอยู่ในช่วง cooldown → รอจนพร้อมปล่อยคลื่นรอบถัดไป
        if (data.cooldown > 0) {
            data.cooldown--;
            if (data.cooldown <= 60) { // เตรียมปล่อยคลื่นใหม่
                if (data.cooldown == 0) {
                    data.stunTick = Config.STUN_DURATION;
                    data.cooldown = Config.STUN_COOLDOWN;
                }
                return;
            }
        }

        // เดินตามผู้เล่นเมื่อไม่ได้สตันหรือคูลดาวน์
        self.follow(player.getX(), player.getY(), self.getSpeed());
    }

    @Override
    public void render(Graphics2D g, Monster self) {
        Data data = states.get(self);
        if (data == null || data.stunTick <= 0) return;

        // คำนวณการขยายของวงแหวนตามเวลาที่ผ่านไป
        float progress = 1f - (data.stunTick / (float) Config.STUN_DURATION);
        int min = self.getSize() / 2 + 6;
        int radius = (int) (min + (Config.STUN_RING_RADIUS - min) * progress);

        // ความโปร่งแสงของวงแหวนจะลดลงเมื่อใกล้จบ
        float alpha = Utils.clamp(0.6f * (1f - progress), 0f, 0.6f);

        // วาดวงแหวนสีน้ำเงินรอบมอนสเตอร์
        g.setColor(new Color(100, 200, 255));
        var oldStroke = g.getStroke();
        var oldComposite = g.getComposite();
        system.EffectRenderer.setAlpha(g, alpha);
        g.setStroke(new BasicStroke(8));

        int cx = self.getCenterX();
        int cy = self.getCenterY();
        int d = radius * 2;
        g.drawOval(cx - radius, cy - radius, d, d);

        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    /**
     * ตรวจว่าผู้เล่นอยู่ในระยะของคลื่น stun หรือไม่
     * ถ้าอยู่ในขอบเขตของวงแหวน → ให้สถานะสตันกับผู้เล่น
     */
    private void applyStun(Monster self, Player player, Data data) {
        if (player == null || player.isStunned()) return;

        float progress = 1f - (data.stunTick / (float) Config.STUN_DURATION);
        int min = self.getSize() / 2 + 6;
        int radius = (int) (min + (Config.STUN_RING_RADIUS - min) * progress);
        int inner = Math.max(0, radius - Config.STUN_RING_THICKNESS);
        int outer = radius + Config.STUN_RING_THICKNESS;

        int px = player.getCenterX();
        int py = player.getCenterY();
        int dist2 = Utils.distanceSquared(px, py, self.getCenterX(), self.getCenterY());

        // ถ้าผู้เล่นอยู่ระหว่างขอบในและขอบนอกของวงแหวน → โดนสตัน
        if (dist2 >= inner * inner && dist2 <= outer * outer) {
            player.applyStun(Config.STUN_DURATION);
        }
    }

    // คืนข้อมูลสถานะของมอนสเตอร์ ถ้ายังไม่มีจะสร้างใหม่
    private Data get(Monster self) {
        return states.computeIfAbsent(self, s -> new Data());
    }
}
