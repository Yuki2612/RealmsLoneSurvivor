package gameproject.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import gameproject.FontManager;
import gameproject.ImageManager;
import gameproject.GamePanel;

public class DialogueManager {
    public static class DialogueLine {
        public String speaker;
        public String text;
        public String portraitKey;

        public DialogueLine(String speaker, String text, String portraitKey) {
            this.speaker = speaker;
            this.text = text;
            this.portraitKey = portraitKey;
        }
    }

    private Queue<DialogueLine> dialogueQueue = new LinkedList<>();
    private DialogueLine currentLine = null;
    private boolean active = false;

    // Hiệu ứng chữ chạy
    private String displayedText = "";
    private int charIndex = 0;
    private long lastCharTime = 0;
    private final int typeSpeed = 30; // ms per char

    public synchronized void startDialogue(ArrayList<DialogueLine> lines) {
        dialogueQueue.clear();
        dialogueQueue.addAll(lines);
        active = true;
        GamePanel.pauseGame();
        nextPace();
    }

    public synchronized void nextPace() {
        if (currentLine != null && charIndex < currentLine.text.length()) {
            // Nếu đang chạy chữ mà nhấn Enter, hiện hết luôn
            displayedText = currentLine.text;
            charIndex = currentLine.text.length();
            return;
        }

        if (dialogueQueue.isEmpty()) {
            active = false;
            currentLine = null;
            GamePanel.resumeGame();
            return;
        }

        currentLine = dialogueQueue.poll();
        displayedText = "";
        charIndex = 0;
        lastCharTime = System.currentTimeMillis();
    }

    public synchronized void update() {
        if (!active || currentLine == null) return;

        if (charIndex < currentLine.text.length()) {
            long now = System.currentTimeMillis();
            if (now - lastCharTime > typeSpeed) {
                charIndex++;
                displayedText = currentLine.text.substring(0, charIndex);
                lastCharTime = now;
            }
        }
    }

    public synchronized void draw(Graphics g, int screenWidth, int screenHeight) {
        DialogueLine line = currentLine;
        if (!active || line == null) return;

        Graphics2D g2d = (Graphics2D) g;

        // 1. Overlay mờ màn hình sau
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // 2. Vẽ Hộp thoại lớn hơn
        int boxW = 1050;
        int boxH = 240;
        int boxX = (screenWidth - boxW) / 2;
        int boxY = screenHeight - boxH - 50;

        // Shadow & Background
        g2d.setColor(new Color(15, 15, 25, 240));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 20, 20);
        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(new Color(0, 220, 255)); // Cyan border
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 20, 20);

        // 3. Vẽ Portrait lớn hơn
        java.awt.image.BufferedImage portrait = ImageManager.get(line.portraitKey);
        if (portrait != null) {
            int pSize = 160;
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(boxX + 30, boxY + 30, pSize, pSize);
            g2d.drawImage(portrait, boxX + 30, boxY + 30, pSize, pSize, null);
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.drawRect(boxX + 30, boxY + 30, pSize, pSize);
        }

        // 4. Vẽ Tên người nói lớn hơn
        g2d.setFont(FontManager.getFont(32f));
        g2d.setColor(new Color(255, 215, 0)); // Gold
        g2d.drawString(line.speaker, boxX + 220, boxY + 55);

        // 5. Vẽ Nội dung thoại lớn hơn
        g2d.setFont(FontManager.getFont(26f));
        g2d.setColor(Color.WHITE);
        
        // Cắt dòng với số ký tự tối ưu cho cỡ chữ mới
        String[] lines = splitText(displayedText, 40);
        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], boxX + 220, boxY + 105 + (i * 38));
        }

        // 6. Indicator "Press ENTER"
        if (charIndex == line.text.length()) {
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                g2d.setFont(FontManager.getFont(18f));
                g2d.setColor(Color.GRAY);
                g2d.drawString("Press [ENTER] to continue", boxX + boxW - 300, boxY + boxH - 20);
            }
        }
    }

    private String[] splitText(String text, int maxChars) {
        if (text.length() <= maxChars) return new String[]{text};
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxChars) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
            if (currentLine.length() > 0) currentLine.append(" ");
            currentLine.append(word);
        }
        lines.add(currentLine.toString());
        return lines.toArray(new String[0]);
    }

    public boolean isActive() { return active; }
}
