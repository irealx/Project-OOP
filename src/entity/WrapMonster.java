package entity;

// มอนสเตอร์ที่วาร์ปทะลุขอบจอได้
public class WrapMonster extends Monster {

    // Constructor สำหรับกำหนดขนาดและความเร็ว
    public WrapMonster(int size, int speed) {
        super(size, speed);
    }

    // อัปเดตตำแหน่งมอนสเตอร์แต่ละเฟรม
    @Override
    protected void behave(int playerX, int playerY) {
        // เคลื่อนที่เข้าหาผู้เล่น
        moveToward(playerX, playerY, getSpeed());
        // วาร์ปกลับเมื่อออกนอกขอบจอ
        wrapWithinBounds();
    }

    // ฟังก์ชันย้ายตำแหน่งกลับอีกด้านถ้าออกนอกจอ
    private void wrapWithinBounds() {
        int size = getSize();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        // ถ้าออกซ้าย → ไปขวา
        if (x < 0) {
            x = panelWidth - size;
        }
        // ถ้าออกขวา → ไปซ้าย
        else if (x > panelWidth - size) {
            x = 0;
        }

        // ถ้าออกบน → ไปล่าง
        if (y < 0) {
            y = panelHeight - size;
        }
        // ถ้าออกล่าง → ไปบน
        else if (y > panelHeight - size) {
            y = 0;
        }
    }
}