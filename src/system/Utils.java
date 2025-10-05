package system;

// REFACTOR: เก็บฟังก์ชันช่วยเหลือที่ใช้บ่อยในเกม เช่น การสุ่มหรือคำนวณระยะทาง
public final class Utils {
    private Utils() {}
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    public static int distanceSquared(int ax, int ay, int bx, int by) {
        int dx = ax - bx;
        int dy = ay - by;
        return dx * dx + dy * dy;
    }
    public static boolean withinBounds(int value, int min, int max) {
        return value >= min && value <= max;
    }
}