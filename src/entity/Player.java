package entity;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import system.Config;

/**
 * Player — ตัวละครผู้เล่นหลักของเกม
 * จัดการการเคลื่อนไหว, สถานะ (Idle / Run / Death / Stun),
 * การควบคุมจากคีย์บอร์ด และการวาด sprite animation
 */
public class Player extends Sprite {

    // สถานะหลักของผู้เล่น
    private enum State { IDLE, RUN, DEATH }

    // อินพุตที่รองรับ (ทิศทาง)
    private static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;

    // เก็บ animation frame ของแต่ละสถานะ
    private final BufferedImage[][] frames = new BufferedImage[State.values().length][];

    private State state = State.IDLE;
    private boolean facingLeft;     // true ถ้าหันซ้าย
    private boolean dead;           // true ถ้าผู้เล่นตายแล้ว
    private boolean deathFinished;  // true เมื่ออนิเมชันตายจบ
    private boolean stunned;        // true เมื่อโดน stun
    private int stunTick;           // ตัวนับระยะเวลา stun
    private int frameIndex;         // index เฟรมปัจจุบันของอนิเมชัน
    private int frameTimer;         // ตัวนับดีเลย์ระหว่างเฟรม
    private final boolean[] input = new boolean[4]; // การกดปุ่มทิศทาง

    public Player() {
        super(Config.PLAYER_SIZE, Config.PLAYER_SPEED);
        setFrame(null, Config.PLAYER_FALLBACK);
        loadAnimations();
        spawn();
    }

    // รีเซ็ตสถานะผู้เล่นเมื่อเริ่มด่านใหม่
    public void spawn() {
        center();
        state = State.IDLE;
        facingLeft = dead = deathFinished = stunned = false;
        stunTick = frameIndex = frameTimer = 0;
        Arrays.fill(input, false);
    }

    // รับการกดปุ่มจากคีย์บอร์ด
    public void handleKeyPressed(int code) {
        if (!dead) {
            setKeyState(code, true);
        }
    }

    // รับการปล่อยปุ่ม
    public void handleKeyReleased(int code) {
        setKeyState(code, false);
    }

    // อัปเดตสถานะการกดปุ่มในแต่ละทิศทาง
    private void setKeyState(int code, boolean pressed) {
        int index = switch (code) {
            case java.awt.event.KeyEvent.VK_LEFT, java.awt.event.KeyEvent.VK_A -> LEFT;
            case java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.KeyEvent.VK_D -> RIGHT;
            case java.awt.event.KeyEvent.VK_UP, java.awt.event.KeyEvent.VK_W -> UP;
            case java.awt.event.KeyEvent.VK_DOWN, java.awt.event.KeyEvent.VK_S -> DOWN;
            default -> -1;
        };
        if (index >= 0) {
            input[index] = pressed;
            if (pressed && (index == LEFT || index == RIGHT)) {
                facingLeft = index == LEFT; // กำหนดทิศทางการหัน
            }
        }
    }

    // อัปเดตการเคลื่อนไหวและสถานะอนิเมชัน
    public void update() {
        if (dead) { // ถ้าตายแล้วเล่นอนิเมชันตายต่อจนจบ
            advanceAnimation();
            return;
        }
        if (stunned) { // ถ้าโดน stun จะไม่ขยับ
            if (--stunTick <= 0) stunned = false;
            return;
        }

        // คำนวณความเร็วจากปุ่มที่กด
        int vx = (input[RIGHT] ? speed : 0) - (input[LEFT] ? speed : 0);
        int vy = (input[DOWN] ? speed : 0) - (input[UP] ? speed : 0);
        setVelocity(vx, vy);

        updateBase(); // อัปเดตตำแหน่งพื้นฐานจาก Sprite
        clamp(); // ป้องกันออกนอกขอบจอ

        state = (vx == 0 && vy == 0) ? State.IDLE : State.RUN;
        advanceAnimation();
    }

    // วาดผู้เล่นตามทิศทางและเฟรมปัจจุบัน
    public void draw(Graphics2D g) {
        BufferedImage frame = currentFrame();
        if (frame == null) {
            drawBase(g);
            return;
        }

        AffineTransform old = g.getTransform();
        int drawWidth = Math.max(1, (int) Math.round(size * Config.PLAYER_SPRITE_SCALE));
        int drawHeight = drawWidth;
        int offsetX = (size - drawWidth) / 2;
        int offsetY = (size - drawHeight) / 2;

        g.translate(x + offsetX, y + offsetY);
        if (facingLeft) {
            g.scale(-1, 1);
            g.drawImage(frame, -drawWidth, 0, drawWidth, drawHeight, null);
        } else {
            g.drawImage(frame, 0, 0, drawWidth, drawHeight, null);
        }
        g.setTransform(old);
    }

    // ทำให้ผู้เล่นอยู่ในสถานะ Stun ชั่วคราว
    public void applyStun(int duration) {
        stunned = true;
        stunTick = duration;
    }

    public boolean isStunned() {
        return stunned;
    }

    // หยุดการเคลื่อนไหวทันที (ใช้ตอนชนประตู)
    public void stopImmediately() {
        Arrays.fill(input, false);
        if (!dead) {
            state = State.IDLE;
            frameIndex = frameTimer = 0;
        }
    }

    // เมื่อผู้เล่นตาย → เปลี่ยนสถานะและเริ่มอนิเมชันตาย
    public void die() {
        if (dead) return;
        dead = true;
        state = State.DEATH;
        frameIndex = frameTimer = 0;
    }

    public boolean isDead() {
        return dead;
    }

    public boolean isDeathAnimationFinished() {
        return deathFinished;
    }

    // เปลี่ยนเฟรมอนิเมชันตามเวลาและสถานะ
    private void advanceAnimation() {
        BufferedImage[] current = frames[state.ordinal()];
        if (current == null || current.length == 0) return;

        if (++frameTimer >= 6) {
            frameTimer = 0;
            if (dead) {
                if (frameIndex < current.length - 1) frameIndex++;
                else deathFinished = true; // เมื่อเล่นจนครบ → flag ว่าตายเสร็จ
            } else {
                frameIndex = (frameIndex + 1) % current.length;
            }
        }
    }

    // ดึงเฟรมปัจจุบันของสถานะที่กำลังเล่น
    private BufferedImage currentFrame() {
        BufferedImage[] current = frames[state.ordinal()];
        if (current == null || current.length == 0) return null;
        return current[Math.min(frameIndex, current.length - 1)];
    }

    // โหลดภาพอนิเมชันของแต่ละสถานะจากโฟลเดอร์
    private void loadAnimations() {
        frames[State.IDLE.ordinal()] = loadFrames("Idle", "IDLE", 4);
        frames[State.RUN.ordinal()] = loadFrames("Run", "RUN", 8);
        frames[State.DEATH.ordinal()] = loadFrames("Death", "DEATH", 8);
    }

    // โหลดภาพต่อเนื่องจากโฟลเดอร์ที่กำหนด
    private BufferedImage[] loadFrames(String folder, String action, int count) {
        BufferedImage[] imgs = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            String path = String.format("Pic/character/MC/%s/FASE 2 %s%d.png", folder, action, i + 1);
            try {
                imgs[i] = ImageIO.read(new File(path));
            } catch (IOException ex) {
                imgs[i] = null;
            }
        }
        return imgs;
    }
}
