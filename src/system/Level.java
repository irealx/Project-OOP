package system;

import entity.Sprite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// จัดการการสร้างเลเวล ประตู และรหัสผ่านในแต่ละด่าน
public class Level {

    private final int doorSize;
    private final int doorCount;
    private final Random random = new Random();
    private final List<Door> doors = new ArrayList<>();

    private int password;
    private int width;
    private int height;

    public Level(int doorCount, int doorSize) {
        this.doorCount = doorCount;
        this.doorSize = doorSize;
    }

    // รีเซ็ตด่านใหม่ (ใช้ทุกครั้งที่เริ่มเลเวล)
    public void reset(int width, int height) {
        this.width = width;
        this.height = height;
        doors.clear();
        password = 0;

        // สุ่มเลขจาก pool สำหรับใช้เป็นรหัส (รวมกันเป็นคำตอบ)
        List<Integer> pool = new ArrayList<>(Config.PUZZLE_POOL);
        Collections.shuffle(pool, random);
        List<Integer> selected = pool.subList(0, Math.min(4, pool.size()));
        for (int v : selected) password += v;
        Collections.shuffle(selected, random);

        // สร้างรายการประเภทของประตู (1 ไปต่อ, 1 ย้อนกลับ, ที่เหลือเป็น Puzzle)
        List<Door.Type> types = new ArrayList<>();
        types.add(Door.Type.ADVANCE);
        types.add(Door.Type.BACK);
        while (types.size() < doorCount) types.add(Door.Type.PUZZLE);
        Collections.shuffle(types, random);

        // คำนวณตำแหน่งสุ่มของประตูแต่ละบาน
        List<int[]> spots = buildDoorPositions();

        // สร้างประตูและกำหนดหมายเลข Puzzle
        int puzzleIndex = 0;
        for (int i = 0; i < types.size(); i++) {
            Door.Type type = types.get(i);
            int[] p = spots.get(i);
            Door door = new Door(type, p[0], p[1], width, height, doorSize);

            if (type == Door.Type.PUZZLE && puzzleIndex < selected.size()) {
                door.setPuzzleNumber(selected.get(puzzleIndex++));
            }
            doors.add(door);
        }
    }

    public List<Door> getDoors() {
        return doors;
    }

    // ตรวจการชนของ Sprite กับประตู
    public DoorHit detectDoorCollision(Sprite sprite) {
        for (Door door : doors) {
            if (sprite.intersects(door.getX(width), door.getY(height), doorSize, doorSize)) {
                return new DoorHit(door);
            }
        }
        return null;
    }

    // ตรวจว่ารหัสที่กรอกตรงกับคำตอบหรือไม่
    public boolean validatePassword(String input) {
        if (input == null || input.isBlank()) return false;
        if (input.trim().equals(String.format("%02d", password))) return true;

        try {
            return Integer.parseInt(input.trim()) == password;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    // ดันตัวละครให้ออกจากบริเวณประตูหลังชน
    public void pushAway(Sprite sprite, Door door) {
        if (door == null) return;
        sprite.pushOutside(door.getX(width), door.getY(height), doorSize, doorSize);
    }

    // สุ่มตำแหน่งประตู โดยตรวจไม่ให้ซ้อนกัน
    private List<int[]> buildDoorPositions() {
        List<int[]> points = new ArrayList<>();
        int attempts = 0;

        // พยายามสุ่มตำแหน่งจนกว่าจะครบจำนวนหรือครบจำนวนครั้งสูงสุด
        while (points.size() < doorCount && attempts++ < 5000) {
            int px = 50 + random.nextInt(Math.max(1, width - 100 - doorSize));
            int py = 80 + random.nextInt(Math.max(1, height - 150 - doorSize));

            // ตรวจระยะห่างระหว่างประตู ไม่ให้วางใกล้กันเกินไป
            if (points.stream().noneMatch(p ->
                    Utils.distanceSquared(px, py, p[0], p[1]) < (doorSize + 10) * (doorSize + 10))) {
                points.add(new int[]{px, py});
            }
        }

        // ถ้าสุ่มไม่ครบ ให้เรียงตำแหน่งแบบ fallback
        while (points.size() < doorCount) {
            int fx = Utils.clamp(60 * points.size(), 0, Math.max(0, width - doorSize));
            int fy = Utils.clamp(100 + 40 * points.size(), 0, Math.max(0, height - doorSize));
            points.add(new int[]{fx, fy});
        }

        return points;
    }

    // ใช้แทนการคืนค่าประตูที่ชนได้พร้อมข้อมูลเพิ่มเติม
    public record DoorHit(Door door) {
        public Door.Type type() { return door.getType(); }
        public int puzzleNumber() { return door.getPuzzleNumber(); }
    }
}
