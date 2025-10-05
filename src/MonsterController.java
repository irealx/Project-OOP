import java.util.Random;
/**
 * คลาสควบคุมการทำงานของมอนสเตอร์แต่ละตัว
 */
public class MonsterController {
    private final Monster monster;
    private final boolean[] activeLevels;
    private boolean active;

    public MonsterController(Monster monster, int[] activeLevelIndices, int totalLevels) {
        this.monster = monster;
        this.activeLevels = new boolean[Math.max(0, totalLevels)];
        if (activeLevelIndices != null) {
            for (int idx : activeLevelIndices) {
                if (idx >= 0 && idx < this.activeLevels.length) {
                    this.activeLevels[idx] = true;
                }
            }
        }
    }

    public void prepareForLevel(int levelIndex, Random random, int panelWidth, int panelHeight) {
        active = levelIndex >= 0 && levelIndex < activeLevels.length && activeLevels[levelIndex];
        if (active) {
            monster.spawn(random, panelWidth, panelHeight);
            if (monster instanceof StunMonster) {
                ((StunMonster) monster).triggerStun();
            }
        }
    }

    public void update(int playerX, int playerY, int panelWidth, int panelHeight) {
        if (!active) {
            return;
        }
        monster.update(playerX, playerY, panelWidth, panelHeight);
    }

    public boolean isActive() {
        return active;
    }

    public Monster getMonster() {
        return monster;
    }
}
