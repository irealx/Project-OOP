// มอนสเตอร์พื้นฐานที่แค่เคลื่อนเข้าหาผู้เล่น
public class StunMonster extends Monster {
    private final int speed; // ความเร็วคงที่ของมอนสเตอร์

    // Constructor กำหนดขนาดและความเร็ว
    public StunMonster(int size, int speed) {
        super(size);
        this.speed = speed;
    }

    // อัปเดตตำแหน่งมอนสเตอร์แต่ละเฟรม
    @Override
    public void update(int playerX, int playerY, int panelWidth, int panelHeight) {
        // เดินเข้าหาผู้เล่นด้วยความเร็วคงที่
        moveTowardPlayer(playerX, playerY, speed);
        // ป้องกันไม่ให้ออกนอกขอบจอ
        clampToBounds(panelWidth, panelHeight);
    }
}