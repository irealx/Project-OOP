package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import system.Config;
import system.Utils;

// REFACTOR: Sprite.java เก็บโค้ดพื้นฐานที่ทุกตัวละครใช้ร่วมกัน เช่น การเคลื่อนไหวและการชน
public abstract class Sprite {
    protected int x;
    protected int y;

    protected int dx;
    protected int dy;
    protected int speed;
    protected int size;
    protected int panelWidth = Config.PANEL_WIDTH;
    protected int panelHeight = Config.PANEL_HEIGHT;
    private BufferedImage frame;
    private Color fallbackColor = Color.WHITE;
    protected Sprite(int size, int speed) {
        this.size = size;
        this.speed = speed;
    }
    public void setFrame(BufferedImage frame, Color fallback) {
        this.frame = frame;
        if (fallback != null) {
            this.fallbackColor = fallback;
        }
    }
    public void updateBounds(int width, int height) {
        panelWidth = Math.max(size, width);
        panelHeight = Math.max(size, height);
        clamp();
    }
    protected void setPosition(int nx, int ny) {
        x = nx;
        y = ny;
        clamp();
    }

    /** เคลื่อนที่ด้วยค่า dx, dy ตรง ๆ พร้อมกัน แล้วบีบให้อยู่ในกรอบ */
    protected void setVelocity(int ndx, int ndy) {
        dx = ndx;
        dy = ndy;
    }
    protected void center() {
        setPosition((panelWidth - size) / 2, (panelHeight - size) / 2);
    }
    protected void clamp() {
        x = Utils.clamp(x, 0, panelWidth - size);
        y = Utils.clamp(y, 0, panelHeight - size);
    }
    protected void wrap() {
        if (x < -size) x = panelWidth - Config.WRAP_MARGIN;
        if (x > panelWidth) x = Config.WRAP_MARGIN;
        if (y < -size) y = panelHeight - Config.WRAP_MARGIN;
        if (y > panelHeight) y = Config.WRAP_MARGIN;
    }
    protected void follow(int targetX, int targetY, int customSpeed) {
        dx = Integer.compare(targetX, x) * customSpeed;
        dy = Integer.compare(targetY, y) * customSpeed;
    }
    protected void follow(int targetX, int targetY) {
        follow(targetX, targetY, speed);
    }
    public void pushOutside(int rectX, int rectY, int rectW, int rectH) {
        if (!intersects(rectX, rectY, rectW, rectH)) {
            return;
        }
        int right = x + size;
        int bottom = y + size;
        int rectRight = rectX + rectW;
        int rectBottom = rectY + rectH;
        int overlapLeft = right - rectX;
        int overlapRight = rectRight - x;
        int overlapTop = bottom - rectY;
        int overlapBottom = rectBottom - y;
        int min = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));
        if (min == overlapLeft) {
            x = rectX - size - 1;
        } else if (min == overlapRight) {
            x = rectRight + 1;
        } else if (min == overlapTop) {
            y = rectY - size - 1;
        } else {
            y = rectBottom + 1;
        }
        clamp();
    }
    protected void updateBase() {
        if (dx != 0 || dy != 0) {
            x += dx;
            y += dy;
        }
        dx = dy = 0;
    }
    protected void drawBase(Graphics2D g) {
        if (frame != null) {
            g.drawImage(frame, x, y, size, size, null);
            return;
        }
        g.setColor(fallbackColor);
        g.fillRect(x, y, size, size);
    }
    public boolean intersects(Sprite other) {
        return other != null && intersects(other.x, other.y, other.size, other.size);
    }
    public boolean intersects(int ox, int oy, int ow, int oh) {
        return x < ox + ow && x + size > ox && y < oy + oh && y + size > oy;
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getCenterX() { return x + size / 2; }
    public int getCenterY() { return y + size / 2; }
    public int getSize() { return size; }
    public int getSpeed() { return speed; }
}