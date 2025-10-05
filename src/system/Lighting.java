package system;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * จัดการเอฟเฟกต์แสง/ความมืดของเกมให้อยู่แยกจาก GamePanel
 */
public final class Lighting {

    private Lighting() {
    }

    /**
     * โครงสร้างเก็บข้อมูลจุดกำเนิดแสงแต่ละจุด
     */
    public static class LightSource {
        private final int x;
        private final int y;
        private final int radius;

        public LightSource(int x, int y, int radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getRadius() {
            return radius;
        }
    }

    /** สร้างภาพเลเยอร์ความมืดแล้วเจาะรูจากแหล่งกำเนิดแสงทุกจุด */
    public static BufferedImage createLightingMask(int width, int height, List<LightSource> lights) {
        BufferedImage darkness = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gDark = darkness.createGraphics();

        gDark.setComposite(AlphaComposite.Src);
        gDark.setColor(new Color(0, 0, 0, 230));
        gDark.fillRect(0, 0, width, height);

        for (LightSource light : lights) {
            punchLight(gDark, light);
        }

        gDark.dispose();
        return darkness;
    }

    /** ตรวจว่าจุดหนึ่ง ๆ อยู่ในรัศมีแสงใดหรือไม่ */
    public static boolean isPointLit(int px, int py, List<LightSource> lights) {
        for (LightSource light : lights) {
            int dx = px - light.getX();
            int dy = py - light.getY();
            int radius = light.getRadius();
            if (dx * dx + dy * dy <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private static void punchLight(Graphics2D gDark, LightSource light) {
        Paint oldPaint = gDark.getPaint();
        Composite oldComposite = gDark.getComposite();

        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(light.getX(), light.getY()),
                light.getRadius(),
                new float[] { 0f, 1f },
                new Color[] { new Color(1f, 1f, 1f, 1f), new Color(1f, 1f, 1f, 0f) }
        );

        gDark.setComposite(AlphaComposite.DstOut);
        gDark.setPaint(paint);
        int radius = light.getRadius();
        gDark.fillOval(light.getX() - radius, light.getY() - radius, radius * 2, radius * 2);

        gDark.setPaint(oldPaint);
        gDark.setComposite(oldComposite);
    }
}