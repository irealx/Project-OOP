package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import system.Level;

/**
 * ShootAttack — มอนสเตอร์สายยิงพลังงาน (ภาษาไทย)
 * ใช้แอนิเมชัน summon.png ก่อนปล่อยกระสุน 3 ทิศทางพร้อมกัน
 */
public class ShootAttack implements Monster.AttackBehavior {

    // ===== ค่าคงที่หลัก =====
    private static final long SHOOT_COOLDOWN_MS = 2000L;                 // พัก 2 วินาทีระหว่างชุดยิง
    private static final int FRAME_DELAY = 6;                            // ความเร็วเฟรมของแอนิเมชัน summon
    private static final int RANGE = 360;                                 // ระยะที่เริ่มยิง
    private static final double PROJECTILE_SPEED = 6.0;                  // ความเร็วกระสุน
    private static final double PROJECTILE_MAX_DISTANCE = 640.0;         // ระยะสูงสุดก่อนหายไป
    private static final int PROJECTILE_SIZE = 26;                       // ขนาดสเกลการวาดกระสุน
    private static final int SUMMON_FRAMES = Math.max(1,
            Monster.gMonsterAnimator().get("summon").length);
    private static final BufferedImage[] PROJECTILE_FRAMES =
            Monster.gMonsterAnimator().get("summonIdle");

    private static class Data {
        boolean attacking;
        boolean fired;
        int frameIndex;
        int frameTimer;
        boolean animationFinished;
        long lastShotTime = System.currentTimeMillis();
        final List<Projectile> projectiles = new ArrayList<>();
        String currentAnim = "";
    }

    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    @Override
    public void attack(Monster self, Player player, Level level) {
        Data data = state(self);

        // 🔹 อัปเดตกระสุนก่อนเสมอ
        updateProjectiles(data, player, self);

        if (player == null) {
            switchAnimation(self, data, "idle");
            data.attacking = false;
            return;
        }

        if (data.attacking) {
            // 🔸 ระหว่างร่ายให้ยืนกับที่
            self.setVelocity(0, 0);
            handleCasting(self, player, data);
            return;
        }

        switchAnimation(self, data, "idle");

        // 🔹 เคลื่อนเข้าหาผู้เล่นอย่างค่อยเป็นค่อยไป
        self.follow(player.getX(), player.getY());

        long now = System.currentTimeMillis();
        if (now - data.lastShotTime < SHOOT_COOLDOWN_MS) return;

        int dx = player.getCenterX() - self.getCenterX();
        int dy = player.getCenterY() - self.getCenterY();
        if (dx * dx + dy * dy > RANGE * RANGE) return;

        // 🔹 เริ่มเล่นแอนิเมชัน summon.png
        data.attacking = true;
        data.fired = false;
        data.frameIndex = data.frameTimer = 0;
        data.animationFinished = false;
        switchAnimation(self, data, "summon");
    }

    @Override
    public void afterUpdate(Monster self) {
        // 🔸 บังคับไม่ให้ออกนอกกรอบจอ
        self.clamp();
    }
    @Override
    public void render(Graphics2D g, Monster self) {
        // 🔹 วาดกระสุนทั้งหมด
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
            // 🔸 ยิงกระสุน 3 ทิศเมื่อถึงเฟรมสุดท้าย
            fireProjectiles(self, player, data);
            data.fired = true;
            data.lastShotTime = System.currentTimeMillis();
        }

        if (advanceAnimation(data, SUMMON_FRAMES)) {
            // 🔹 เมื่อเล่นครบ → กลับไป idle
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

    private void updateProjectiles(Data data, Player player, Monster self) {
        Iterator<Projectile> it = data.projectiles.iterator();
        while (it.hasNext()) {
            Projectile projectile = it.next();
            projectile.update(player, self.panelWidth, self.panelHeight);
            if (!projectile.active) {
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

    // ===== คลาสภายในสำหรับกระสุน =====
    static class Projectile {
        double x, y, dx, dy;
        final double speed = PROJECTILE_SPEED;
        double distance;
        final double maxDistance = PROJECTILE_MAX_DISTANCE;
        int frameIndex;
        int frameTimer;
        boolean active = true;

        private static final int FRAME_DELAY = 5;

        Projectile(double startX, double startY, double dirX, double dirY) {
            this.x = startX;
            this.y = startY;
            double len = Math.hypot(dirX, dirY);
            if (len < 1e-4) {
                this.dx = 1;
                this.dy = 0;
            } else {
                this.dx = dirX / len;
                this.dy = dirY / len;
            }
        }

        void update(Player player, int boundsW, int boundsH) {
            if (!active) return;

            x += dx * speed;
            y += dy * speed;
            distance += speed;

            // 🔹 ถ้าเดินไกลเกินกำหนดให้สลายตัว
            if (distance >= maxDistance) {
                active = false;
                return;
            }

            // 🔹 ถ้าออกนอกขอบให้หยุดแสดงทันที
            if (x < -PROJECTILE_SIZE || y < -PROJECTILE_SIZE
                    || x > boundsW + PROJECTILE_SIZE || y > boundsH + PROJECTILE_SIZE) {
                active = false;
                return;
            }

            // 🔸 ตรวจการชนกับผู้เล่น (โดนแล้วตายทันที)
            if (player != null && !player.isDead()) {
                int px = player.getX();
                int py = player.getY();
                int size = player.getSize();
                if (x + PROJECTILE_SIZE / 2.0 > px && x - PROJECTILE_SIZE / 2.0 < px + size
                        && y + PROJECTILE_SIZE / 2.0 > py && y - PROJECTILE_SIZE / 2.0 < py + size) {
                    player.die();
                    active = false;
                    return;
                }
            }

            // 🔹 หมุนเฟรมของกระสุนให้เคลื่อนไหวต่อเนื่อง
            if (++frameTimer >= FRAME_DELAY) {
                frameTimer = 0;
                frameIndex = (frameIndex + 1) % Math.max(1, PROJECTILE_FRAMES.length);
            }
        }

        void draw(Graphics2D g) {
            if (!active) return;

            int drawX = (int) Math.round(x - PROJECTILE_SIZE / 2.0);
            int drawY = (int) Math.round(y - PROJECTILE_SIZE / 2.0);
            if (PROJECTILE_FRAMES.length > 0) {
                BufferedImage frame = PROJECTILE_FRAMES[frameIndex % PROJECTILE_FRAMES.length];
                g.drawImage(frame, drawX, drawY, PROJECTILE_SIZE, PROJECTILE_SIZE, null);
                return;
            }

            // 🔸 fallback เมื่อโหลดรูปไม่สำเร็จ
            g.setColor(new Color(120, 200, 255));
            g.fillOval(drawX, drawY, PROJECTILE_SIZE, PROJECTILE_SIZE);
        }
    }
}
