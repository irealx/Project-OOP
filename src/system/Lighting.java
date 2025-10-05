package system;

import entity.Monster;
import entity.Player;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// รวมการจัดการระบบแสงของเกม เช่น การสร้าง mask เงาและแหล่งกำเนิดแสง
public final class Lighting {

    // ใช้เก็บข้อมูลของแหล่งกำเนิดแสง (ตำแหน่งและรัศมี)
    public record LightSource(int x, int y, int radius) {}

    private Lighting() {} // utility class ไม่ให้สร้างอ็อบเจกต์

    // สร้างภาพ mask ของแสงทั้งหมดบนฉาก
    public static BufferedImage createMask(int width, int height, List<LightSource> lights) {
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();

        // วาดพื้นมืดก่อน แล้วค่อยวาดแสงทับ
        EffectRenderer.drawShadow(g, 0, 0, width, height, 0.92f);
        for (LightSource light : lights) {
            EffectRenderer.drawLight(g, light.x(), light.y(), light.radius(), 1f);
        }

        g.dispose();
        return mask;
    }

    // ตรวจว่าจุดที่กำหนดอยู่ในบริเวณที่มีแสงหรือไม่
    public static boolean isPointLit(int px, int py, List<LightSource> lights) {
        for (LightSource light : lights) {
            // ใช้ระยะกำลังสองเพื่อลดการใช้ sqrt()
            if (Utils.distanceSquared(px, py, light.x(), light.y()) <= light.radius() * light.radius()) {
                return true;
            }
        }
        return false;
    }

    // รวมแหล่งกำเนิดแสงจากผู้เล่นและมอนสเตอร์ทั้งหมด
    public static List<LightSource> collect(Player player, List<Monster> monsters) {
        List<LightSource> lights = new ArrayList<>();

        // แสงจากผู้เล่น
        lights.add(new LightSource(
                player.getCenterX(),
                player.getCenterY(),
                Config.PLAYER_LIGHT_RADIUS
        ));

        // แสงจากมอนสเตอร์ที่ยัง active
        for (Monster monster : monsters) {
            if (monster.isActive()) {
                lights.add(new LightSource(
                        monster.getCenterX(),
                        monster.getCenterY(),
                        Config.MONSTER_LIGHT[monster.getAttackType().ordinal()]
                ));
            }
        }

        return lights;
    }
}
