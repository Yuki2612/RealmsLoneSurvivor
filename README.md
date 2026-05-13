# REALMS: LONE SURVIVOR

REALMS: LONE SURVIVOR là một dự án game Roguelike Survivor được phát triển trên nền tảng Java Swing. Game tập trung vào việc tối ưu hóa hiệu năng render 2D thủ công, mang lại trải nghiệm chiến đấu nhịp độ cao, mượt mà với phong cách đồ họa Pixel Art hiện đại.

---

## Luồng Gameplay

### 1. Khởi đầu và Phát triển
- Người chơi lựa chọn 1 trong 5 lớp nhân vật (Soldier, Assassin, Wizard, Ranger, Tank) với các chỉ số cơ bản khác nhau.
- Chiến đấu qua các đợt quái vật (Waves) trên các bản đồ đặc trưng như Grassland và Swamp.
- Thu thập EXP để lên cấp, lựa chọn giữa các thẻ nâng cấp chỉ số hoặc kỹ năng đột phá.
- Sau mỗi 5 wave, một Boss hùng mạnh sẽ xuất hiện với cơ chế tấn công đặc trưng.

### 2. Hệ thống Combo (Heat Meter)
Duy trì chuỗi tiêu diệt liên tục để kích hoạt các trạng thái cường hóa:
- Tier 1 (15x): Tăng 10% Tốc độ di chuyển.
- Tier 2 (30x): Tăng 15% Tốc độ di chuyển và 5% Tốc độ bắn (Thông báo "GREAT!").
- Tier 3 (50x - RAMPAGE): Tăng 20% Tốc độ di chuyển và 10% Tốc độ bắn. Nhân vật có hiệu ứng lóe sáng và màn hình xuất hiện viền rực cháy.

---

## Hệ thống Trang bị và Kỹ năng

### 1. Vũ khí và Tiến hóa
- Pistol: Vũ khí cơ bản, tốc độ trung bình.
- Shotgun: Sát thương diện rộng tầm gần. Tiến hóa thành Hellfire Boomstick (Đạn lửa xuyên thấu).
- Assault Rifle: Tốc độ bắn cực nhanh. Tiến hóa thành Lightning Gun (Đạn sét liên hoàn).
- Sniper Rifle: Sát thương cực cao, tầm xa. Tiến hóa thành Railgun (Tia laser xuyên thấu mọi mục tiêu).

### 2. Kỹ năng Đột phá (Breakthrough Skills)
Người chơi có thể nhận các kỹ năng đặc biệt mỗi 3 cấp độ:
- Chain Lightning: Sét chuyền giữa các mục tiêu.
- Trail of Fire: Để lại vệt lửa gây sát thương khi di chuyển.
- Orbiting Orbs: Các quả cầu ma thuật xoay quanh bảo vệ nhân vật.
- Explosive Bullets: Đạn phát nổ khi va chạm.
- Frost Aura: Hào quang làm chậm và đóng băng kẻ thù xung quanh.
- Poison Cloud: Tạo ra các đám mây độc gây sát thương theo thời gian.
- Energy Shield: Khiên năng lượng hấp thụ sát thương.
- Meteor Strike: Gọi thiên thạch ngẫu nhiên xuống bản đồ.
- Pulse Wave: Sóng xung kích đẩy lùi và gây sát thương diện rộng.

---

## Thư viện Kẻ thù và Boss

### 1. Các loại quái vật
- Normal Enemy: Kẻ địch cơ bản, áp sát số lượng đông.
- Assassin: Di chuyển cực nhanh, tấn công bất ngờ.
- Shooter: Tấn công từ xa bằng đạn.
- Spawner: Triệu hồi thêm các quái vật nhỏ.
- Wizard: Bắn đạn nổ gây sát thương diện rộng.
- Mimic: Ngụy trang dưới dạng rương báu, thức tỉnh trong sự kiện đặc biệt.

### 2. Các thực thể Boss
- Soul Reaper: Sử dụng các đòn lướt chém và dash tầm gần.
- The Shadow: Có khả năng tàng hình và dịch chuyển tức thời.
- Cannon Turtle: Boss pháo binh với khả năng nã pháo truy đuổi và bắn đạn nổ diện rộng.
- The Dark Fairy: Tấn công bằng các loạt đạn ma thuật dày đặc.
- Phantom Warlock: Tạo ảo ảnh phân thân và lồng giam linh hồn.
- The King: Boss tối cao với các đòn đánh uy lực và diện rộng.
- Swamp Priest: Boss 2 giai đoạn với khả năng bùa chú, dịch chuyển và cưỡi kẻ hầu cận trong giai đoạn 2.

---

## Sự kiện và Môi trường

### 1. Sự kiện Toàn cầu
- Blood Moon: Tăng mạnh số lượng và tốc độ của quái vật.
- Acid Rain: Mưa axit gây sát thương liên tục, yêu cầu người chơi phải trú ẩn trong các tòa nhà.
- Darkness: Thu hẹp tầm nhìn, yêu cầu sử dụng đèn pin để chiến đấu.
- Mimic Mania: Rương báu thức tỉnh thành quái vật Mimic.
- Toxic Waters: Vùng nước đầm lầy trở nên cực độc, gây sát thương lớn khi bước vào.
- Altar Ascension: Yêu cầu người chơi đứng trong vùng hào quang của Bàn thờ trong 30 giây để thăng hoa. Thành công sẽ tăng 1 Máu tối đa và tiêu diệt toàn bộ kẻ địch trên màn hình. Thất bại sẽ bị trừ máu và triệu hồi đợt quái vật hung hãn.

### 2. Tương tác bản đồ
- Hệ thống tòa nhà sinh ngẫu nhiên với mái nhà tự động mờ dần (Fade-out).
- Các vật thể có thể phá hủy (Cây, đá, thùng gỗ) để thu thập vật phẩm hỗ trợ.
- Hệ thống đầm lầy và vùng nước ảnh hưởng đến tốc độ di chuyển.

---

## Kỹ thuật và Hiệu năng
- Pixel-Perfect Snapping: Loại bỏ rung lắc hình ảnh bằng cách khóa tọa độ Camera và vật thể ở dạng số nguyên tại mỗi frame.
- Y-Sorting: Hệ thống phân lớp render thông minh dựa trên tọa độ chân của vật thể, tạo chiều sâu 2D chân thực.
- Flow Field AI: Quản lý hàng trăm kẻ thù di chuyển mượt mà mà không gây tụt FPS.
- Hệ thống Save/Load: Lưu trữ tiến trình chơi, thành tựu và nâng cấp vĩnh viễn qua file .dat.

---

## Hướng dẫn Cài đặt và Chạy
Trò chơi hiện đã được đóng gói dưới dạng file thực thi độc lập (.exe) để thuận tiện cho việc trải nghiệm.

1. Tải về gói cài đặt của trò chơi.
2. Chạy file `REALMS.exe` để bắt đầu.
3. Dữ liệu save và thành tựu sẽ được lưu tự động tại thư mục gốc của game.

---

## Cấu trúc Mã nguồn
- `gameproject.state`: Quản lý các trạng thái của game (Menu, Playing, LevelUp, Stats...).
- `gameproject.entity`: Logic của nhân vật, quái vật, Boss và hệ thống Drops.
- `gameproject.weapon`: Hệ thống vũ khí, đạn dược và logic tiến hóa.
- `gameproject.skill`: Danh sách các kỹ năng nâng cấp và đột phá.
- `gameproject.environment`: Quản lý bản đồ, công trình, sự kiện và AI tìm đường.
- `gameproject.meta`: Hệ thống thành tựu, lưu trữ và dữ liệu người chơi.
- `gameproject.ui`: Hệ thống HUD, menu và các hiệu ứng Overlay.

---
Dự án đang trong quá trình phát triển và hoàn thiện các nội dung mới.
