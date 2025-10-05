package main;

import java.awt.Dimension;
import java.awt.Toolkit;           // ใช้สำหรับดึงขนาดหน้าจอจริง
import java.awt.event.KeyAdapter; // ใช้ตรวจจับปุ่มกด
import java.awt.event.KeyEvent;   // ใช้รหัสปุ่ม (เช่น F11)
import javax.swing.JFrame;
import system.Config;

/**
 * คลาส GameFrame: สร้างหน้าต่างหลักของเกม
 * - จัดการขนาดหน้าต่าง
 * - เพิ่มระบบกด F11 เพื่อสลับโหมด Fullscreen ↔ Window
 */
public class GameFrame extends JFrame {
    private boolean fullscreen = false; // ตัวแปรบอกสถานะว่าอยู่โหมดเต็มจอไหม
    private final GamePanel panel; // พาเนลหลักของเกมที่ต้องขยาย/ย่อ
    private final Dimension windowedSize = new Dimension(Config.PANEL_WIDTH, Config.PANEL_HEIGHT); // ขนาดโหมดปกติ

    // Constructor ของ GameFrame (สร้างหน้าต่างเกม)
    public GameFrame() {
        super("Six Door Maze"); // ตั้งชื่อหน้าต่างเกม

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อกดปุ่ม X
        setResizable(false); // ปิดการปรับขนาดหน้าต่าง

        // สร้าง panel หลักของเกม (จอที่แสดงภาพและอัปเดตเกม)
        panel = new GamePanel();
        setContentPane(panel);
        setPreferredSize(windowedSize);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullscreen();
                }
            }
        });
        fullscreen = true;
        applyWindowState();
    }
    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        applyWindowState();
    }
    
    private void applyWindowState() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (isDisplayable()) {
            dispose();
        }

        setUndecorated(fullscreen);

        if (fullscreen) {
            panel.setGameSize(screenSize.width, screenSize.height);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setSize(screenSize);
        } else {
            panel.setGameSize(windowedSize.width, windowedSize.height);
            setExtendedState(JFrame.NORMAL);
            setSize(windowedSize);
            setLocationRelativeTo(null);
        }

        setVisible(true);
        panel.requestFocusInWindow();
    }
}