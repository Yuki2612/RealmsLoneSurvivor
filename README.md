# Pixel Survivor

Dự án game Roguelike Survivor được phát triển bằng Java Swing, tập trung vào tối ưu hóa hiệu năng render 2D và hệ thống chiến đấu nhịp độ cao. Dự án áp dụng các kỹ thuật đồ họa thủ công để mang lại trải nghiệm mượt mà và ổn định.

---

## Trải nghiệm Gameplay

### 1. Luồng Game (Game Flow)
- **Khởi đầu:** Người chơi lựa chọn 1 trong 5 lớp nhân vật với các chỉ số và kỹ năng khởi đầu khác nhau.
- **Chiến đấu:** Tiêu diệt các đợt quái vật (Waves) ngày càng đông đảo. Boss sẽ xuất hiện sau mỗi 5 wave với các cơ chế tấn công đặc thù.
- **Phát triển:** Thu thập EXP để lên cấp và chọn 1 trong 3 thẻ nâng cấp ngẫu nhiên. Mỗi 3 cấp độ, người chơi sẽ được tiếp cận với các "Kỹ năng Đột phá" mạnh mẽ.
- **Tiến hóa vũ khí:** Kết hợp vũ khí cơ bản với các kỹ năng hỗ trợ đạt mốc level yêu cầu để tiến hóa thành các vũ khí huyền thoại (Hellfire Boomstick, Railgun, Lightning Gun).

### 2. Giao diện người dùng (HUD)
Hệ thống HUD hiển thị các thông số thiết yếu giúp người chơi kiểm soát trận đấu:
- **Thanh máu (HP Hearts):** Số lượng tim hiện có. Sát thương từ quái hoặc môi trường sẽ trừ trực tiếp vào các đơn vị tim này.
- **Thanh EXP:** Hiển thị tiến trình đạt cấp độ tiếp theo ở cạnh dưới màn hình.
- **Trạng thái Dash:** Thông báo "READY" hoặc "WAIT" hiển thị trực quan thời gian hồi chiêu của kỹ năng lướt.
- **Kinh tế:** Hiển thị số lượng Vàng (Gold) và Đá linh hồn (Soul Stones) thu thập được trong và sau trận đấu.
- **Hiệu năng:** Hiển thị FPS thực tế để theo dõi độ ổn định của trò chơi.

### 3. Hệ thống Combo (Heat Meter)
Duy trì chuỗi tiêu diệt để kích hoạt các trạng thái cường hóa:
- **Tier 1 (15x):** +10% Tốc độ di chuyển.
- **Tier 2 (30x):** +15% Tốc độ di chuyển, +5% Tốc độ bắn. Hiển thị thông báo: **"GREAT!"**.
- **Tier 3 (50x):** +20% Tốc độ di chuyển, +10% Tốc độ bắn. Nhân vật sẽ có hiệu ứng lóe sáng và màn hình xuất hiện viền **Vàng Cam (Golden-Orange)** rực cháy kèm tiêu đề: **"RAMPAGE!"**.

---

## Hệ thống Sự kiện và Môi trường

### 1. Sự kiện Toàn cầu (Global Events)
Các sự kiện xuất hiện ngẫu nhiên với cảnh báo trước 10 giây:
- **Blood Moon:** Tăng 1.5x số lượng và tốc độ xuất hiện quái vật. Toàn bộ màn hình bao phủ bởi sắc đỏ rực rỡ.
- **Acid Rain:** Mưa axit gây sát thương liên tục lên người chơi mỗi 0.5 giây. Người chơi **bắt buộc phải tìm nơi trú ẩn** bên trong các tòa nhà để ngăn chặn việc mất máu.
- **Darkness:** Thu hẹp tầm nhìn đáng kể, buộc người chơi phải chiến đấu trong không gian tối tăm với hiệu ứng đèn pin giới hạn.
- **Mimic Mania:** Sinh ra các rương báu đặc biệt bên trong các tòa nhà. Nếu người chơi không kịp mở rương trước khi sự kiện kết thúc, chúng sẽ thức tỉnh và biến thành quái vật **Mimic** hung dữ.

### 2. Tương tác Môi trường
- **Hệ thống Tòa nhà:** Các công trình sinh ngẫu nhiên (Rectangle, L, T, U). Mái nhà sẽ tự động mờ đi (Fade-out) khi người chơi bước vào bên trong. Đây là nơi trú ẩn an toàn duy nhất trong sự kiện Acid Rain.
- **Vật thể phá hủy (Destructibles):** Người chơi có thể tấn công và phá hủy cây cối, đá và thùng gỗ. Phá hủy thùng gỗ có cơ hội rơi ra các vật phẩm hỗ trợ.
- **AI Pathfinding:** Quái vật sử dụng Flow Field để tìm đường vòng qua vật cản, nhưng chúng sẽ chủ động tấn công và phá hủy vật thể nếu bị chặn đường quá lâu.

---

## Đột phá Kỹ thuật: Pixel-Perfect Snapping

Dự án giải quyết triệt để vấn đề rung lắc hình ảnh (jittering) thông qua kiến trúc Render khóa tọa độ:
- **Camera Snapshot:** Chốt tọa độ Camera ở dạng số nguyên (Integer) tại đầu mỗi frame.
- **Snapped Translation:** Toàn bộ Graphics2D được dịch chuyển theo tọa độ đã khóa, đảm bảo mọi Sprite (Player, Enemy, Projectile) luôn hiển thị khớp trên lưới pixel của màn hình.
- **Z-Ordering (Y-Sorting):** Sắp xếp thứ tự render dựa trên tọa độ chân của vật thể, đảm bảo các tòa nhà, cây cối và nhân vật luôn hiển thị đúng tầng lớp không gian.

---

## Cấu trúc Dự án
- `gameproject.state`: Quản lý các trạng thái máy (Menu, Playing, LevelUp, CharacterSelect...).
- `gameproject.entity`: Logic nhân vật, quái vật, Boss và hệ thống rơi vật phẩm (Drops).
- `gameproject.weapon`: Hệ thống vũ khí, đạn dược và logic tiến hóa.
- `gameproject.environment`: Quản lý bản đồ, công trình sinh ngẫu nhiên và Flow Field AI.
- `gameproject.ui`: Hệ thống HUD và các giao diện người dùng Overlay.

---
Phát triển bởi đội ngũ đam mê kỹ thuật và trải nghiệm người dùng.