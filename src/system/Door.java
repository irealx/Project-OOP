package system;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

// REFACTOR: รวมค่าคงที่ไว้ที่เดียวเพื่อแก้ไขง่ายและป้องกันค่าซ้ำซ้อนในหลายไฟล์
public class Door {
    public enum Type { PUZZLE, ADVANCE, BACK }
    private static final int FRAME_SIZE = 128;
    private static final int FRAMES = 8;
    private static final int FRAME_DELAY = 6;
    private static final BufferedImage[] SPRITES = loadSprites();
    private final Type type;
    private final double xRatio;
    private final double yRatio;
    private final int size;
    private int tick;
    private int frame;
    private Integer puzzleNumber;
    public Door(Type type, int x, int y, int panelWidth, int panelHeight, int size) {
        this.type = type;
        this.size = size;
        this.xRatio = Math.max(0, Math.min(1, x / (double) Math.max(1, panelWidth - size)));
        this.yRatio = Math.max(0, Math.min(1, y / (double) Math.max(1, panelHeight - size)));
    }
    public void setPuzzleNumber(Integer puzzleNumber) {
        this.puzzleNumber = puzzleNumber;
    }
    public Integer getPuzzleNumber() {
        return puzzleNumber;
    }
    public Type getType() {
        return type;
    }
    public void updateAnimation() {
        if (SPRITES.length == 0) {
            return;
        }
        if (++tick >= FRAME_DELAY) {
            tick = 0;
            frame = (frame + 1) % SPRITES.length;
        }
    }
    public void draw(Graphics2D g, int width, int height) {
        int drawX = getX(width);
        int drawY = getY(height);
        if (SPRITES.length > 0) {
            g.drawImage(SPRITES[frame], drawX, drawY, size, size, null);
        } else {
            g.setColor(switch (type) {
                case ADVANCE -> new Color(0x3CB371);
                case BACK -> new Color(0xCD5C5C);
                default -> new Color(0x4682B4);
            });
            g.fillRect(drawX, drawY, size, size);
        }
    }
    public int getX(int width) {
        int available = Math.max(0, width - size);
        return (int) Math.round(xRatio * available);
    }
    public int getY(int height) {
        int available = Math.max(0, height - size);
        return (int) Math.round(yRatio * available);
    }
    private static BufferedImage[] loadSprites() {
        try {
            BufferedImage sheet = ImageIO.read(new File("Pic/door.png"));
            int count = Math.min(FRAMES, sheet.getWidth() / FRAME_SIZE);
            BufferedImage[] frames = new BufferedImage[count];
            for (int i = 0; i < count; i++) {
                frames[i] = sheet.getSubimage(i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
            }
            return frames;
        } catch (IOException ex) {
            return new BufferedImage[0];
        }
    }
}