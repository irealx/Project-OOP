import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * คลาส Sprite รวมฟังก์ชันวาดสไปรต์ให้ใช้งานร่วมกัน
 * - แยกตรรกะการวาดออกจากคลาส Player เพื่อให้จัดการได้ง่าย
 * - รองรับการวาดสไปรต์ที่ขยายขนาดและกลับด้านซ้าย/ขวา
 */
public class Sprite {

    /** ไม่ให้สร้างอินสแตนซ์ เพราะใช้เป็นคลาสเครื่องมือเท่านั้น */
    private Sprite() {
    }

    /**
     * ฟังก์ชันวาดสไปรต์ของผู้เล่นให้อยู่กลาง hitbox และขยายขนาดตามต้องการ
     *
     * @param g2d          อ็อบเจกต์ Graphics2D ที่ใช้วาด
     * @param frame        ภาพเฟรมปัจจุบัน (อาจเป็น null)
     * @param x            ตำแหน่งซ้ายบนของ hitbox ผู้เล่น (แกน X)
     * @param y            ตำแหน่งซ้ายบนของ hitbox ผู้เล่น (แกน Y)
     * @param hitboxSize   ขนาด hitbox ที่ใช้สำหรับการชน
     * @param facingLeft   ผู้เล่นหันซ้ายอยู่หรือไม่ (true = หันซ้าย)
     * @param fallbackColor สีสำรองที่ใช้ถ้าโหลดภาพไม่ได้
     */
    public static void drawPlayer(Graphics2D g2d, BufferedImage frame, int x, int y,
                                  int hitboxSize, boolean facingLeft, Color fallbackColor) {
        if (frame == null) {
            // ถ้าไม่มีภาพให้ใช้สี่เหลี่ยมสีแทนเพื่อไม่ให้วัตถุหาย
            g2d.setColor(fallbackColor);
            g2d.fillRect(x, y, hitboxSize, hitboxSize);
            return;
        }

        double spriteScale = 12; // อัตราส่วนขยายของสไปรต์จาก hitbox เดิม
        double scaleX = spriteScale * hitboxSize / frame.getWidth();
        double scaleY = spriteScale * hitboxSize / frame.getHeight();

        // จัดให้สไปรต์อยู่กลาง hitbox และกลับด้านได้เมื่อหันซ้าย
        AffineTransform transform = new AffineTransform();
        int centerX = x + hitboxSize / 2;
        int centerY = y + hitboxSize / 2;

        transform.translate(centerX, centerY);
        if (facingLeft) {
            transform.scale(-scaleX, scaleY);
        } else {
            transform.scale(scaleX, scaleY);
        }
        transform.translate(-frame.getWidth() / 2.0, -frame.getHeight() / 2.0);

        g2d.drawImage(frame, transform, null);
    }
}
