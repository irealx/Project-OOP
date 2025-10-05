package main;

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
    private final GamePanel panel; // พาเนลหลักของเกมที่ต้องขยาย/ย่อ
    private final Dimension windowedSize = new Dimension(GamePanel.DEFAULT_WIDTH, GamePanel.DEFAULT_HEIGHT); // ขนาดโหมดปกติ

    // Constructor ของ GameFrame (สร้างหน้าต่างเกม)
    public GameFrame() {
        super("Six Door Maze"); // ตั้งชื่อหน้าต่างเกม

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อกดปุ่ม X
        setResizable(false); // ปิดการปรับขนาดหน้าต่าง

        // สร้าง panel หลักของเกม (จอที่แสดงภาพและอัปเดตเกม)