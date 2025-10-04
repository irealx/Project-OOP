import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * คลาสควบคุมการแสดงผลและการเคลื่อนไหวของผู้เล่น
 * - เก็บข้อมูลตำแหน่ง การเคลื่อนที่ และแอนิเมชัน
 * - เพิ่มขนาด sprite ให้ใหญ่ขึ้น แต่ hitbox เท่าเดิม
 */
public class Player {

    // สถานะของผู้เล่น
    private enum State {
        IDLE,   // ยืนนิ่ง
        RUN,    // วิ่ง
        DEATH   // ตาย
    }

    private static final int FRAME_DELAY = 8; // จำนวนเฟรมก่อนเปลี่ยนภาพใหม่

    private final int size;         // ขนาด hitbox ของผู้เล่น (ไม่เปลี่ยน)
    private final int speed;        // ความเร็วเคลื่อนที่
    private int panelWidth;   // ความกว้างของฉาก
    private int panelHeight;  // ความสูงของฉาก
    private final Color fallbackColor = new Color(0xFFD700); // สีสำรองถ้าโหลดภาพไม่ได้

    // อาร์เรย์เก็บเฟรมของแต่ละแอ็กชัน
    private final BufferedImage[] idleFrames;
    private final BufferedImage[] runFrames;
    private final BufferedImage[] deathFrames;

    // ตำแหน่งและสถานะปุ่ม
    private int x, y;
    private boolean leftPressed, rightPressed, upPressed, downPressed;
    private boolean facingLeft; // true = หันซ้าย

    // ตัวแปรควบคุมสถานะและแอนิเมชัน
    private State state = State.IDLE;
    private int animationFrame;
    private int animationTick;
    private boolean deathAnimationFinished;

    /**
     * ฟังก์ชันสร้างผู้เล่น และโหลดภาพ Sprite ทั้งหมด
     */
    public Player(int size, int speed, int panelWidth, int panelHeight) {
        this.size = size;
        this.speed = speed;
        updateBounds(panelWidth, panelHeight);

        // โหลดภาพทั้งหมด
        this.idleFrames = loadFrames("Idle", "IDLE", 4);
        this.runFrames = loadFrames("Run", "RUN", 8);
        this.deathFrames = loadFrames("Death", "DEATH", 8);

        spawn(); // ตั้งค่าตำแหน่งเริ่มต้นกลางจอ
    }

    /**
     * ฟังก์ชัน spawn() — วางผู้เล่นที่กลางจอใหม่
     */
    public final void spawn() {
        x = panelWidth / 2 - size / 2;
        y = panelHeight / 2 - size / 2;
        leftPressed = rightPressed = upPressed = downPressed = false;
        facingLeft = false;
        changeState(State.IDLE);
    }


    /**
     * ปรับขนาดขอบเขตการเคลื่อนที่ของผู้เล่นเมื่อพื้นที่เกมเปลี่ยนไป
     */
    public void updateBounds(int panelWidth, int panelHeight) {
        this.panelWidth = Math.max(size, panelWidth);
        this.panelHeight = Math.max(size, panelHeight);
        x = Math.max(0, Math.min(this.panelWidth - size, x));
        y = Math.max(0, Math.min(this.panelHeight - size, y));
    }

    /**
     * ฟังก์ชันรับการกดปุ่มจากคีย์บอร์ด
     */
    public void handleKeyPressed(int keyCode) {
        if (state == State.DEATH) return;
        switch (keyCode) {
            case java.awt.event.KeyEvent.VK_LEFT:
            case java.awt.event.KeyEvent.VK_A:
                leftPressed = true;
                facingLeft = true;
                break;
            case java.awt.event.KeyEvent.VK_RIGHT:
            case java.awt.event.KeyEvent.VK_D:
                rightPressed = true;
                facingLeft = false;
                break;
            case java.awt.event.KeyEvent.VK_UP:
            case java.awt.event.KeyEvent.VK_W:
                upPressed = true;
                break;
            case java.awt.event.KeyEvent.VK_DOWN:
            case java.awt.event.KeyEvent.VK_S:
                downPressed = true;
                break;
        }
    }

    /**
     * ฟังก์ชันรับการปล่อยปุ่ม
     */
    public void handleKeyReleased(int keyCode) {
        switch (keyCode) {
            case java.awt.event.KeyEvent.VK_LEFT:
            case java.awt.event.KeyEvent.VK_A:
                leftPressed = false;
                break;
            case java.awt.event.KeyEvent.VK_RIGHT:
            case java.awt.event.KeyEvent.VK_D:
                rightPressed = false;
                break;
            case java.awt.event.KeyEvent.VK_UP:
            case java.awt.event.KeyEvent.VK_W:
                upPressed = false;
                break;
            case java.awt.event.KeyEvent.VK_DOWN:
            case java.awt.event.KeyEvent.VK_S:
                downPressed = false;
                break;
        }
    }

    /**
     * ฟังก์ชัน update() — อัปเดตตำแหน่งและแอนิเมชันของผู้เล่นในแต่ละเฟรม
     */
    public void update() {
        if (state == State.DEATH) { // ถ้าตาย ให้เล่นแอนิเมชันตายต่อ
            advanceAnimation();
            return;
        }

        int vx = 0, vy = 0; // ความเร็วแนวนอนและแนวตั้ง

        if (leftPressed && !rightPressed) vx = -speed;
        else if (rightPressed && !leftPressed) vx = speed;

        if (upPressed && !downPressed) vy = -speed;
        else if (downPressed && !upPressed) vy = speed;

        // อัปเดตตำแหน่ง
        x += vx;
        y += vy;

        // จำกัดไม่ให้ออกนอกขอบจอ
        x = Math.max(0, Math.min(panelWidth - size, x));
        y = Math.max(0, Math.min(panelHeight - size, y));

        // เปลี่ยนสถานะระหว่างยืนนิ่งกับวิ่ง
        if (vx != 0 || vy != 0) changeState(State.RUN);
        else changeState(State.IDLE);

        advanceAnimation(); // เปลี่ยนเฟรมภาพตามเวลา
    }

    /**
     * ฟังก์ชัน draw() — วาด sprite ของผู้เล่น
     * (เพิ่มขนาดการวาดให้ใหญ่ขึ้นจาก hitbox โดยไม่เปลี่ยนการชน)
     */
    public void draw(Graphics2D g2d) {
        BufferedImage frame = getCurrentFrame();
        if (frame == null) {
            g2d.setColor(fallbackColor);
            g2d.fillRect(x, y, size, size); // วาดสี่เหลี่ยมสีเหลืองแทนถ้าไม่มีภาพ
            return;
        }

        // --- เพิ่มบรรทัดนี้ ---
        double spriteScale = 12; // ปรับขนาด sprite ให้ใหญ่ขึ้น 1.8 เท่า (สามารถปรับได้)

        AffineTransform transform = new AffineTransform();

        // คำนวณอัตราส่วนการขยาย (hitbox = ขนาดจริงในเกม)
        double scaleX = (double) size * spriteScale / frame.getWidth();
        double scaleY = (double) size * spriteScale / frame.getHeight();

        // เพื่อให้ sprite อยู่ตรงกลาง hitbox ต้องเลื่อนพิกัดก่อนวาด
        int offsetX = (int) ((frame.getWidth() * scaleX - size) / 2);
        int offsetY = (int) ((frame.getHeight() * scaleY - size) / 2);

        if (facingLeft) {
            transform.translate(x + size - offsetX, y - offsetY);
            transform.scale(-scaleX, scaleY);
        } else {
            transform.translate(x - offsetX, y - offsetY);
            transform.scale(scaleX, scaleY);
        }

        g2d.drawImage(frame, transform, null);
    }

    /**
     * ฟังก์ชันตรวจการชนกับวัตถุอื่น
     */
    public boolean intersects(int otherX, int otherY, int w, int h) {
        return x < otherX + w && x + size > otherX && y < otherY + h && y + size > otherY;
    }

    /**
     * ฟังก์ชันเมื่อผู้เล่นตาย เริ่มเล่นแอนิเมชัน Death
     */
    public void die() {
        if (state == State.DEATH) return;
        changeState(State.DEATH);
        deathAnimationFinished = false;
        leftPressed = rightPressed = upPressed = downPressed = false;
    }

    /**
     * ฟังก์ชันเช็กว่าผู้เล่นตายอยู่หรือไม่
     */
    public boolean isDead() {
        return state == State.DEATH;
    }

    /**
     * ฟังก์ชันเช็กว่าแอนิเมชันตายเล่นจบหรือยัง
     */
    public boolean isDeathAnimationFinished() {
        return deathAnimationFinished;
    }

    // Getter คืนค่าตำแหน่งต่าง ๆ
    public int getX() { return x; }
    public int getY() { return y; }
    public int getCenterX() { return x + size / 2; }
    public int getCenterY() { return y + size / 2; }
    public int getSize() { return size; }

    /**
     * ฟังก์ชันเปลี่ยนสถานะของผู้เล่น (Idle / Run / Death)
     */
    private void changeState(State newState) {
        if (state != newState) {
            state = newState;
            animationFrame = 0;
            animationTick = 0;
            if (state != State.DEATH) deathAnimationFinished = false;
        }
    }

    /**
     * ฟังก์ชันเลื่อนเฟรมแอนิเมชันไปข้างหน้า
     */
    private void advanceAnimation() {
        BufferedImage[] frames = getCurrentFrames();
        if (frames.length == 0) return;

        animationTick++;
        if (animationTick < FRAME_DELAY) return;
        animationTick = 0;

        if (state == State.DEATH) {
            if (animationFrame < frames.length - 1) animationFrame++;
            else deathAnimationFinished = true;
        } else {
            animationFrame = (animationFrame + 1) % frames.length;
        }
    }

    /**
     * ฟังก์ชันคืนภาพเฟรมปัจจุบันของสถานะนั้น
     */
    private BufferedImage getCurrentFrame() {
        BufferedImage[] frames = getCurrentFrames();
        if (frames.length == 0) return null;
        return frames[Math.min(animationFrame, frames.length - 1)];
    }

    /**
     * ฟังก์ชันคืนอาร์เรย์เฟรมของสถานะปัจจุบัน
     */
    private BufferedImage[] getCurrentFrames() {
        switch (state) {
            case RUN: return runFrames;
            case DEATH: return deathFrames;
            case IDLE:
            default: return idleFrames;
        }
    }

    /**
     * ฟังก์ชันโหลดภาพแอนิเมชันแต่ละชุดจากโฟลเดอร์
     */
    private BufferedImage[] loadFrames(String folder, String action, int count) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            String path = String.format("Pic/character/MC/%s/FASE 2 %s%d.png", folder, action, i + 1);
            try {
                frames[i] = ImageIO.read(new File(path));
            } catch (IOException e) {
                System.err.println("ไม่สามารถโหลดภาพของผู้เล่นได้: " + path + " - " + e.getMessage());
                frames[i] = null;
            }
        }
        return frames;
    }
}
