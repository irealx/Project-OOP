package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import system.Config;
import system.Utils;

/**
 * Sprite — คลาสพื้นฐานของทุกเอนทิตี (Player, Monster ฯลฯ)
 * รวมฟังก์ชันพื้นฐานที่ใช้ร่วมกัน เช่น การเคลื่อนไหว การชน และการวาดภาพ
 */
public abstract class Sprite {

    // พิกัดและความเร็ว
    protected int x, y;
    protected int dx, dy;
    protected int speed;
    protected int size;

    // ขอบเขตของพื้นที่เกม
    protected int panelWidth = Config.PANEL_WIDTH;
    protected int panelHeight = Config.PANEL_HEIGHT;

    // ภาพ sprite และสีสำรอง (ใช้เมื่อโหลดภาพไม่ได้)
    private BufferedImage frame;
    private Color fallbackColor = Color.WHITE;

    protected Sprite(int size, int speed) {
        this.size = size;
        this.speed = speed;
    }

    // ตั้งค่าภาพและสีสำรอง
    public void setFrame(BufferedImage frame, Color fallback) {
        this.frame = frame;
        if (fallback != null) this.fallbackColor = fallback;
    }

    // อัปเดตขอบเขตของจอเกม และบีบให้อยู่ในขอบ
    public void updateBounds(int width, int height) {
        panelWidth = Math.max(size, width);
        panelHeight = Math.max(size, height);
        clamp();
    }

    // ตั้งตำแหน่งใหม่โดยบังคับให้อยู่ในขอบเขต
    protected void setPosition(int nx, int ny) {
        x = nx;
        y = ny;
        clamp();
    }

    // กำหนดความเร็วในแกน x, y
    protected void setVelocity(int ndx, int ndy) {
        dx = ndx;
        dy = ndy;
    }

    // จัดให้อยู่กลางจอ
    protected void center() {
        setPosition((panelWidth - size) / 2, (panelHeight - size) / 2);
    }

    // ป้องกันไม่ให้ออกนอกขอบจอ
    protected void clamp() {
        x = Utils.clamp(x, 0, panelWidth - size);
        y = Utils.clamp(y, 0, panelHeight - size);
    }

    // เคลื่อนไหวแบบ “ห่อกลับ” เมื่อออกนอกขอบจอ (ใช้ในมอนสเตอร์บางตัว)
    protected void wrap() {
        if (x < -size) x = panelWidth - Config.WRAP_MARGIN;
        if (x > panelWidth) x = Config.WRAP_MARGIN;
        if (y < -size) y = panelHeight - Config.WRAP_MARGIN;
        if (y > panelHeight) y = Config.WRAP_MARGIN;
    }

    // เดินตามตำแหน่งเป้าหมายด้วยความเร็วที่กำหนดเอง
    protected void follow(int targetX, int targetY, int customSpeed) {
        dx = Integer.compare(targetX, x) * customSpeed;
        dy = Integer.compare(targetY, y) * customSpeed;
    }

    // เดินตามเป้าหมายด้วยความเร็วปกติ
    protected void follow(int targetX, int targetY) {
        follow(targetX, targetY, speed);
    }

    // ดันตัวเองออกนอกสี่เหลี่ยม (ใช้เวลาโดนชนประตู)
    public void pushOutside(int rectX, int rectY, int rectW, int rectH) {
        if (!intersects(rectX, rectY, rectW, rectH)) return;

        int right = x + size;
        int bottom = y + size;
        int rectRight = rectX + rectW;
        int rectBottom = rectY + rectH;

        int overlapLeft = right - rectX;
        int overlapRight = rectRight - x;
        int overlapTop = bottom - rectY;
        int overlapBottom = rectBottom - y;

        int min = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        // ดันออกจากด้านที่ชนใกล้ที่สุด
        if (min == overlapLeft) x = rectX - size - 1;
        else if (min == overlapRight) x = rectRight + 1;
        else if (min == overlapTop) y = rectY - size - 1;
        else y = rectBottom + 1;

        clamp();
    }

    // อัปเดตตำแหน่งพื้นฐานจากความเร็ว แล้วรีเซ็ต dx, dy
    protected void updateBase() {
        if (dx != 0 || dy != 0) {
            x += dx;
            y += dy;
        }
        dx = dy = 0;
    }

    // วาด sprite พื้นฐาน (ใช้ภาพถ้ามี ไม่งั้นใช้สี่เหลี่ยมสี)
    protected void drawBase(Graphics2D g) {
        if (frame != null) {
            g.drawImage(frame, x, y, size, size, null);
            return;
        }
        g.setColor(fallbackColor);
        g.fillRect(x, y, size, size);
    }

    // ตรวจการชนกับ Sprite อื่น
    public boolean intersects(Sprite other) {
        return other != null && intersects(other.x, other.y, other.size, other.size);
    }

    // ตรวจการชนกับพิกัดที่กำหนด
    public boolean intersects(int ox, int oy, int ow, int oh) {
        return x < ox + ow && x + size > ox && y < oy + oh && y + size > oy;
    }

    // Getter พื้นฐาน
    public int getX() { return x; }
    public int getY() { return y; }
    public int getCenterX() { return x + size / 2; }
    public int getCenterY() { return y + size / 2; }
    public int getSize() { return size; }
    public int getSpeed() { return speed; }
}
