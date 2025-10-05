package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * คลาสแม่สำหรับวัตถุที่เคลื่อนไหวได้ในฉาก (ผู้เล่น/มอนสเตอร์)
 * รวมการคำนวณตำแหน่ง การชน และการวาดพื้นฐานให้ใช้ร่วมกัน
 */
public abstract class Sprite {

    // พิกัดมุมซ้ายบนของสไปรต์บนฉาก
    protected int x;
    protected int y;

    private final int size;      // ขนาด hitbox สี่เหลี่ยมของสไปรต์
    private final int speed;     // ความเร็วพื้นฐาน (ใช้กับการเคลื่อนที่ทั่วไป)
    private int panelWidth;      // ความกว้างฉากปัจจุบัน
    private int panelHeight;     // ความสูงฉากปัจจุบัน

    protected Sprite(int size, int speed) {
        this.size = size;
        this.speed = speed;
    }

    /** ปรับขนาดฉากที่สไปรต์สามารถเคลื่อนที่ได้ และบีบตำแหน่งให้อยู่ในกรอบ */
    public void updateBounds(int panelWidth, int panelHeight) {
        this.panelWidth = Math.max(size, panelWidth);
        this.panelHeight = Math.max(size, panelHeight);
        clampToBounds();
    }

    /** จัดตำแหน่งให้อยู่กลางหน้าจอ (ใช้ตอนรีเซ็ตหรือเกิดใหม่) */
    public void centerOnScreen() {
        setPosition(panelWidth / 2 - size / 2, panelHeight / 2 - size / 2);
    }

    /** ตั้งตำแหน่งใหม่โดยตรง แล้วบีบให้อยู่ในกรอบ */
    protected void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        clampToBounds();
    }

    /** เคลื่อนที่ด้วยค่า dx, dy ตรง ๆ พร้อมกัน แล้วบีบให้อยู่ในกรอบ */
    protected void moveBy(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        clampToBounds();
    }

    /** เคลื่อนที่เข้าหาจุดเป้าหมายด้วยความเร็วพื้นฐานของสไปรต์ */
    protected void moveToward(int targetX, int targetY) {
        moveToward(targetX, targetY, speed);
    }

    /** เคลื่อนที่เข้าหาจุดเป้าหมายด้วยความเร็วที่กำหนดเอง */
    protected void moveToward(int targetX, int targetY, int customSpeed) {
        int vx = 0;
        int vy = 0;

        if (targetX < x) {
            vx = -customSpeed;
        } else if (targetX > x) {
            vx = customSpeed;
        }

        if (targetY < y) {
            vy = -customSpeed;
        } else if (targetY > y) {
            vy = customSpeed;
        }

        moveBy(vx, vy);
    }
    /** บังคับให้อยู่ในขอบเขตของฉาก */
    public void clampToBounds() {
        x = Math.max(0, Math.min(panelWidth - size, x));
        y = Math.max(0, Math.min(panelHeight - size, y));
    }

    /** สุ่มเกิดตามมุมของฉาก (ใช้กับมอนสเตอร์) */
    public void spawnAtRandomCorner(Random random) {
        int[][] corners = new int[][] {
            {10, 10},
            {Math.max(0, panelWidth - size - 10), 10},
            {10, Math.max(0, panelHeight - size - 10)},
            {Math.max(0, panelWidth - size - 10), Math.max(0, panelHeight - size - 10)}
        };
        int[] corner = corners[random.nextInt(corners.length)];
        setPosition(corner[0], corner[1]);
    }

    /** ตรวจการชนกับสี่เหลี่ยมอื่น ๆ */
    public boolean intersects(int otherX, int otherY, int otherWidth, int otherHeight) {
        return x < otherX + otherWidth &&
               x + size > otherX &&
               y < otherY + otherHeight &&
               y + size > otherY;
    }

    /** ตรวจการชนกับสไปรต์อีกตัว */
    public boolean intersects(Sprite other) {
        return intersects(other.x, other.y, other.size, other.size);
    }

    /** ดันตัวสไปรต์ให้ออกจากพื้นที่สี่เหลี่ยมที่กำหนด (ใช้หลังชนประตู) */
    public void pushOutsideRect(int rectX, int rectY, int rectWidth, int rectHeight) {
        if (!intersects(rectX, rectY, rectWidth, rectHeight)) {
            return;
        }

        int right = x + size;
        int bottom = y + size;
        int rectRight = rectX + rectWidth;
        int rectBottom = rectY + rectHeight;

        int overlapLeft = right - rectX;
        int overlapRight = rectRight - x;
        int overlapTop = bottom - rectY;
        int overlapBottom = rectBottom - y;

        int minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapLeft) {
            x = rectX - size - 1;
        } else if (minOverlap == overlapRight) {
            x = rectRight + 1;
        } else if (minOverlap == overlapTop) {
            y = rectY - size - 1;
        } else {
            y = rectBottom + 1;
        }

        clampToBounds();
    }

    /** ดันออกจากสี่เหลี่ยมจัตุรัส (ช่วยให้อ่านโค้ดง่ายขึ้น) */
    public void pushOutsideSquare(int rectX, int rectY, int rectSize) {
        pushOutsideRect(rectX, rectY, rectSize, rectSize);
    }

    /** วาดสี่เหลี่ยมสีเรียบ (ใช้เป็น fallback หรือวัตถุอย่างมอนสเตอร์) */
    public void drawAsBox(Graphics2D g2d, Color color) {
        g2d.setColor(color);
        g2d.fillRect(x, y, size, size);
    }
    
    /**
     * วาดเฟรมภาพสไปรต์พร้อมขยายให้ใหญ่กว่าขนาด hitbox เล็กน้อย
     * @param g2d กราฟิกที่ใช้วาด
     * @param frame ภาพเฟรม (null = ใช้สีสำรอง)
     * @param facingLeft หันซ้ายอยู่หรือไม่
     * @param fallbackColor สีสำรองเมื่อไม่มีภาพให้วาด
     */
    protected void drawFrame(Graphics2D g2d, BufferedImage frame, boolean facingLeft, Color fallbackColor) {
        if (frame == null) {
            drawAsBox(g2d, fallbackColor);
            return;
        }

        double spriteScale = 12; // ขยายสไปรต์ให้ใหญ่กว่ากล่องชน
        double scaleX = spriteScale * size / (double) frame.getWidth();
        double scaleY = spriteScale * size / (double) frame.getHeight();
        AffineTransform transform = new AffineTransform();
        double centerX = x + size / 2.0;
        double centerY = y + size / 2.0;
        transform.translate(centerX, centerY);
        if (facingLeft) {
            transform.scale(-scaleX, scaleY);
        } else {
            transform.scale(scaleX, scaleY);
        }
        transform.translate(-frame.getWidth() / 2.0, -frame.getHeight() / 2.0);

        g2d.drawImage(frame, transform, null);
    }


    public static void drawPlayer(Graphics2D g2d, BufferedImage frame, int x, int y,
                                  int hitboxSize, boolean facingLeft, Color fallbackColor) {
        if (frame == null) {
            // ถ้าไม่มีภาพให้ใช้สี่เหลี่ยมสีแทนเพื่อไม่ให้วัตถุหาย
            g2d.setColor(fallbackColor);
            g2d.fillRect(x, y, hitboxSize, hitboxSize);
            return;
        }

        double spriteScale = 12; // อัตราส่วนขยายของสไปรต์จาก hitbox เดิม
        double scaleX = spriteScale * hitboxSize / (double) frame.getWidth();
        double scaleY = spriteScale * hitboxSize / (double) frame.getHeight();

        AffineTransform transform = new AffineTransform();
        double centerX = x + hitboxSize / 2.0;
        double centerY = y + hitboxSize / 2.0;
        transform.translate(centerX, centerY);
        if (facingLeft) {
            transform.scale(-scaleX, scaleY);
        } else {
            transform.scale(scaleX, scaleY);
        }
        transform.translate(-frame.getWidth() / 2.0, -frame.getHeight() / 2.0);

        g2d.drawImage(frame, transform, null);
    }
    // ---------- Getter พื้นฐาน ----------
    public int getX() { return x; }
    public int getY() { return y; }
    public int getCenterX() { return x + size / 2; }
    public int getCenterY() { return y + size / 2; }
    public int getSize() { return size; }
    public int getSpeed() { return speed; }
    public int getPanelWidth() { return panelWidth; }
    public int getPanelHeight() { return panelHeight; }
}