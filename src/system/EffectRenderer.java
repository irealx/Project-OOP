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

// REFACTOR: รวมระบบวาดเอฟเฟกต์ทั้งหมดไว้ที่เดียว ป้องกันโค้ดซ้ำในการวาดภาพและการผสมสี
public final class EffectRenderer {
    private EffectRenderer() {}

    // REFACTOR: ใช้ระบบโปร่งแสงร่วมกันในทุกไฟล์ ลดการซ้ำของโค้ดกราฟิก
    public static void setAlpha(Graphics2D g, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Utils.clamp(alpha, 0f, 1f)));
    }
    public static void drawLight(Graphics2D g, int x, int y, int radius, float alpha) {
        Paint old = g.getPaint();
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT, Utils.clamp(alpha, 0f, 1f)));
        RadialGradientPaint paint = new RadialGradientPaint(
            new Point2D.Float(x, y),
            radius,
            new float[] { 0f, 1f },
            new Color[] { new Color(1f, 1f, 1f, 1f), new Color(1f, 1f, 1f, 0f) }
        );
        g.setPaint(paint);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setPaint(old);
        g.setComposite(oldComposite);
    }
    public static void drawShadow(Graphics2D g, int x, int y, int w, int h, float opacity) {
        Composite old = g.getComposite();
        setAlpha(g, opacity);
        g.setColor(Color.BLACK);
        g.fillRect(x, y, w, h);
        g.setComposite(old);
    }
    public static void drawPuzzleOverlay(Graphics2D g, BufferedImage img, float alpha) {
        if (img == null) {
            return;
        }
        var bounds = g.getClipBounds();
        int width = bounds != null ? bounds.width : img.getWidth();
        int height = bounds != null ? bounds.height : img.getHeight();
        drawShadow(g, 0, 0, width, height, alpha);
        double scale = Math.min((width - 120) / (double) img.getWidth(), (height - 160) / (double) img.getHeight());
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
    public static void drawGlow(Graphics2D g, Color color, int x, int y, int radius) {
        Composite oldComposite = g.getComposite();
        Paint oldPaint = g.getPaint();
        setAlpha(g, 0.4f);
        g.setPaint(new GradientPaint(x, y, color, x, y + radius, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)));
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setPaint(oldPaint);
        g.setComposite(oldComposite);
    }
}