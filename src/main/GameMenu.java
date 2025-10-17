package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * GameMenu — หน้าแนะนำเกมในธีม Horror
 * แสดงมอนสเตอร์ 3 ตัว พร้อมคำอธิบายสกิลและวิธีเล่น
 */
public class GameMenu {

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 64);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 24);
    private static final Font CARD_TITLE_FONT = new Font("SansSerif", Font.BOLD, 26);
    private static final Font CARD_DESC_FONT = new Font("SansSerif", Font.PLAIN, 17);

    private BufferedImage stunImg, wrapImg, shootImg;

    public GameMenu() {
        loadImages();
    }

    private void loadImages() {
        try {
            stunImg  = ImageIO.read(new File("Pic/character/Mon/skill1.png"));
            wrapImg  = ImageIO.read(new File("Pic/character/Mon/death.png"));
            shootImg = ImageIO.read(new File("Pic/character/Mon/summon.png"));
        } catch (IOException e) {
            System.err.println("⚠ Cannot load monster images.");
        }
    }

    public void draw(Graphics2D g2, int width, int height) {
        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // พื้นหลัง gradient ดำ -> แดงเลือด
        GradientPaint grad = new GradientPaint(0, 0, new Color(0, 0, 0, 240),
                                               0, height, new Color(80, 0, 0, 200));
        g2.setPaint(grad);
        g2.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int startY = height / 6;

        // หัวข้อเกม
        g2.setColor(Color.WHITE);
        g2.setFont(TITLE_FONT);
        drawCenteredString(g2, "SIX DOOR MAZE", centerX, startY);

        g2.setFont(SUBTITLE_FONT);
        drawCenteredString(g2, "Use W A S D or Arrow Keys to Move", centerX, startY + 70);
        drawCenteredString(g2, "Avoid Monsters and Find Answer to Enter the Correct Door", centerX, startY + 110);

        // การ์ดมอนสเตอร์
        int cardTop = startY + 250;
        int spacing = width / 4;
        drawMonsterCard(g2, centerX - spacing, cardTop, new Color(70,150,255),
                "STUN MONSTER", "Releases a blue shockwave that stuns you.", stunImg);
        drawMonsterCard(g2, centerX, cardTop, new Color(170,100,255),
                "WRAP MONSTER", "Teleports instantly and attacks you from behind.", wrapImg);
        drawMonsterCard(g2, centerX + spacing, cardTop, new Color(255,120,80),
                "SHOOTING MONSTER", "Fires energy projectiles from distance that can kill instantly.", shootImg);

        // ปุ่มเริ่มเกม (มีกล่องพอดีข้อความ)
        drawTextWithBox(g2,
            "Press ENTER to Begin",
            centerX,
            height - 100,
            SUBTITLE_FONT,
            Color.WHITE,
            new Color(255, 80, 80)
        );

        g2.dispose();
    }

    /** วาดการ์ดมอนสเตอร์แต่ละตัว */
    private void drawMonsterCard(Graphics2D g2, int centerX, int topY, Color accent, String title, String description, BufferedImage img) {
        int cardW = 260, cardH = 240, corner = 25;
        int iconSize = 110;
        int iconX = centerX - iconSize / 2;
        int iconY = topY + 20;

        // เงาใต้การ์ด
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(centerX - cardW/2 + 4, topY + 4, cardW, cardH, corner, corner);

        // การ์ดพื้นหลัง
        g2.setColor(new Color(30, 30, 30, 180));
        g2.fillRoundRect(centerX - cardW/2, topY, cardW, cardH, corner, corner);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(centerX - cardW/2, topY, cardW, cardH, corner, corner);

        // วงแสงกลาง
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
        g2.fillOval(iconX - 10, iconY - 10, iconSize + 20, iconSize + 20);

        // ภาพมอน
        if (img != null) {
            g2.drawImage(img.getSubimage(0, 0, 100, 100), iconX, iconY, iconSize, iconSize, null);
        } else {
            g2.setColor(accent);
            g2.fillOval(iconX, iconY, iconSize, iconSize);
        }

        // วงพลัง
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(iconX - 2, iconY - 2, iconSize + 4, iconSize + 4);

        // ชื่อมอน
        g2.setColor(Color.WHITE);
        g2.setFont(CARD_TITLE_FONT);
        drawCenteredString(g2, title, centerX, iconY + iconSize + 30);

        // เส้นใต้ชื่อ
        g2.setColor(accent);
        g2.fillRect(centerX - 50, iconY + iconSize + 35, 100, 2);

        // คำอธิบาย (ตัดบรรทัดอัตโนมัติ)
        g2.setFont(CARD_DESC_FONT);
        g2.setColor(new Color(220, 220, 220));
        drawWrappedText(g2, description, centerX, iconY + iconSize + 60, cardW - 40, 22);
    }

    // ---- 🔹 Utilities ----

    /** วาดข้อความให้อยู่กึ่งกลางแนวนอน */
    private void drawCenteredString(Graphics2D g2, String text, int centerX, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2.drawString(text, centerX - textWidth / 2, y);
    }

    /** วาดข้อความหลายบรรทัดให้อยู่กึ่งกลาง */
    private void drawWrappedText(Graphics2D g2, String text, int centerX, int startY, int maxWidth, int lineSpacing) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int y = startY;

        for (String word : words) {
            String testLine = line + word + " ";
            int width = fm.stringWidth(testLine);
            if (width > maxWidth && line.length() > 0) {
                drawCenteredString(g2, line.toString(), centerX, y);
                line = new StringBuilder(word + " ");
                y += lineSpacing;
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0) {
            drawCenteredString(g2, line.toString(), centerX, y);
        }
    }

    /** วาดข้อความพร้อมกรอบโค้งมนพอดีข้อความ */
    private void drawTextWithBox(Graphics2D g2, String text, int centerX, int y, Font font, Color textColor, Color boxColor) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        int boxPaddingX = 20;
        int boxPaddingY = 10;
        int boxWidth = textWidth + boxPaddingX * 2;
        int boxHeight = textHeight + boxPaddingY * 2;

        int boxX = centerX - boxWidth / 2;
        int boxY = y - textHeight;

        // วาดกล่องพื้นหลัง (โปร่งแสง)
        g2.setColor(new Color(boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), 60));
        g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 18, 18);

        // เส้นขอบกล่อง
        g2.setColor(boxColor);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 18, 18);

        // วาดข้อความตรงกลางกล่อง
        g2.setColor(textColor);
        g2.drawString(text, centerX - textWidth / 2, boxY + fm.getAscent() + boxPaddingY);
    }
}
