package entity;
// มอนสเตอร์ที่วาร์ปทะลุขอบจอได้
public class WrapMonster extends Monster {
    private final int speed; // ความเร็วคงที่ของมอนสเตอร์

    // Constructor สำหรับกำหนดขนาดและความเร็ว
    public WrapMonster(int size, int speed) {
        super(size);
        this.speed = speed;
    }

    // อัปเดตตำแหน่งมอนสเตอร์แต่ละเฟรม
    @Override
    public void update(int playerX, int playerY, int panelWidth, int panelHeight) {
        // เคลื่อนที่เข้าหาผู้เล่น
        moveTowardPlayer(playerX, playerY, speed);
        // วาร์ปกลับเมื่อออกนอกขอบจอ
        wrapWithinBounds(panelWidth, panelHeight);
    }

    // ฟังก์ชันย้ายตำแหน่งกลับอีกด้านถ้าออกนอกจอ
    private void wrapWithinBounds(int panelWidth, int panelHeight) {
        int size = getSize();

        // ถ้าออกซ้าย → ไปขวา
        if (x < 0) {
            x = panelWidth - size;
        }
        // ถ้าออกขวา → ไปซ้าย
        else if (x > panelWidth - size) {
            x = 0;
        }

        // ถ้าออกบน → ไปล่าง
        if (y < 0) {
            y = panelHeight - size;
        }
        // ถ้าออกล่าง → ไปบน
        else if (y > panelHeight - size) {
            y = 0;
        }
    }
}