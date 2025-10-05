package main;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;

/**
 * GameFrame — หน้าต่างหลักของเกม
 * เปิดเกมในโหมดเต็มหน้าจอทันที (fullscreen)
 * ไม่มีระบบกด F11 สลับโหมด
 */
public class GameFrame extends JFrame {

    private final GamePanel panel;

    public GameFrame() {
        super("Six Door Maze");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // สร้างและเพิ่ม GamePanel (จอเกมหลัก)
        panel = new GamePanel();
        setContentPane(panel);

        // ดึงขนาดหน้าจอจริงของเครื่อง
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // ตั้งค่าเป็น fullscreen
        setUndecorated(true); // ซ่อนขอบหน้าต่าง
        setExtendedState(JFrame.MAXIMIZED_BOTH); // ขยายเต็มจอ
        setSize(screenSize);

        // แจ้งให้ GamePanel รู้ขนาดหน้าจอจริง
        panel.setGameSize(screenSize.width, screenSize.height);

        setVisible(true);
        panel.requestFocusInWindow(); // โฟกัสไปยัง panel ทันที
    }
}
