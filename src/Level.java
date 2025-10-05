import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * ตัวจัดการข้อมูลของด่านแต่ละด่าน
 * รับผิดชอบเฉพาะรายการประตู การสุ่มตำแหน่ง/ประเภท การสร้างรหัสผ่าน และการรีเซ็ตด่าน
 */
public class Level {
    private final int doorCount;
    private final int doorSize;

    private List<Door> doors = new ArrayList<>();
    private int passwordCode = 0;

    public Level(int doorCount, int doorSize) {
        this.doorCount = doorCount;
        this.doorSize = doorSize;
    }

    /**
     * รีเซ็ตด่านใหม่ โดยจะสุ่มประตูทั้งหมดและสร้างรหัสผ่านใหม่
     */
    public void reset(Random random, int panelWidth, int panelHeight) {
        randomizeDoors(random, panelWidth, panelHeight);
    }

    public List<Door> getDoors() {
        return Collections.unmodifiableList(doors);
    }

    public int getPasswordCode() {
        return passwordCode;
    }

    public void updateDoorAnimations() {
        for (Door door : doors) {
            door.updateAnimation();
        }
    }

    private void randomizeDoors(Random random, int panelWidth, int panelHeight) {
        List<Door.Type> types = new ArrayList<>();
        types.add(Door.Type.ADVANCE);
        types.add(Door.Type.BACK);
        while (types.size() < doorCount) {
            types.add(Door.Type.PUZZLE);
        }
        Collections.shuffle(types, random);

        // สุ่มตำแหน่งของประตูโดยพยายามไม่ให้ทับกัน
        List<Point> points = generateDistinctDoorPositions(random, panelWidth, panelHeight);
        doors = new ArrayList<>();
        passwordCode = 0;

        // เตรียมตัวเลขไว้สำหรับประตูปริศนา โดยเลือกมาสูงสุด 4 ตัวเลข
        List<Integer> puzzlePool = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            puzzlePool.add(i);
        }
        Collections.shuffle(puzzlePool, random);
        List<Integer> selectedNumbers = new ArrayList<>(puzzlePool.subList(0, Math.min(4, puzzlePool.size())));
        int sum = 0;
        for (int value : selectedNumbers) {
            sum += value;
        }
        passwordCode = sum;
        Collections.shuffle(selectedNumbers, random);
        int puzzleIndex = 0;

        for (int i = 0; i < doorCount; i++) {
            Point p = points.get(i);
            Door door = new Door(types.get(i), p.x, p.y, panelWidth, panelHeight, doorSize);
            if (door.getType() == Door.Type.PUZZLE && puzzleIndex < selectedNumbers.size()) {
                door.setPuzzleNumber(selectedNumbers.get(puzzleIndex++));
            }
            doors.add(door);
        }
    }

    private List<Point> generateDistinctDoorPositions(Random random, int panelWidth, int panelHeight) {
        List<Point> points = new ArrayList<>();
        int attempts = 0;

        while (points.size() < doorCount && attempts < 10_000) {
            attempts++;
            int xRange = Math.max(1, panelWidth - 100 - doorSize);
            int yRange = Math.max(1, panelHeight - 150 - doorSize);

            int x = 50 + random.nextInt(xRange);
            int y = 80 + random.nextInt(yRange);
            Point candidate = new Point(x, y);

            boolean overlaps = false;
            for (Point existing : points) {
                if (existing.distanceSquared(candidate) < (doorSize + 10) * (doorSize + 10)) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                points.add(candidate);
            }
        }

        while (points.size() < doorCount) {
            // ถ้าพยายามสุ่มไม่สำเร็จก็ใช้ตำแหน่งสำรองเพื่อไม่ให้จำนวนประตูขาด
            int fallbackX = Math.min(panelWidth - doorSize, 60 * points.size());
            int fallbackY = Math.min(panelHeight - doorSize, 100 + 40 * points.size());
            points.add(new Point(Math.max(0, fallbackX), Math.max(0, fallbackY)));
        }
        return points;
    }

    private static class Point {
        private final int x;
        private final int y;

        private Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private int distanceSquared(Point other) {
            int dx = x - other.x;
            int dy = y - other.y;
            return dx * dx + dy * dy;
        }
    }
}
