package entity;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

// มอนสเตอร์พื้นฐานที่แค่เคลื่อนเข้าหาผู้เล่น
public class StunMonster extends Monster {
    private final int speed; // ความเร็วคงที่ของมอนสเตอร์
    private final int stunDurationTicks = 60;     // ระยะเวลาที่ผู้เล่นจะโดนสตัน (ประมาณ 1 วินาทีหากเฟรม ~60fps)
    private final int stunCooldownTicks = 180;    // คูลดาวน์ก่อนยิงวงครั้งต่อไป (~3 วินาที)
    private final int ringMaxRadius = 480;        // รัศมีสูงสุดของวงสตั้น
    private final int ringThickness = 10;            // ความหนาของวงสตั้น (ใช้คำนวณว่าผู้เล่นอยู่ในวงหรือไม่)
    private int stunTick = 0;                     // ตัวนับเวลาของวงสตั้นตอนกำลังทำงาน (นับถอยหลัง)
    private int cooldownTick = 0;                 // ตัวนับคูลดาวน์ (นับถอยหลัง)
    private boolean autoTrigger = true;          // ถ้าเป็น true จะปล่อยวงอัตโนมัติเมื่อคูลดาวน์หมด

    // Constructor กำหนดขนาดและความเร็ว
    public StunMonster(int size, int speed) {
        super(size);
        this.speed = speed;
    }

    @Override
    public void update(int playerX, int playerY, int panelWidth, int panelHeight) {
        // ถ้ามีวงสตันกำลังทำงาน → อยู่นิ่งและนับถอยหลัง
        if (stunTick > 0) {
            stunTick--;
            return; // ยังอยู่ในวงสตัน ไม่ต้องเดิน
        }

        // ถ้ายังอยู่ในช่วง "หยุดนิ่งก่อนปล่อยวง"
        if (cooldownTick > 0 && cooldownTick <= 60) { 
            cooldownTick--;
            // พอครบ 1 วิ → ปล่อยวงสตัน
            if (cooldownTick == 0 && autoTrigger) {
                triggerStun();
            }
            return; // ช่วงนี้ไม่เดิน
        }

        // ถ้ามีคูลดาวน์หลังยิงวง (รอประมาณ 3 วิ)
        if (cooldownTick > 60) {
            cooldownTick--;
            // หลัง stunTick หมด ให้กลับมาวิ่งตาม player ทันที ไม่หยุดนิ่ง
        }

        // เคลื่อนที่เข้าหาผู้เล่นตามปกติ (จะวิ่งทันทีหลัง stunTick หมด แม้จะอยู่ในช่วง cooldown)
        moveTowardPlayer(playerX, playerY, speed);
        clampToBounds(panelWidth, panelHeight); // บังคับไม่ให้ออกนอกขอบจอ

        // ถ้าไม่มีทั้งสตันและคูลดาวน์ → เริ่มจับเวลาหยุดนิ่งก่อนปล่อยวง
        if (stunTick == 0 && cooldownTick == 0 && autoTrigger) {
            cooldownTick = 60; // หยุดนิ่ง 1 วิ ก่อนยิงวง
        }
    }


    /**
     * เริ่มเอฟเฟกต์วงสตั้น หากยังไม่ติดคูลดาวน์และตอนนี้ไม่มีวงทำงานอยู่
     */
    public void triggerStun() {
        // ยิงได้ก็ต่อเมื่อไม่มีวงสตั้นกำลังทำงานอยู่ และคูลดาวน์หมดแล้ว
        if (stunTick == 0 && cooldownTick == 0) {
            stunTick = stunDurationTicks;   // เริ่มนับเวลาวงสตั้น
            cooldownTick = stunCooldownTicks; // ตั้งคูลดาวน์รอรอบถัดไป
        }
    }


    public void drawStun(Graphics2D g2d) {
    if (stunTick <= 0) return; // ถ้าไม่มีวงสตันทำงานอยู่ ให้ข้ามเลย

        // คำนวณความคืบหน้าของแอนิเมชัน (0.0 → 1.0)
        float progress = 1.0f - ((float) stunTick / (float) stunDurationTicks);

        // คำนวณรัศมีของวงจาก progress
        int minRadius = getSize() / 2 + 6; // เริ่มจากรอบ ๆ ตัวมอนสเตอร์
        int radius = (int) (minRadius + (ringMaxRadius - minRadius) * progress);

        // ความโปร่งใส (เริ่มชัดแล้วจางลง)
        float alpha = Math.max(0.0f, 0.6f * (1.0f - progress));

        // จุดศูนย์กลางวง
            int cx = getX() + getSize() / 2;
        int cy = getY() + getSize() / 2;

        // เก็บสถานะเดิมของ g2d
        var oldComposite = g2d.getComposite();
        var oldStroke = g2d.getStroke();
        var oldColor = g2d.getColor();

        // ตั้งค่าการวาดวง
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(new Color(100, 200, 255)); // สีฟ้าอ่อน
        g2d.setStroke(new BasicStroke(8)); // ความหนาเส้น

        // วาดวงกลมรอบตัวมอน
        int d = radius * 2;
        g2d.drawOval(cx - radius, cy - radius, d, d);

        // คืนค่าการตั้งค่าเดิม
        g2d.setComposite(oldComposite);
        g2d.setStroke(oldStroke);
        g2d.setColor(oldColor);
    }

    /**
     * ตรวจว่าผู้เล่นอยู่ภายในระยะของวงสตันที่มอนสเตอร์ปล่อยออกมาหรือไม่
     * ถ้าอยู่ในรัศมีของวง ให้คืนค่า true (ถือว่าโดนสตัน)
     * ถ้าอยู่นอกวงหรือไม่มีวงทำงานอยู่ ให้คืนค่า false
     *
     * @param playerX     พิกัดแกน X ของผู้เล่น (มุมซ้ายบน)
     * @param playerY     พิกัดแกน Y ของผู้เล่น (มุมซ้ายบน)
     * @param playerSize  ขนาดของสี่เหลี่ยมผู้เล่น (ใช้คำนวณจุดกึ่งกลาง)
     * @return true ถ้าผู้เล่นอยู่ในวงสตัน, false ถ้าไม่โดน
     */
    public boolean isPlayerInStun(int playerX, int playerY, int playerSize) {
        // ถ้าวงสตันยังไม่ทำงาน ไม่ต้องตรวจ
        if (stunTick <= 0) return false;

        // คำนวณความคืบหน้าของแอนิเมชันวง (0.0 → 1.0)
        // เมื่อ stunTick ลดจาก stunDurationTicks จนเหลือ 0 ค่า progress จะเพิ่มขึ้นเรื่อย ๆ
        float progress = 1.0f - ((float) stunTick / (float) stunDurationTicks);

        // คำนวณรัศมีปัจจุบันของวง จากรัศมีเริ่มต้นถึงรัศมีสูงสุด
        int minRadius = getSize() / 2 + 6; // เริ่มพ้นขอบมอนเล็กน้อย
        int radius = (int) (minRadius + (ringMaxRadius - minRadius) * progress);

        // หาจุดศูนย์กลางของผู้เล่น
        int px = playerX + playerSize / 2;
        int py = playerY + playerSize / 2;

        // หาจุดศูนย์กลางของมอนสเตอร์ (ต้นกำเนิดของวงสตัน)
        int cx = getX() + getSize() / 2;
        int cy = getY() + getSize() / 2;

        // คำนวณระยะห่างระหว่างศูนย์กลางผู้เล่นกับมอน (ใช้กำลังสองเพื่อลดการใช้ sqrt)
        int dx = px - cx;
        int dy = py - cy;
        int dist2 = dx * dx + dy * dy;

        // คำนวณขอบในและขอบนอกของวงสตัน เพื่อให้ถือว่าโดนเฉพาะบริเวณของวง
        int inner = Math.max(0, radius - ringThickness);
        int outer = radius + ringThickness;

        // คืนค่า true ถ้าผู้เล่นอยู่ในบริเวณของวง
        return dist2 >= inner * inner && dist2 <= outer * outer;
    }

    /** คืนค่าระยะเวลาสตันเป็นจำนวนเฟรม เพื่อให้ฝั่งผู้เล่นใช้งาน */
    public int getStunDurationTicks() {
        return stunDurationTicks;
    }
}