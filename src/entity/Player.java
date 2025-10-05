package entity;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import system.Config;

// REFACTOR: ลดบรรทัดที่ไม่จำเป็น ทำให้โค้ดอ่านง่ายและดูแลรักษาง่ายขึ้น
public class Player extends Sprite {
    private enum State { IDLE, RUN, DEATH }
    private static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;
    private final BufferedImage[][] frames = new BufferedImage[State.values().length][];
    private State state = State.IDLE;
    private boolean facingLeft;
    private boolean dead;
    private boolean deathFinished;
    private boolean stunned;
    private int stunTick;
    private int frameIndex;
    private int frameTimer;
    private final boolean[] input = new boolean[4];
    public Player() {
        super(Config.PLAYER_SIZE, Config.PLAYER_SPEED);
        setFrame(null, Config.PLAYER_FALLBACK);
        loadAnimations();
        spawn();
    }
    public void spawn() {
        center();
        state = State.IDLE;
        facingLeft = dead = deathFinished = stunned = false;
        stunTick = frameIndex = frameTimer = 0;
        Arrays.fill(input, false);
    }
    public void handleKeyPressed(int code) {
        if (!dead) {
            setKeyState(code, true);
        }
    }
    public void handleKeyReleased(int code) {
        setKeyState(code, false);
    }
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
                facingLeft = index == LEFT;
            }
        }
    }

    public void update() {
        if (dead) {
            advanceAnimation();
            return;
        }
        if (stunned) {
            if (--stunTick <= 0) {
                stunned = false;
            }
            return;
        }
        int vx = (input[RIGHT] ? speed : 0) - (input[LEFT] ? speed : 0);
        int vy = (input[DOWN] ? speed : 0) - (input[UP] ? speed : 0);
        setVelocity(vx, vy);
        updateBase();
        clamp();
        state = (vx == 0 && vy == 0) ? State.IDLE : State.RUN;
        advanceAnimation();
    }
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

    public void applyStun(int duration) {
        stunned = true;
        stunTick = duration;
    }

    public boolean isStunned() {
        return stunned;
    }
    public void stopImmediately() {
        Arrays.fill(input, false);
        if (!dead) {
            state = State.IDLE;
            frameIndex = frameTimer = 0;
        }
    }

    public void die() {
        if (dead) {
            return;
        }
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

    private void advanceAnimation() {
        BufferedImage[] current = frames[state.ordinal()];
        if (current == null || current.length == 0) {
            return;
        }
        if (++frameTimer >= 6) {
            frameTimer = 0;
            if (dead) {
                if (frameIndex < current.length - 1) {
                    frameIndex++;
                } else {
                    deathFinished = true;
                }
            } else {
                frameIndex = (frameIndex + 1) % current.length;
            }
        }
    }

    private BufferedImage currentFrame() {
        BufferedImage[] current = frames[state.ordinal()];
        if (current == null || current.length == 0) {
            return null;
        }
        return current[Math.min(frameIndex, current.length - 1)];
    }
    private void loadAnimations() {
        frames[State.IDLE.ordinal()] = loadFrames("Idle", "IDLE", 4);
        frames[State.RUN.ordinal()] = loadFrames("Run", "RUN", 8);
        frames[State.DEATH.ordinal()] = loadFrames("Death", "DEATH", 8);
    }

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