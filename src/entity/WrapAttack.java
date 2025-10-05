package entity;

import system.Level;

/**
 * WrapAttack — พฤติกรรมของมอนสเตอร์ประเภท "วาร์ป"
 * ไล่ตามผู้เล่นแบบปกติ แต่จะวาร์ปไปอีกด้านของจอเมื่อหลุดขอบ
 */
class WrapAttack implements Monster.AttackBehavior {

    /**
     * ติดตามผู้เล่นโดยตรงด้วยความเร็วพื้นฐานของมอนสเตอร์
     */
    @Override
    public void attack(Monster self, Player player, Level level) {
        if (player == null) return;
        self.follow(player.getX(), player.getY());
    }

    /**
     * หลังอัปเดตตำแหน่ง → ถ้าออกนอกขอบจอให้วาร์ปไปอีกฝั่ง
     */
    @Override
    public void afterUpdate(Monster self) {
        self.wrap();
    }
}
