import java.awt.Dimension;
import javax.swing.JFrame;

public class GameFrame extends JFrame {

    // Constructor ของ GameFrame (สร้างหน้าต่างเกม)
    public GameFrame() {
        super("Six Door Maze"); // ตั้งชื่อหน้าต่างเกม

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อกดปุ่ม X
        setResizable(false); // ปิดการปรับขนาดหน้าต่าง

        // สร้าง panel หลักของเกม (จอที่แสดงภาพและอัปเดตเกม)
        GamePanel panel = new GamePanel();

        // กำหนดขนาดของ panel
        panel.setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));

        // เพิ่ม panel เข้าสู่หน้าต่าง
        setContentPane(panel);

        // ปรับขนาดเฟรมให้พอดีกับคอนเทนต์ภายใน
        pack();

        // จัดให้อยู่ตรงกลางหน้าจอ
        setLocationRelativeTo(null);
    }
}