import java.awt.Dimension;
import java.awt.Toolkit;           // ใช้สำหรับดึงขนาดหน้าจอจริง
import java.awt.event.KeyAdapter; // ใช้ตรวจจับปุ่มกด
import java.awt.event.KeyEvent;   // ใช้รหัสปุ่ม (เช่น F11)
import javax.swing.JFrame;

/**
 * คลาส GameFrame: สร้างหน้าต่างหลักของเกม
 * - จัดการขนาดหน้าต่าง
 * - เพิ่มระบบกด F11 เพื่อสลับโหมด Fullscreen ↔ Window
 */
public class GameFrame extends JFrame {

    private boolean fullscreen = false; // ตัวแปรบอกสถานะว่าอยู่โหมดเต็มจอไหม
    private final Dimension windowedSize = new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT); // ขนาดโหมดปกติ

    // Constructor ของ GameFrame (สร้างหน้าต่างเกม)
    public GameFrame() {
        super("Six Door Maze"); // ตั้งชื่อหน้าต่างเกม

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อกดปุ่ม X
        setResizable(false); // ปิดการปรับขนาดหน้าต่าง

        // สร้าง panel หลักของเกม (จอที่แสดงภาพและอัปเดตเกม)
        GamePanel panel = new GamePanel();
        panel.setPreferredSize(windowedSize); // ตั้งขนาดเริ่มต้นของหน้าต่าง

        // เพิ่ม panel เข้าสู่หน้าต่าง
        setContentPane(panel);

        pack(); // ปรับขนาดเฟรมให้พอดีกับคอนเทนต์ภายใน
        setLocationRelativeTo(null); // จัดให้อยู่ตรงกลางหน้าจอ
        setVisible(true); // แสดงหน้าต่าง

        // เพิ่ม KeyListener เพื่อจับการกดปุ่ม F11
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) { // ถ้ากด F11
                    toggleFullscreen(); // เรียกฟังก์ชันสลับโหมดเต็มจอ
                }
            }
        });
    }

    /**
     * ฟังก์ชันสลับโหมดเต็มจอ ↔ หน้าต่างปกติ
     */
    private void toggleFullscreen() {
        fullscreen = !fullscreen; // สลับสถานะ (true <-> false)
        dispose(); // ปิดหน้าต่างชั่วคราวก่อนเปลี่ยนโหมด (จำเป็นใน Swing)

        if (fullscreen) {
            // ---------- เข้าโหมดเต็มจอ ----------
            setUndecorated(true); // ซ่อน title bar
            setExtendedState(JFrame.MAXIMIZED_BOTH); // ขยายเต็มจอ
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            getContentPane().setPreferredSize(screen); // panel ขยายเท่าหน้าจอ
        } else {
            // ---------- กลับสู่โหมดปกติ ----------
            setUndecorated(false); // แสดง title bar กลับมา
            setExtendedState(JFrame.NORMAL); // คืนขนาดเดิม
            getContentPane().setPreferredSize(windowedSize); // panel กลับขนาดเดิม
        }

        pack(); // ปรับขนาดใหม่ให้พอดี
        setLocationRelativeTo(null); // จัดให้อยู่กลางจอ
        setVisible(true); // แสดงอีกครั้ง
    }
}
