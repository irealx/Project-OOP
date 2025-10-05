package system;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

// REFACTOR: รวมค่าคงที่ไว้ที่เดียวเพื่อแก้ไขง่ายและป้องกันค่าซ้ำซ้อนในหลายไฟล์
public class Puzzle {
    private static final int TOTAL = 9;
    private final BufferedImage[] images = new BufferedImage[TOTAL];
    private BufferedImage active;
    public Puzzle() {
        loadImages();
    }
    public void show(Integer number) {
        if (number == null || number < 1 || number > TOTAL) {
            active = null;
            return;
        }
        active = images[number - 1];
    }
    public void clear() {
        active = null;
    }
    public void draw(Graphics2D g, int width, int height) {
        if (active == null) {
            return;
        }
        var oldClip = g.getClip();
        g.setClip(0, 0, width, height);
        EffectRenderer.drawPuzzleOverlay(g, active, Config.PUZZLE_SHOW_ALPHA / 255f);
        g.setClip(oldClip);
    }
    private void loadImages() {
        for (int i = 1; i <= TOTAL; i++) {
            try {
                images[i - 1] = ImageIO.read(new File(String.format("Pic/character/puzzle/pz%d.png", i)));
            } catch (IOException ex) {
                images[i - 1] = null;
            }
        }
    }
}
