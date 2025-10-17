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
    private static final int CAST_DELAY = 6; // หน่วงเฟรมระหว่าง skill1
    private static final int SKILL_FRAMES = Math.max(1,
            Monster.gMonsterAnimator().get("skill1").length);

    // เก็บสถานะเฉพาะของมอนสเตอร์แต่ละตัว เช่น เวลาคูลดาวน์และระยะ stun
    private static class Data {
        int stunTick;          // ตัวนับเวลาคลื่น stun กำลังทำงาน
        int cooldown;          // เวลารอระหว่างการปล่อยคลื่น
        boolean casting;       // กำลังเล่น skill1 หรือไม่
        int castFrameIndex;    // เฟรมปัจจุบันของ skill1
        int castFrameTimer;    // ตัวนับดีเลย์ของ skill1
        boolean castFinished;  // เล่น skill1 จบแล้วหรือยัง
        String currentAnim = ""; // จดจำแอนิเมชันล่าสุดที่สั่งเล่น
    }

    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    @Override
    public void reset(Monster self) {
        Data data = get(self);
        data.stunTick = 0;
        data.cooldown = Config.STUN_COOLDOWN;
        data.casting = false;
        data.castFrameIndex = 0;
        data.castFrameTimer = 0;
        data.castFinished = false;
        data.currentAnim = "";
        switchAnimation(self, data, "idle");
    }

    @Override
    public void attack(Monster self, Player player, system.Level level) {
        Data data = get(self);

        if (data.stunTick > 0) {
            // 🔹 เมื่อวง stun ทำงาน → ตรวจผู้เล่นทุกเฟรม
            data.stunTick--;
            applyStun(self, player, data);
            return;
        }

        if (data.casting) {
            // 🔸 ระหว่างร่าย skill1 ให้ยืนนิ่งและเล่นแอนิเมชันก่อน
            self.setVelocity(0, 0);
            switchAnimation(self, data, "skill1");
            if (advanceCasting(data)) {
                data.casting = false;
                data.stunTick = Config.STUN_DURATION;
                data.cooldown = Config.STUN_COOLDOWN;
                switchAnimation(self, data, "idle");
            }
            return;
        }

        if (data.cooldown > 0) {
            // 🔹 นับถอยหลังจนกว่าจะถึงรอบร่ายใหม่
            data.cooldown--;
            if (data.cooldown <= 0) {
                startCasting(self, data);
                return;
            }
        }

        // 🔹 ยังไม่ถึงเวลา → ให้เดินตามผู้เล่นพร้อมอนิเมชัน idle
        switchAnimation(self, data, "idle");
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
    private void startCasting(Monster self, Data data) {
        data.casting = true;
        data.castFrameIndex = 0;
        data.castFrameTimer = 0;
        data.castFinished = false;
        switchAnimation(self, data, "skill1");
    }

    private boolean advanceCasting(Data data) {
        if (data.castFinished) return true;

        if (++data.castFrameTimer >= CAST_DELAY) {
            data.castFrameTimer = 0;
            data.castFrameIndex++;
            if (data.castFrameIndex >= SKILL_FRAMES) {
                data.castFrameIndex = SKILL_FRAMES - 1;
                data.castFinished = true;
            }
        }
        return data.castFinished;
    }

    private void switchAnimation(Monster self, Data data, String animation) {
        if (!animation.equals(data.currentAnim)) {
            self.setAnimation(animation);
            data.currentAnim = animation;
        }
    }
}
