import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Puzzle {
    private static final int TOTAL_PUZZLE_IMAGES = 9; // จำนวนไฟล์ภาพ Puzzle ทั้งหมด

    private final BufferedImage[] puzzleImages; // เก็บภาพ Puzzle ที่โหลดมาแล้ว
    private BufferedImage activePuzzleImage;    // ภาพ Puzzle ที่กำลังแสดงอยู่
    private Integer activePuzzleNumber;         // หมายเลขภาพปัจจุบัน (ใช้ตรวจสอบซ้ำ)

    public Puzzle() {
        puzzleImages = loadPuzzleImages();
        clearActivePuzzle();
    }

    /**
     * โหลดภาพ Puzzle ทั้งหมดจากโฟลเดอร์ Pic/character/puzzle
     */
    private BufferedImage[] loadPuzzleImages() {
        BufferedImage[] images = new BufferedImage[TOTAL_PUZZLE_IMAGES];
        for (int i = 1; i <= TOTAL_PUZZLE_IMAGES; i++) {
            try {
                images[i - 1] = ImageIO.read(new File(String.format("Pic/character/puzzle/pz%d.png", i)));
            } catch (IOException e) {
                System.err.println("ไม่สามารถโหลดภาพ puzzle หมายเลข " + i + ": " + e.getMessage());
                images[i - 1] = null; // ถ้าโหลดไม่ได้ให้เก็บค่า null ไว้ก่อน
            }
        }
        return images;
    }

    /**
     * ตั้งค่าภาพ Puzzle ที่ต้องการแสดง โดยรับหมายเลขจากประตู
     */
    public void showPuzzle(Integer number) {
        if (number != null && number >= 1 && number <= puzzleImages.length) {
            activePuzzleImage = puzzleImages[number - 1];
            activePuzzleNumber = number;
        } else {
            clearActivePuzzle();
        }
    }

    /**
     * ล้างสถานะภาพ Puzzle เพื่อซ่อนหน้าต่างปริศนา
     */
    public void clearActivePuzzle() {
        activePuzzleImage = null;
        activePuzzleNumber = null;
    }

    /**
     * วาดหน้าต่าง Overlay ของ Puzzle ตรงกลางหน้าจอ
     */
    public void drawOverlay(Graphics2D g2d, int panelWidth, int panelHeight) {
        // พื้นหลังโปร่งแสงเพื่อเน้นภาพ Puzzle
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        if (activePuzzleImage == null) {
            return; // ถ้าไม่มีภาพให้แสดงก็ไม่ต้องวาดต่อ
        }

        int imgWidth = activePuzzleImage.getWidth();
        int imgHeight = activePuzzleImage.getHeight();

        // คำนวณสเกลไม่ให้ภาพใหญ่เกินหน้าจอ พร้อมรักษาอัตราส่วนเดิม
        double scale = Math.min((panelWidth - 120) / (double) imgWidth,
                                (panelHeight - 160) / (double) imgHeight);
        scale = Math.min(1.0, Math.max(0.1, scale));

        int drawWidth = (int) Math.round(imgWidth * scale);
        int drawHeight = (int) Math.round(imgHeight * scale);
        int drawX = (panelWidth - drawWidth) / 2;
        int drawY = (panelHeight - drawHeight) / 2 - 20;

        g2d.drawImage(activePuzzleImage, drawX, drawY, drawWidth, drawHeight, null);
    }

    /**
     * ใช้สำหรับดีบั๊กหรือขยายผลในอนาคต หากต้องรู้ว่ากำลังเปิดภาพหมายเลขใด
     */
    public Integer getActivePuzzleNumber() {
        return activePuzzleNumber;
    }
}
