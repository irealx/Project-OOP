package entity;
import java.util.Random;

// คลาส abstract พื้นฐานของมอนสเตอร์ทุกประเภท
public abstract class Monster {
    private final int size; // ขนาดของมอนสเตอร์
    protected int x;        // ตำแหน่งแกน X
    protected int y;        // ตำแหน่งแกน Y

    // Constructor สำหรับกำหนดขนาดของมอนสเตอร์
    protected Monster(int size) {
        this.size = size;
    }

    // Getter สำหรับตำแหน่ง X
    public int getX() { return x; }

    // Getter สำหรับตำแหน่ง Y
    public int getY() { return y; }

    // Getter สำหรับขนาดมอนสเตอร์
    public int getSize() { return size; }

    // ฟังก์ชันสุ่มเกิดมอนสเตอร์ที่มุมของหน้าจอ
    public void spawn(Random random, int panelWidth, int panelHeight) {
        int[][] corners = new int[][] {
            {10, 10},
            {panelWidth - size - 10, 10},
            {10, panelHeight - size - 10},
            {panelWidth - size - 10, panelHeight - size - 10}
        };

        // เลือกมุมแบบสุ่ม
        int[] corner = corners[random.nextInt(corners.length)];
        x = corner[0];
        y = corner[1];
    }

    // ฟังก์ชัน abstract ที่ subclass ต้อง override เพื่ออัปเดตพฤติกรรม
    public abstract void update(int playerX, int playerY, int panelWidth, int panelHeight);

    // เคลื่อนที่เข้าหาผู้เล่นด้วยความเร็ว speed
    protected void moveTowardPlayer(int playerX, int playerY, int speed) {
        if (playerX < x) {
            x -= speed;
        } else if (playerX > x) {
            x += speed;
        }

        if (playerY < y) {
            y -= speed;
        } else if (playerY > y) {
            y += speed;
        }
    }

    // ป้องกันไม่ให้มอนสเตอร์หลุดขอบจอ
    protected void clampToBounds(int panelWidth, int panelHeight) {
        x = Math.max(0, Math.min(panelWidth - size, x));
        y = Math.max(0, Math.min(panelHeight - size, y));
    }

    // ตรวจสอบการชนกับวัตถุอื่น (เช่น ผู้เล่น)
    public boolean intersects(int otherX, int otherY, int otherSize) {
        return x < otherX + otherSize &&
               x + size > otherX &&
               y < otherY + otherSize &&
               y + size > otherY;
    }
}