package gameproject.environment;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
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

    private java.awt.TexturePaint floorPaint;
    private java.awt.TexturePaint roofPaint;

    private java.awt.Rectangle cachedBounds;

    // --- BAKING FIELDS ---
    private java.awt.image.BufferedImage bakedFloorImg;
    private java.awt.image.BufferedImage bakedRoofImg;
    private java.awt.image.BufferedImage bakedShadowImg;
    private int bakedX, bakedY;

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
        
        // --- TEXTURE CACHING ---
        java.awt.image.BufferedImage floorImg = gameproject.ImageManager.get("floor");
        if (floorImg != null) {
            java.awt.Rectangle tileRect = new java.awt.Rectangle(0, 0, floorImg.getWidth(), floorImg.getHeight());
            this.floorPaint = new java.awt.TexturePaint(floorImg, tileRect);
        }

        java.awt.image.BufferedImage roofImg = gameproject.ImageManager.get("roof");
        if (roofImg != null) {
            java.awt.Rectangle tileRect = new java.awt.Rectangle(0, 0, roofImg.getWidth(), roofImg.getHeight());
            this.roofPaint = new java.awt.TexturePaint(roofImg, tileRect);
        }

        // --- GEOMETRIC CACHING (Optimization Part 1) ---
        // Cache shadow areas to avoid expensive Area operations during render
        java.awt.geom.AffineTransform shadowAt = java.awt.geom.AffineTransform.getTranslateInstance(12, 12);
        java.awt.geom.Area roofShadowArea = roofArea.createTransformedArea(shadowAt);

        java.awt.geom.AffineTransform shift = java.awt.geom.AffineTransform.getTranslateInstance(0, 15);
        java.awt.geom.Area bottomShadowEdge = roofArea.createTransformedArea(shift);
        bottomShadowEdge.subtract(roofArea);

        this.cachedBounds = roofArea.getBounds();
        this.cachedBounds.add(roofShadowArea.getBounds());
        // Mở rộng lề
        this.cachedBounds.x -= 10;
        this.cachedBounds.y -= 10;
        this.cachedBounds.width += 40;
        this.cachedBounds.height += 40;
        
        this.bakedX = cachedBounds.x;
        this.bakedY = cachedBounds.y;

        bakeImages(roofShadowArea, bottomShadowEdge);
    }

    private void bakeImages(java.awt.geom.Area roofShadowArea, java.awt.geom.Area bottomShadowEdge) {
        int w = cachedBounds.width;
        int h = cachedBounds.height;
        if (w <= 0 || h <= 0) return;

        bakedFloorImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        bakedRoofImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        bakedShadowImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getTranslateInstance(-bakedX, -bakedY);

        // 1. Bake Ground Shadow
        Graphics2D gS = bakedShadowImg.createGraphics();
        gS.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gS.setColor(new Color(0, 0, 0, 100));
        gS.fill(roofShadowArea.createTransformedArea(at));
        gS.dispose();

        // 2. Bake Floor
        Graphics2D gF = bakedFloorImg.createGraphics();
        gF.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (floorPaint != null) {
            gF.setPaint(floorPaint);
            gF.fill(interiorArea.createTransformedArea(at));
        }
        if (style == 2) {
            gF.setColor(new Color(0, 0, 0, 60));
            gF.fill(interiorArea.createTransformedArea(at));
        }
        gF.setColor(new Color(30, 30, 30, 150));
        gF.draw(interiorArea.createTransformedArea(at));

        // Bake Steps
        int pSize = 128;
        for (DoorInfo door : doors) {
            int px = door.x, py = door.y;
            Rectangle pRect;
            if (door.side.equals("N")) pRect = new Rectangle(px, py - pSize, pSize, pSize);
            else if (door.side.equals("S")) pRect = new Rectangle(px, py + 64, pSize, pSize);
            else if (door.side.equals("W")) pRect = new Rectangle(px - pSize, py, pSize, pSize);
            else pRect = new Rectangle(px + 64, py, pSize, pSize);

            gF.setColor(new Color(0, 0, 0, 50));
            gF.fillRect(pRect.x + 4 - bakedX, pRect.y + 4 - bakedY, pRect.width, pRect.height);
            if (floorPaint != null) {
                gF.setPaint(floorPaint);
                gF.fillRect(pRect.x - bakedX, pRect.y - bakedY, pRect.width, pRect.height);
            }
            gF.setColor(new Color(30, 30, 30, 200));
            gF.drawRect(pRect.x - bakedX, pRect.y - bakedY, pRect.width, pRect.height);
        }
        gF.dispose();

        // 3. Bake Roof
        Graphics2D gR = bakedRoofImg.createGraphics();
        gR.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (roofPaint != null) {
            gR.setPaint(roofPaint);
            gR.fill(roofArea.createTransformedArea(at));
        }
        gR.setColor(new Color(0, 0, 0, 80));
        gR.fill(bottomShadowEdge.createTransformedArea(at));
        gR.dispose();
    }

    public Rectangle getBounds() {
        return interiorArea.getBounds();
    }

    public boolean isVisible(int camX, int camY, int screenW, int screenH) {
        return cachedBounds.intersects(camX, camY, screenW, screenH);
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

    public void renderFloor(Graphics2D g2d) {
        if (bakedFloorImg != null) {
            g2d.drawImage(bakedFloorImg, bakedX, bakedY, null);
        }
    }

    public void renderRoof(Graphics2D g2d) {
        if (roofAlpha <= 0.0f) return;

        // Vẽ bóng đổ dưới đất
        if (bakedShadowImg != null && roofAlpha > 0.5f) {
            g2d.drawImage(bakedShadowImg, bakedX, bakedY, null);
        }

        // Vẽ mái nhà với độ trong suốt
        if (bakedRoofImg != null) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, roofAlpha));
            g2d.drawImage(bakedRoofImg, bakedX, bakedY, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
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
