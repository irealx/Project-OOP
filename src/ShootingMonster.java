// มอนสเตอร์ที่สามารถ "พุ่งเร็ว" ได้เป็นช่วง ๆ
public class ShootingMonster extends Monster {
    private final int baseSpeed;       // ความเร็วพื้นฐาน
    private final int dashMultiplier;  // ค่าคูณความเร็วเมื่อพุ่ง
    private final int dashInterval;    // จำนวนเฟรมก่อนพุ่งครั้งถัดไป
    private int framesUntilDash;       // ตัวนับเฟรมก่อนพุ่ง

    // Constructor เริ่มต้น (ใช้ค่า dashMultiplier=3, dashInterval=45)
    public ShootingMonster(int size, int speed) {
        this(size, speed, 3, 45);
    }

    // Constructor ที่กำหนดค่าพิเศษเองได้
    public ShootingMonster(int size, int speed, int dashMultiplier, int dashInterval) {
        super(size);
        this.baseSpeed = speed;
        this.dashMultiplier = Math.max(1, dashMultiplier);
        this.dashInterval = Math.max(1, dashInterval);
        this.framesUntilDash = this.dashInterval;
    }

    // ฟังก์ชันอัปเดตตำแหน่งมอนสเตอร์แต่ละเฟรม
    @Override
    public void update(int playerX, int playerY, int panelWidth, int panelHeight) {
        // เมื่อครบระยะ dash ให้พุ่งด้วยความเร็วคูณ
        if (framesUntilDash <= 0) {
            moveTowardPlayer(playerX, playerY, baseSpeed * dashMultiplier);
            framesUntilDash = dashInterval; // รีเซ็ตตัวนับเฟรม
        } else {
            // เคลื่อนที่ปกติ
            moveTowardPlayer(playerX, playerY, baseSpeed);
            framesUntilDash--;
        }

        // ไม่ให้หลุดขอบจอ
        clampToBounds(panelWidth, panelHeight);
    }
}