# 🎮 Pixel Survivor

**Pixel Survivor** là một dự án game nhập vai sinh tồn (Survivor-like) được xây dựng bằng Java Swing. Người chơi sẽ điều khiển nhân vật chiến đấu chống lại bầy đàn quái vật, thu thập tài nguyên và nâng cấp kỹ năng để sống sót lâu nhất có thể.

---

## 🚀 Hướng dẫn khởi chạy

### Yêu cầu hệ thống
*   **Java SDK**: Phiên bản 17 hoặc 21 (Khuyên dùng 21).
*   **Build System**: Gradle (Đã tích hợp sẵn Wrapper).

### Cách chạy Game
1.  **Sử dụng Terminal**:
    ```bash
    ./gradlew run
    ```
2.  **Sử dụng VS Code**:
    *   Mở thư mục gốc của dự án.
    *   Nhấn `F5` hoặc chọn **Run > Start Debugging**.

---

## ✨ Tính năng nổi bật
*   **Hệ thống Combo/Heat Meter**: Buff tốc độ và hiệu ứng hình ảnh (Energy Surge) khi đạt Tier cao.
*   **Bản đồ ngẫu nhiên**: Địa hình, nhà cửa và vật thể tự nhiên được sinh mới mỗi khi bắt đầu trận đấu.
*   **Phản ứng nguyên tố**: Kết hợp các loại sát thương (Lửa, Băng, Độc, Điện) để tạo ra các hiệu ứng combo mạnh mẽ.
*   **Hệ thống VFX mượt mà**: Hiệu ứng hạt, rung màn hình và văn bản sát thương sinh động.
*   **Vật lý va chạm chuẩn**: Cơ chế trượt (Sliding collision) giúp quái vật di chuyển thông minh quanh vật cản.

---

## 📂 Cấu trúc dự án
*   `app/src/main/java/gameproject/`: Mã nguồn chính của game.
    *   `entity/`: Quản lý nhân vật, quái vật và Boss.
    *   `environment/`: Hệ thống bản đồ, vật cản và Flow Field.
    *   `skill/`: Logic các kỹ năng chủ động và bị động.
    *   `state/`: Quản lý các trạng thái game (Menu, Playing, GameOver...).
    *   `weapon/`: Hệ thống vũ khí và đạn dược.
*   `app/res/`: Chứa tài nguyên game (Hình ảnh, Âm thanh, Font).

---

## 🤝 Quy tắc đóng góp (Contributing)
Để dự án phát triển ổn định, các thành viên vui lòng tuân thủ:
1.  **Nhánh (Branching)**: Tạo nhánh mới từ `develop` cho mỗi tính năng (`feat/...`) hoặc sửa lỗi (`fix/...`).
2.  **Commit Message**: Viết tiếng Việt có dấu, rõ ràng mục đích. Ví dụ: `feat: thêm vũ khí Súng máy`, `fix: sửa lỗi kẹt quái vào tường`.
3.  **Code Style**: 
    *   Đảm bảo code an toàn đa luồng khi làm việc với `VFXManager`.
    *   Sử dụng `MapManager` để kiểm tra va chạm môi trường.

---

## 🚩 Lộ trình sắp tới (Roadmap)
- [ ] Bổ sung các Sprite nhân vật thiếu: `player2.png` -> `player5.png`.
- [ ] Thiết kế thêm hệ thống Boss cho các Wave cao hơn (Wave 20+).
- [ ] Tối ưu hóa hiệu năng Flow Field cho map kích thước cực lớn.

---
*Chúc cả nhóm phát triển dự án Pixel Survivor thật thành công!* 🍻