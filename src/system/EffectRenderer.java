package system;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

// รวมระบบวาดเอฟเฟกต์ทั้งหมดไว้ที่เดียว ป้องกันโค้ดซ้ำในการวาดภาพและการผสมสี
public final class EffectRenderer {

    private EffectRenderer() {} // utility class ไม่ให้สร้างอ็อบเจกต์

    // ตั้งค่าความโปร่งแสงของการวาด
    public static void setAlpha(Graphics2D g, float alpha) {
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                Utils.clamp(alpha, 0f, 1f) // จำกัดค่าให้อยู่ในช่วง 0–1
        ));
    }

    // วาดแสงแบบรัศมี (ใช้ในระบบ Lighting)
    public static void drawLight(Graphics2D g, int x, int y, int radius, float alpha) {
        Paint old = g.getPaint();
        Composite oldComposite = g.getComposite();

        // ใช้ DST_OUT เพื่อเจาะเงาออกให้เห็นแสง
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.DST_OUT,
                Utils.clamp(alpha, 0f, 1f)
        ));

        // สร้างแสงจางจากจุดศูนย์กลางออกไปขอบ
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(x, y),
                radius,
                new float[]{0f, 1f},
                new Color[]{
                        new Color(1f, 1f, 1f, 1f),
                        new Color(1f, 1f, 1f, 0f)
                }
        );

        g.setPaint(paint);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setPaint(old);
        g.setComposite(oldComposite);
    }

    // วาดเงาทึบ (ใช้ทำพื้นมืดหรือครอบทั้งฉาก)
    public static void drawShadow(Graphics2D g, int x, int y, int w, int h, float opacity) {
        Composite old = g.getComposite();
        setAlpha(g, opacity);
        g.setColor(Color.BLACK);
        g.fillRect(x, y, w, h);
        g.setComposite(old);
    }

    // วาดภาพ Puzzle overlay พร้อมพื้นหลังมืดจาง ๆ
    public static void drawPuzzleOverlay(Graphics2D g, BufferedImage img, float alpha) {
        if (img == null) return;

        var bounds = g.getClipBounds();
        int width = bounds != null ? bounds.width : img.getWidth();
        int height = bounds != null ? bounds.height : img.getHeight();

        // วาดพื้นมืดก่อน
        drawShadow(g, 0, 0, width, height, alpha);

        // คำนวณขนาดให้พอดีหน้าจอโดยไม่บิดภาพ
        double scale = Math.min((width - 120) / (double) img.getWidth(),
                                (height - 160) / (double) img.getHeight());
        scale = Math.min(1.0, Math.max(0.1, scale));

        int drawW = (int) Math.round(img.getWidth() * scale);
        int drawH = (int) Math.round(img.getHeight() * scale);
        int drawX = (width - drawW) / 2;
        int drawY = (height - drawH) / 2 - 20;

        Composite old = g.getComposite();
        setAlpha(g, 1f);
        g.drawImage(img, drawX, drawY, drawW, drawH, null);
        g.setComposite(old);
    }

    // วาดเอฟเฟกต์เรืองแสงรอบวัตถุ (เช่นประตูหรือจุดพิเศษ)
    public static void drawGlow(Graphics2D g, Color color, int x, int y, int radius) {
        Composite oldComposite = g.getComposite();
        Paint oldPaint = g.getPaint();

        setAlpha(g, 0.4f); // ตั้งค่าโปร่งแสงเล็กน้อย
        g.setPaint(new GradientPaint(
                x, y, color,
                x, y + radius,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
        ));
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }
}
