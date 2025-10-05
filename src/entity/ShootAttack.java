package entity;

import java.util.WeakHashMap;
import system.Config;
import system.Level;

/**
 * ShootAttack — พฤติกรรมของมอนสเตอร์ประเภท "ยิง" หรือ "พุ่งเข้าใส่"
 * ใช้ WeakHashMap เก็บสถานะเฉพาะของมอนสเตอร์แต่ละตัว เช่น ตัวนับเวลา dash
 */
class ShootAttack implements Monster.AttackBehavior {

    // เก็บข้อมูลเพิ่มเติมของมอนสเตอร์แต่ละตัว เช่น counter สำหรับหน่วงเวลา dash
    private static class Data {
        int counter;
    }

    // ใช้ WeakHashMap เพื่อให้ข้อมูลถูกลบอัตโนมัติเมื่อมอนสเตอร์ถูกเก็บจากหน่วยความจำ
    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    @Override
    public void reset(Monster self) {
        // รีเซ็ตตัวนับเวลาการ dash เมื่อเริ่มเลเวลใหม่
        get(self).counter = Config.SHOOT_DASH_INTERVAL;
    }

    @Override
    public void attack(Monster self, Player player, Level level) {
        if (player == null) return;
        Data data = get(self);

        // เมื่อ counter หมด → dash เข้าใส่ผู้เล่นด้วยความเร็วสูง
        if (data.counter <= 0) {
            self.follow(player.getX(), player.getY(),
                        self.getSpeed() * Config.SHOOT_DASH_MULTIPLIER);
            data.counter = Config.SHOOT_DASH_INTERVAL; // รีเซ็ตตัวนับใหม่
        } else {
            // ระหว่างรอ dash จะเคลื่อนที่ปกติ
            data.counter--;
            self.follow(player.getX(), player.getY());
        }
    }

    @Override
    public void afterUpdate(Monster self) {
        // ป้องกันมอนสเตอร์ออกนอกขอบจอ
        self.clamp();
    }

    // คืนค่า Data ของมอนสเตอร์แต่ละตัว ถ้ายังไม่มีจะสร้างใหม่
    private Data get(Monster self) {
        return states.computeIfAbsent(self, s -> new Data());
    }
}
