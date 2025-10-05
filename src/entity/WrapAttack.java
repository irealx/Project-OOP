package entity;

import system.Level;

// REFACTOR: กลยุทธ์การโจมตีแต่ละแบบถูกแยกออกจากกันเพื่อให้เพิ่ม/แก้ได้ง่ายและไม่ซ้ำโค้ด
class WrapAttack implements Monster.AttackBehavior {

    @Override
    public void attack(Monster self, Player player, Level level) {
        if (player == null) {
            return;
        }
        self.follow(player.getX(), player.getY());
    }

    @Override
    public void afterUpdate(Monster self) {
        self.wrap();
    }
}