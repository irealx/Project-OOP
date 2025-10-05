package main;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        // เริ่มโปรแกรมบน Event Dispatch Thread ของ Swing (ป้องกันบั๊กด้าน UI)
        SwingUtilities.invokeLater(() -> {
            // สร้างหน้าต่างหลักของเกม
            GameFrame frame = new GameFrame();
            // แสดงหน้าต่างบนจอ
            frame.setVisible(true);
        });
    }
}