package system;

import entity.Monster;
import entity.Player;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// REFACTOR: ทำให้ลำดับการวาดภาพเป็นระบบเดียวกัน แก้ไขง่ายและลดความซับซ้อน
public final class Lighting {
    public record LightSource(int x, int y, int radius) {}
    private Lighting() {}
    public static BufferedImage createMask(int width, int height, List<LightSource> lights) {
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        EffectRenderer.drawShadow(g, 0, 0, width, height, 0.92f);
        for (LightSource light : lights) {
            EffectRenderer.drawLight(g, light.x(), light.y(), light.radius(), 1f);
        }
        g.dispose();
        return mask;
    }
    public static boolean isPointLit(int px, int py, List<LightSource> lights) {
        for (LightSource light : lights) {
            if (Utils.distanceSquared(px, py, light.x(), light.y()) <= light.radius() * light.radius()) {
                return true;
            }
        }
        return false;
    }
    public static List<LightSource> collect(Player player, List<Monster> monsters) {
        List<LightSource> lights = new ArrayList<>();
        lights.add(new LightSource(player.getCenterX(), player.getCenterY(), Config.PLAYER_LIGHT_RADIUS));
        for (Monster monster : monsters) {
            if (monster.isActive()) {
                lights.add(new LightSource(monster.getCenterX(), monster.getCenterY(), Config.MONSTER_LIGHT[monster.getAttackType().ordinal()]));
            }
        }
        return lights;
    }
}