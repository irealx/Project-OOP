package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * จัดการกระสุนพลังงานของมอนสเตอร์สายยิง
 * ใช้ sprite summonIdle.png (4 เฟรม ขนาด 100x100)
 */

public class Projectile {
    private static final double SPEED = 6.0;
    private static final double MAX_DISTANCE = 640.0;
    private static final int FRAME_DELAY = 5;
    private static final BufferedImage[] FRAMES =
            Monster.gMonsterAnimator().get("summonIdle");
    private static final int BASE_FRAME_SIZE =
            (FRAMES.length > 0 ? FRAMES[0].getWidth() : 100);
    private static final double DRAW_SCALE = 0.6; // ย่อให้กระสุนไม่ใหญ่จนเกินไป
    private static final int DRAW_SIZE = (int) Math.round(BASE_FRAME_SIZE * DRAW_SCALE);

    private double x;
    private double y;
    private double dx;
    private double dy;
    private double distance;
    private boolean active = true;
    private int frameIndex;
    private int frameTimer;

    public Projectile(double startX, double startY, double dirX, double dirY) {
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

    public void update(Player player, int boundsW, int boundsH) {
        if (!active) return;

        x += dx * SPEED;
        y += dy * SPEED;
        distance += SPEED;

        if (distance >= MAX_DISTANCE) {
            active = false;
            return;
        }

        if (x < -DRAW_SIZE || y < -DRAW_SIZE
                || x > boundsW + DRAW_SIZE || y > boundsH + DRAW_SIZE) {
            active = false;
            return;
        }

        if (player != null && !player.isDead()) {
            int px = player.getX();
            int py = player.getY();
            int size = player.getSize();
            if (x + DRAW_SIZE / 2.0 > px && x - DRAW_SIZE / 2.0 < px + size
                    && y + DRAW_SIZE / 2.0 > py && y - DRAW_SIZE / 2.0 < py + size) {
                player.die();
                active = false;
                return;
            }
        }

        if (++frameTimer >= FRAME_DELAY) {
            frameTimer = 0;
            frameIndex = (frameIndex + 1) % Math.max(1, FRAMES.length);
        }
    }

    public void draw(Graphics2D g) {
        if (!active) return;

        int drawX = (int) Math.round(x - DRAW_SIZE / 2.0);
        int drawY = (int) Math.round(y - DRAW_SIZE / 2.0);

        if (FRAMES.length > 0) {
            BufferedImage frame = FRAMES[frameIndex % FRAMES.length];
            g.drawImage(frame, drawX, drawY, DRAW_SIZE, DRAW_SIZE, null);
            return;
        }

        g.setColor(new Color(120, 200, 255));
        g.fillOval(drawX, drawY, DRAW_SIZE, DRAW_SIZE);
    }

    public boolean isActive() {
        return active;
    }
}
