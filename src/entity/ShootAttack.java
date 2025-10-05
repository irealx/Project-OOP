package entity;

import java.util.WeakHashMap;
import system.Config;
import system.Level;

// REFACTOR: กลยุทธ์การโจมตีแต่ละแบบถูกแยกออกจากกันเพื่อให้เพิ่ม/แก้ได้ง่ายและไม่ซ้ำโค้ด
class ShootAttack implements Monster.AttackBehavior {
    private static class Data {
        int counter;
    }
    private final WeakHashMap<Monster, Data> states = new WeakHashMap<>();

    @Override
    public void reset(Monster self) {
        get(self).counter = Config.SHOOT_DASH_INTERVAL;
    }

    @Override
    public void attack(Monster self, Player player, Level level) {
        if (player == null) {
            return;
        }
        Data data = get(self);
        if (data.counter <= 0) {
            self.follow(player.getX(), player.getY(), self.getSpeed() * Config.SHOOT_DASH_MULTIPLIER);
            data.counter = Config.SHOOT_DASH_INTERVAL;
        } else {
            data.counter--;
            self.follow(player.getX(), player.getY());
        }
    }

    @Override
    public void afterUpdate(Monster self) {
        self.clamp();
    }
    private Data get(Monster self) {
        return states.computeIfAbsent(self, s -> new Data());
    }
}