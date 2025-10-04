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
    // ตัวแปรสถานะสตัน
    private boolean isStunned = false;   // กำลังติดสตันอยู่ไหม
    private int stunTick = 0;            // ตัวจับเวลาสตัน (หน่วยเป็นเฟรม)

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

        // เคลียร์สถานะสตันเผื่อว่ามีการรีเซ็ตด่านใหม่
        isStunned = false;
        stunTick = 0;
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
     * หยุดการเคลื่อนไหวทันที ใช้ตอนชนประตูเพื่อให้ผู้เล่นและมอนสเตอร์หยุดนิ่ง
     */
    public void stopImmediately() {
        leftPressed = rightPressed = upPressed = downPressed = false;
        if (state != State.DEATH) {
            changeState(State.IDLE);
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
        if (isStunned) {
            if (stunTick > 0) {
                stunTick--;
            } else {
                isStunned = false; // หมดเวลาสตัน กลับมาเคลื่อนไหวได้
            }
            return; //  ออกจาก update ทันที ไม่อัปเดตการเคลื่อนที่
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

        // ปรับขนาด sprite ที่แสดงผล (เช่น 1.5 เท่า)
        double spriteScale = 12; // ปรับค่านี้เพื่อเพิ่ม/ลดขนาด sprite
        double scaleX = spriteScale * size / frame.getWidth();
        double scaleY = spriteScale * size / frame.getHeight();

        // สร้าง AffineTransform เพื่อจัดตำแหน่ง sprite ให้อยู่กลาง object player
        AffineTransform transform = new AffineTransform();
        int centerX = x + size / 2;
        int centerY = y + size / 2;

        if (facingLeft) {
            // กลับ sprite ทางซ้าย และจัดให้อยู่กลาง object player
            transform.translate(centerX, centerY);
            transform.scale(-scaleX, scaleY); // scaleX ติดลบเพื่อกลับด้าน
            transform.translate(-frame.getWidth() / 2, -frame.getHeight() / 2);
        } else {
            // sprite ปกติและอยู่กลาง object player
            transform.translate(centerX, centerY);
            transform.scale(scaleX, scaleY);
            transform.translate(-frame.getWidth() / 2, -frame.getHeight() / 2);
        }
        g2d.drawImage(frame, transform, null);
    }

    /**
     * ฟังก์ชันใช้เมื่อตัวผู้เล่นโดนวงสตัน
     * @param duration จำนวนเฟรมที่ผู้เล่นจะหยุดขยับ (เช่น 60 = ประมาณ 1 วินาที)
     */
    public void applyStun(int duration) {
        isStunned = true;
        stunTick = duration;
    }

    /** คืนค่าผู้เล่นกำลังโดนสตันอยู่ไหม */
    public boolean isStunned() {
        return isStunned;
    }

    /**
     * ฟังก์ชันตรวจการชนกับวัตถุอื่น
     */
    public boolean intersects(int otherX, int otherY, int w, int h) {
        return x < otherX + w && x + size > otherX && y < otherY + h && y + size > otherY;
    }

    /**
     * ดันผู้เล่นให้ออกนอกพื้นที่สี่เหลี่ยมที่ระบุ (เช่น หลังยกเลิกตรวจประตู)
     * @param rectX จุดเริ่มต้นแกน X ของพื้นที่ที่ต้องออกมา
     * @param rectY จุดเริ่มต้นแกน Y ของพื้นที่ที่ต้องออกมา
     * @param rectWidth ความกว้างของพื้นที่
     * @param rectHeight ความสูงของพื้นที่
     */
    public void pushOutsideRect(int rectX, int rectY, int rectWidth, int rectHeight) {
        if (!intersects(rectX, rectY, rectWidth, rectHeight)) {
            return; // ถ้าไม่ได้ชนอยู่ก็ไม่ต้องขยับ
        }

        int playerRight = x + size;
        int playerBottom = y + size;
        int rectRight = rectX + rectWidth;
        int rectBottom = rectY + rectHeight;

        int overlapLeft = playerRight - rectX;
        int overlapRight = rectRight - x;
        int overlapTop = playerBottom - rectY;
        int overlapBottom = rectBottom - y;

        int minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapLeft) {
            x = rectX - size - 1;
        } else if (minOverlap == overlapRight) {
            x = rectRight + 1;
        } else if (minOverlap == overlapTop) {
            y = rectY - size - 1;
        } else {
            y = rectBottom + 1;
        }

        x = Math.max(0, Math.min(panelWidth - size, x));
        y = Math.max(0, Math.min(panelHeight - size, y));
    }

    /**
     * ดันผู้เล่นออกจากสี่เหลี่ยมจัตุรัส (เช่น ประตูที่กว้างเท่ากันทุกด้าน)
     */
    public void pushOutsideSquare(int rectX, int rectY, int rectSize) {
        pushOutsideRect(rectX, rectY, rectSize, rectSize);
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
