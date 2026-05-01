package gameproject.environment;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import gameproject.GamePanel;
import java.awt.Rectangle;
import gameproject.Player;

public class Building {
    public static class DoorInfo {
        public int x, y;
        public String side; // "N", "S", "E", "W"

        public DoorInfo(int x, int y, String side) {
            this.x = x;
            this.y = y;
            this.side = side;
        }
    }

    private java.awt.geom.Area interiorArea;
    private java.awt.geom.Area roofArea;
    private java.util.List<DoorInfo> doors;
    private volatile float roofAlpha = 1.0f;
    private float fadeSpeed = 0.05f;
    private int style;

    public Building(java.util.List<Rectangle> components, java.util.List<DoorInfo> doors) {
        this.interiorArea = new java.awt.geom.Area();
        this.roofArea = new java.awt.geom.Area();
        this.doors = doors;

        for (Rectangle r : components) {
            java.awt.geom.Rectangle2D rect = new java.awt.geom.Rectangle2D.Float(r.x, r.y, r.width, r.height);
            this.interiorArea.add(new java.awt.geom.Area(rect));

            // Mở rộng mái tối thiểu 80px để che phủ hoàn toàn lớp tường (64px) và tạo hiên
            // (16px)
            java.awt.geom.RoundRectangle2D roofRect = new java.awt.geom.RoundRectangle2D.Float(
                    r.x - 80, r.y - 80, r.width + 160, r.height + 176, 30, 30);
            this.roofArea.add(new java.awt.geom.Area(roofRect));
        }
        this.style = (int) (Math.random() * 3);
    }

    public Rectangle getBounds() {
        return interiorArea.getBounds();
    }

    public void update(Player player) {
        int playerCenterX = (int) player.getX() + (Player.SIZE / 2);
        int playerCenterY = (int) player.getY() + (Player.SIZE / 2);

        // Kiểm tra chính xác 100% ranh giới phức hợp
        if (interiorArea.contains(playerCenterX, playerCenterY)) {
            roofAlpha -= fadeSpeed;
            if (roofAlpha < 0.1f)
                roofAlpha = 0.1f;
        } else {
            roofAlpha += fadeSpeed;
            if (roofAlpha > 1.0f)
                roofAlpha = 1.0f;
        }
    }

    public boolean isPlayerInside() {
        return roofAlpha < 0.5f;
    }

    public void renderFloor(Graphics2D g) {
        java.awt.image.BufferedImage floorImg = gameproject.ImageManager.get("floor");
        if (floorImg == null)
            return;

        Graphics2D g2 = (Graphics2D) g.create();

        java.awt.Rectangle tileRect = new java.awt.Rectangle(0, 0, floorImg.getWidth(), floorImg.getHeight());
        java.awt.TexturePaint tp = new java.awt.TexturePaint(floorImg, tileRect);

        g2.setPaint(tp);
        g2.fill(interiorArea);

        if (style == 2) {
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fill(interiorArea);
        }

        g2.setColor(new Color(30, 30, 30, 150));
        g2.draw(interiorArea);

        // VẼ BẬC THỀM ĐI THEO CỬA (Dù là gian nào)
        int pSize = 128; // Tăng kích thước thềm để chìa ra khỏi mái
        for (DoorInfo door : doors) {
            int px = door.x, py = door.y;
            Rectangle pRect;

            if (door.side.equals("N"))
                pRect = new Rectangle(px, py - pSize, pSize, pSize);
            else if (door.side.equals("S"))
                pRect = new Rectangle(px, py + 64, pSize, pSize);
            else if (door.side.equals("W"))
                pRect = new Rectangle(px - pSize, py, pSize, pSize);
            else
                pRect = new Rectangle(px + 64, py, pSize, pSize); // Side E

            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillRect(pRect.x + 4, pRect.y + 4, pRect.width, pRect.height);
            g2.setPaint(tp);
            g2.fillRect(pRect.x, pRect.y, pRect.width, pRect.height);
            g2.setColor(new Color(30, 30, 30, 200));
            g2.drawRect(pRect.x, pRect.y, pRect.width, pRect.height);
        }
        g2.dispose();
    }

    public void renderRoof(Graphics2D g) {
        if (roofAlpha <= 0.0f)
            return;

        java.awt.image.BufferedImage roofImg = gameproject.ImageManager.get("roof");

        Graphics2D g2 = (Graphics2D) g.create();

        if (roofAlpha > 0.5f) {
            g2.setColor(new Color(0, 0, 0, (int) (100 * roofAlpha)));
            java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getTranslateInstance(12, 12);
            g2.fill(roofArea.createTransformedArea(at));
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, roofAlpha));
        g2.setClip(roofArea);

        if (roofImg != null) {
            java.awt.Rectangle tileRect = new java.awt.Rectangle(0, 0, roofImg.getWidth(), roofImg.getHeight());
            java.awt.TexturePaint tp = new java.awt.TexturePaint(roofImg, tileRect);
            g2.setPaint(tp);
            g2.fill(roofArea);
        }

        // ĐÁNH BÓNG CẠNH DƯỚI (Mọi cạnh hướng xuống đều có bóng)
        java.awt.geom.Area shadowEdges = (java.awt.geom.Area) roofArea.clone();
        java.awt.geom.AffineTransform shift = java.awt.geom.AffineTransform.getTranslateInstance(0, 15);
        java.awt.geom.Area shiftedArea = roofArea.createTransformedArea(shift);
        shiftedArea.subtract(roofArea); // Chỉ giữ lại phần chìa ra phía dưới

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fill(shiftedArea);

        g2.dispose();
    }

    public void drawOnMinimap(Graphics2D g, int mapX, int mapY, float scaleX, float scaleY) {
        // Màu mái nhà trên minimap: Nâu đỏ đậm, độ trong suốt thấp để che tường tốt hơn
        g.setColor(new Color(120, 70, 30, 220));

        // Tạo một bản sao của roofArea đã được scale theo minimap
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate(mapX, mapY);
        at.scale(scaleX, scaleY);

        java.awt.Shape scaledRoof = roofArea.createTransformedArea(at);
        g.fill(scaledRoof);

        // Vẽ viền nhẹ cho mái nhà trên minimap
        g.setColor(new Color(60, 30, 10, 255));
        g.draw(scaledRoof);
    }
}
