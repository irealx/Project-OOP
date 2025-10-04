import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel implements ActionListener {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private static final int PLAYER_SIZE = 32;
    private static final int MONSTER_SIZE = 32;
    private static final int DOOR_SIZE = 48;
    private static final int PLAYER_SPEED = 4;
    private static final int MONSTER_SPEED = 2;
    static final int TOTAL_LEVELS = 6;
    private static final int DOORS_PER_LEVEL = 6;

    private final Timer timer;
    private final Random random = new Random();

    private final Player player = new Player();
    private final List<MonsterController> monsters = new ArrayList<>();
    private final List<Level> levels = new ArrayList<>();

    private int currentLevelIndex = 0;

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);

        initMonsters();
        initLevels();
        resetForLevel(0);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                player.handleKeyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.handleKeyReleased(e.getKeyCode());
            }
        });

        timer = new Timer(16, this);
        timer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    private void initMonsters() {
        monsters.add(new MonsterController(new StunMonster(MONSTER_SIZE, MONSTER_SPEED), new int[] { 0, 3 }));
        monsters.add(new MonsterController(new WrapMonster(MONSTER_SIZE, MONSTER_SPEED), new int[] { 1, 4 }));
        monsters.add(new MonsterController(new ShootingMonster(MONSTER_SIZE, MONSTER_SPEED + 1), new int[] { 2, 5 }));
    }

    private void initLevels() {
        for (int i = 0; i < TOTAL_LEVELS; i++) {
            levels.add(new Level());
        }
    }

    private void resetForLevel(int levelIndex) {
        currentLevelIndex = levelIndex;
        Level level = levels.get(levelIndex);
        level.randomizeDoors(random);
        player.spawn();
        for (MonsterController controller : monsters) {
            controller.prepareForLevel(levelIndex, random, WIDTH, HEIGHT);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawLevelInfo(g2d);
        drawDoors(g2d);
        drawPlayer(g2d);
        drawMonsters(g2d);
    }

    private void drawLevelInfo(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("Level " + (currentLevelIndex + 1), 20, 30);
    }

    private void drawDoors(Graphics2D g2d) {
        for (Door door : levels.get(currentLevelIndex).doors) {
            switch (door.type) {
                case ADVANCE:
                    g2d.setColor(new Color(0x3CB371));
                    break;
                case BACK:
                    g2d.setColor(new Color(0xCD5C5C));
                    break;
                default:
                    g2d.setColor(new Color(0x4682B4));
            }
            g2d.fillRect(door.x, door.y, DOOR_SIZE, DOOR_SIZE);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        g2d.setColor(new Color(0xFFD700));
        g2d.fillRect(player.x, player.y, PLAYER_SIZE, PLAYER_SIZE);
    }

    private void drawMonsters(Graphics2D g2d) {
        g2d.setColor(new Color(0xEE82EE));
        for (MonsterController controller : monsters) {
            if (controller.isActive()) {
                Monster monster = controller.getMonster();
                g2d.fillRect(monster.getX(), monster.getY(), monster.getSize(), monster.getSize());
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        player.update();
        for (MonsterController controller : monsters) {
            controller.update(player.x, player.y, WIDTH, HEIGHT);
        }

        checkDoorCollisions();
        checkMonsterCollisions();

        repaint();
    }

    private void checkDoorCollisions() {
        Level level = levels.get(currentLevelIndex);
        for (Door door : level.doors) {
            if (player.intersects(door.x, door.y, DOOR_SIZE, DOOR_SIZE)) {
                switch (door.type) {
                    case ADVANCE:
                        int nextLevel = (currentLevelIndex + 1) % TOTAL_LEVELS;
                        resetForLevel(nextLevel);
                        break;
                    case BACK:
                        int prevLevel = (currentLevelIndex - 1 + TOTAL_LEVELS) % TOTAL_LEVELS;
                        resetForLevel(prevLevel);
                        break;
                    default:
                        // nothing happens
                        break;
                }
                break;
            }
        }
    }

    private void checkMonsterCollisions() {
        for (MonsterController controller : monsters) {
            if (controller.isActive()) {
                Monster monster = controller.getMonster();
                if (player.intersects(monster.getX(), monster.getY(), monster.getSize(), monster.getSize())) {
                    resetForLevel(0);
                    break;
                }
            }
        }
    }

    private enum DoorType {
        NEUTRAL,
        ADVANCE,
        BACK
    }

    private static class Door {
        private final DoorType type;
        private final int x;
        private final int y;

        private Door(DoorType type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    private static class Level {
        private List<Door> doors = new ArrayList<>();

        private Level() {
        }

        private void randomizeDoors(Random random) {
            List<DoorType> types = new ArrayList<>();
            types.add(DoorType.ADVANCE);
            types.add(DoorType.BACK);
            while (types.size() < DOORS_PER_LEVEL) {
                types.add(DoorType.NEUTRAL);
            }
            Collections.shuffle(types, random);

            List<Point> points = generateDistinctDoorPositions(random);
            doors = new ArrayList<>();
            for (int i = 0; i < DOORS_PER_LEVEL; i++) {
                Point p = points.get(i);
                doors.add(new Door(types.get(i), p.x, p.y));
            }
        }

        private List<Point> generateDistinctDoorPositions(Random random) {
            List<Point> points = new ArrayList<>();
            int attempts = 0;
            while (points.size() < DOORS_PER_LEVEL && attempts < 10_000) {
                attempts++;
                int x = 50 + random.nextInt(WIDTH - 100 - DOOR_SIZE);
                int y = 80 + random.nextInt(HEIGHT - 150 - DOOR_SIZE);
                Point candidate = new Point(x, y);
                boolean overlaps = false;
                for (Point existing : points) {
                    if (existing.distanceSquared(candidate) < (DOOR_SIZE + 10) * (DOOR_SIZE + 10)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    points.add(candidate);
                }
            }
            while (points.size() < DOORS_PER_LEVEL) {
                points.add(new Point(60 * points.size(), 100));
            }
            return points;
        }
    }

    private static class Player {
        private int x;
        private int y;
        private boolean leftPressed;
        private boolean rightPressed;
        private boolean upPressed;
        private boolean downPressed;

        private void spawn() {
            x = WIDTH / 2 - PLAYER_SIZE / 2;
            y = HEIGHT / 2 - PLAYER_SIZE / 2;
            leftPressed = rightPressed = upPressed = downPressed = false;
        }

        private void handleKeyPressed(int keyCode) {
            if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
                leftPressed = true;
            } else if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
                rightPressed = true;
            } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                upPressed = true;
            } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                downPressed = true;
            }
        }

        private void handleKeyReleased(int keyCode) {
            if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
                leftPressed = false;
            }
            if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
                rightPressed = false;
            }
            if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                upPressed = false;
            }
            if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                downPressed = false;
            }
        }

        private void update() {
            int vx = 0;
            int vy = 0;
            if (leftPressed && !rightPressed) {
                vx = -PLAYER_SPEED;
            } else if (rightPressed && !leftPressed) {
                vx = PLAYER_SPEED;
            }
            if (upPressed && !downPressed) {
                vy = -PLAYER_SPEED;
            } else if (downPressed && !upPressed) {
                vy = PLAYER_SPEED;
            }
            x += vx;
            y += vy;
            x = Math.max(0, Math.min(WIDTH - PLAYER_SIZE, x));
            y = Math.max(0, Math.min(HEIGHT - PLAYER_SIZE, y));
        }

        private boolean intersects(int otherX, int otherY, int w, int h) {
            return x < otherX + w && x + PLAYER_SIZE > otherX && y < otherY + h && y + PLAYER_SIZE > otherY;
        }
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
    
    private static class MonsterController {
        private final Monster monster;
        private final boolean[] activeLevels;
        private boolean active;

        private MonsterController(Monster monster, int[] activeLevelIndices) {
            this.monster = monster;
            this.activeLevels = new boolean[TOTAL_LEVELS];
            for (int idx : activeLevelIndices) {
                if (idx >= 0 && idx < TOTAL_LEVELS) {
                    this.activeLevels[idx] = true;
                }
            }
        }

        private void prepareForLevel(int levelIndex, Random random, int panelWidth, int panelHeight) {
            active = levelIndex >= 0 && levelIndex < activeLevels.length && activeLevels[levelIndex];
            if (active) {
                monster.spawn(random, panelWidth, panelHeight);
            }
        }

        private void update(int playerX, int playerY, int panelWidth, int panelHeight) {
            if (!active) {
                return;
            }
            monster.update(playerX, playerY, panelWidth, panelHeight);
        }

        private boolean isActive() {
            return active;
        }

        private Monster getMonster() {
            return monster;
        }
    }
}