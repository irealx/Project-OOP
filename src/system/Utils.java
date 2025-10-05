package system;

// รวมฟังก์ชันช่วยเหลือทั่วไปที่ใช้บ่อยในเกม เช่น การจำกัดค่าและคำนวณระยะทาง
public final class Utils {

    private Utils() {} // utility class ไม่ให้สร้างอ็อบเจกต์

    // จำกัดค่า value ให้อยู่ระหว่าง min และ max (เวอร์ชัน int)
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // จำกัดค่า value ให้อยู่ระหว่าง min และ max (เวอร์ชัน float)
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // คำนวณระยะห่างระหว่างสองจุดแบบกำลังสอง (ไม่ถอดรูทเพื่อประหยัดเวลา)
    public static int distanceSquared(int ax, int ay, int bx, int by) {
        int dx = ax - bx;
        int dy = ay - by;
        return dx * dx + dy * dy;
    }

    // ตรวจว่าค่าอยู่ภายในช่วงที่กำหนดหรือไม่
    public static boolean withinBounds(int value, int min, int max) {
        return value >= min && value <= max;
    }
}
