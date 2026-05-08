# 🎮 Thiết kế chi tiết: Elite Enemies & Wave Challenges

---

## 📐 Phần I: Quái Tinh Anh (Elite Enemies)

### 1.1. Khái niệm

Elite là một **biến thể mạnh hơn** của bất kỳ loại quái nào (Normal, Shooter, Assassin, Wizard). Không phải class mới — mà là **modifier gắn lên quái thường**. Giống hệ thống Rare/Champion monsters trong Diablo.

### 1.2. Thiết kế dữ liệu — Thêm trường vào `Enemy.java`

```java
// Enemy.java - Thêm các trường sau
public boolean isElite = false;
public EliteAffix eliteAffix = null;

public enum EliteAffix {
    EXPLOSIVE,     // Nổ khi chết, gây 1 damage trong bán kính 100px
    SPLITTING,     // Tách thành 2 con nhỏ hơn khi HP < 50%
    SHIELDED,      // Khiên chắn 3 hit đầu tiên (damage = 0)
    VAMPIRIC,      // Hồi HP khi đánh trúng người chơi
    BERSERKER      // Tốc độ x2 khi HP < 30%
}
```

### 1.3. Chỉ số Elite

| Thuộc tính | Quái thường | Quái Elite |
|-----------|-------------|------------|
| HP | Theo tier | **x3** |
| Kích thước | `size` | `size * 1.4` (to hơn rõ rệt) |
| Tốc độ | Theo tier | **x0.85** (chậm hơn 1 chút, đổi lại trâu) |
| EXP khi giết | Bình thường | **x5** |
| Loot | Bình thường | Đảm bảo rớt 1 Soul + Gold x3 |

### 1.4. Quy tắc Spawn

```
Wave 1-4:   Không có Elite
Wave 5-9:   8% cơ hội mỗi con quái được spawn là Elite
Wave 10-14: 12% cơ hội
Wave 15-19: 15% cơ hội  
Wave 20-25: 18% cơ hội
Giới hạn:   Tối đa 2 Elite trên màn hình cùng lúc
```

**Spawn xa hơn:** Elite luôn spawn ở khoảng cách xa gấp đôi so với quái thường (offset 100px thay vì 50px ra khỏi camera), cho người chơi thời gian chuẩn bị.

### 1.5. Hệ thống Affix chi tiết

#### 💥 EXPLOSIVE (Nổ khi chết)
- Khi Elite bị giết → Phát nổ sau **0.8 giây delay** (vẽ vòng tròn đỏ nhấp nháy cảnh báo)
- Bán kính: 120px, Damage: 1 heart
- **Phản ứng người chơi:** Phải lùi ra xa ngay sau khi giết

#### 🔀 SPLITTING (Tách đôi)
- Khi HP < 50% → Tách thành **2 con cùng loại**, mỗi con có 40% HP gốc
- Con tách ra **không phải Elite** (tránh chain split vô hạn)
- Kích thước con tách = 80% kích thước gốc
- **Chỉ tách 1 lần** (dùng cờ `hasSplit`)

#### 🛡️ SHIELDED (Khiên sát thương)
- 3 hit đầu tiên gây damage = 0 (kể cả crit)
- Hiển thị: Vòng tròn xanh dương bao quanh → vỡ tan khi hết shield
- **Phản ứng người chơi:** Cần tập trung DPS, không phí đạn vào nhiều mục tiêu

#### 🧛 VAMPIRIC (Hút máu)
- Mỗi khi chạm trúng người chơi → Elite **hồi 15% maxHP**
- Hiển thị: Hiệu ứng hạt đỏ bay từ người chơi về Elite
- **Phản ứng người chơi:** Phải giữ khoảng cách, không để bị chạm

#### 🔥 BERSERKER (Điên cuồng)
- Khi HP < 30% → Tốc độ **x2.5**, kích thước lớn hơn, chuyển màu đỏ rực
- **Phản ứng người chơi:** Phải finish nhanh, không để rơi vào phase "berserk" quá lâu

### 1.6. Hiển thị Visual

```
Quái thường:  [Sprite bình thường]
Quái Elite:   [Sprite + Viền vàng phát sáng + Icon affix nhỏ phía trên đầu]
                + Tên affix hiển thị khi di chuột (hoặc khi ở gần)
```

**Cách vẽ viền vàng (trong `draw()`):**
```java
if (isElite) {
    // Vẽ glow vàng
    g2d.setComposite(AlphaComposite.SRC_OVER, 0.4f);
    g2d.setColor(new Color(255, 215, 0)); // Gold
    g2d.fillOval(x - 8, y - 8, size + 16, size + 16);
    g2d.setComposite(AlphaComposite.SRC_OVER, 1.0f);
}
// Sau đó vẽ sprite bình thường lên trên
```

### 1.7. Tích hợp vào code

| File | Thay đổi |
|------|---------|
| `Enemy.java` | Thêm `isElite`, `eliteAffix`, `shieldHits`, `hasSplit` |
| `EntityManager.java` | Trong `spawnSafeEnemy()`: Roll elite, gắn affix, điều chỉnh stats |
| `EntityManager.java` | Trong vòng xử lý chết: Kiểm tra affix (EXPLOSIVE, SPLITTING) |
| Các class Enemy con | Trong `draw()`: Thêm viền vàng nếu `isElite` |
| Các class Enemy con | Override `takeDamage()`: Kiểm tra SHIELDED |

---

## 📐 Phần II: Nhiệm vụ Phụ (Wave Challenges)

### 2.1. Khái niệm

Mỗi wave (trừ boss wave) sẽ có **1 nhiệm vụ phụ ngẫu nhiên** hiển thị ở góc trên phải. Hoàn thành = nhận thưởng ngay. Thất bại = không mất gì, chỉ mất phần thưởng.

### 2.2. Danh sách Challenge

| # | Tên | Điều kiện | Phần thưởng |
|---|-----|-----------|-------------|
| 1 | **Speed Kill** | Giết 5 quái trong 4 giây | +50 EXP |
| 2 | **Untouchable** | Không bị hit trong toàn wave | +1 Heart (tạm thời, mất khi bị đánh) |
| 3 | **Elite Hunter** | Giết 1 Elite trong wave này | +2 Soul |
| 4 | **Combo Master** | Đạt combo 30+ trong wave | +100 EXP |
| 5 | **Marksman** | Đạt 5 critical hits liên tiếp | +Crit Damage x1.5 (10 giây) |
| 6 | **Survivor** | Sống sót với ≤2 HP khi wave kết thúc | Hồi đầy HP |
| 7 | **No Dash** | Không sử dụng Dash trong wave | +0.5 Speed (vĩnh viễn) |
| 8 | **Treasure Rush** | Thu thập 10 Gold/Soul trong wave | +1 Soul |

### 2.3. Quy tắc chọn Challenge

```
Wave 1-2:     Chọn từ pool dễ: {Speed Kill, Combo Master}
Wave 3-9:     Chọn từ pool trung bình: {tất cả trừ Elite Hunter}
Wave 10+:     Chọn từ full pool
Boss Wave:    KHÔNG có challenge (tập trung đánh boss)
```

- Mỗi wave chỉ 1 challenge
- Không lặp lại challenge của wave ngay trước
- Nếu wave không có Elite → không chọn "Elite Hunter"

### 2.4. Thiết kế Class mới: `WaveChallenge.java`

```java
public class WaveChallenge {
    public enum Type {
        SPEED_KILL, UNTOUCHABLE, ELITE_HUNTER, COMBO_MASTER,
        MARKSMAN, SURVIVOR, NO_DASH, TREASURE_RUSH
    }
    
    public Type type;
    public String description;
    public boolean isCompleted = false;
    public boolean isFailed = false;
    
    // Tracking variables
    public int killsInWindow = 0;      // Cho SPEED_KILL
    public long firstKillTime = 0;     // Cho SPEED_KILL
    public int critStreak = 0;         // Cho MARKSMAN
    public int itemsCollected = 0;     // Cho TREASURE_RUSH
    
    // Phương thức chính
    public void onEnemyKilled(boolean isElite, boolean isCrit, long currentTime) { ... }
    public void onPlayerHit() { ... }
    public void onPlayerDash() { ... }
    public void onItemCollected() { ... }
    public void onWaveEnd(Player player) { ... }
    
    // Áp dụng phần thưởng
    public void applyReward(Player player, UpgradeManager um, VFXManager vfx) { ... }
}
```

### 2.5. UI Hiển thị

```
┌─────────────────────────────┐
│  📋 CHALLENGE: Speed Kill   │  ← Góc trên phải, dưới minimap
│  Kill 5 enemies in 4s       │
│  Progress: ███░░ (3/5)      │  ← Thanh progress bar nhỏ
│  Reward: +50 EXP            │
└─────────────────────────────┘
```

**Khi hoàn thành:**
- Banner lớn giữa màn hình: "✅ CHALLENGE COMPLETE!" (màu vàng, 2s)
- Hiệu ứng: Confetti particles + Sound "levelup"
- Thanh challenge biến thành màu xanh lá

**Khi thất bại (nếu có):**
- Thanh challenge chuyển xám mờ dần
- Không hiện banner (tránh gây frustration)

### 2.6. Tích hợp vào code

| File | Thay đổi |
|------|---------|
| **`WaveChallenge.java`** (MỚI) | Class chính quản lý logic challenge |
| `EntityManager.java` | Khởi tạo challenge mới mỗi wave, gọi `onEnemyKilled()` |
| `PlayingState.java` | Gọi `onPlayerHit()`, `onPlayerDash()`, `onItemCollected()` |
| `VFXManager.java` | Thêm method `showChallengeComplete()` |
| `HUDManager.java` | Vẽ thanh challenge ở góc trên phải |

---

## 📐 Phần III: Điều chỉnh Spawn & Tốc độ quái

### 3.1. Spawn xa hơn

```java
// HIỆN TẠI: Quái spawn ngay sát viền camera (50px)
ey = camY - 50;   // Top
ey = camY + sh + 50; // Bottom
ex = camX - 50;   // Left
ex = camX + sw + 50; // Right

// ĐỀ XUẤT: Tăng khoảng cách spawn
int spawnOffset = 150 + rand.nextInt(100); // 150-250px (ngẫu nhiên)
ey = camY - spawnOffset;   // Top
ey = camY + sh + spawnOffset; // Bottom
ex = camX - spawnOffset;   // Left
ex = camX + sw + spawnOffset; // Right
```

> [!TIP]
> Spawn xa hơn giúp người chơi có thêm 2-3 giây nhìn thấy quái đang tiến lại trước khi phải react. Điều này cũng cho thời gian hoàn thành challenge mà không bị áp lực liên tục.

### 3.2. Giảm tốc độ quái

```java
// NormalEnemy — HIỆN TẠI vs ĐỀ XUẤT
Tier 1: 1.2f → 0.9f
Tier 2: 1.5f → 1.1f
Tier 3: 1.8f → 1.3f
Tier 4: 2.1f → 1.6f
Tier 5: 2.4f → 1.9f

// AssassinEnemy — HIỆN TẠI vs ĐỀ XUẤT
Tier 1: 1.5f → 1.2f
Tier 2: 1.8f → 1.4f
Tier 3: 2.1f → 1.7f
Tier 4: 2.5f → 2.0f
Tier 5: 3.0f → 2.4f
```

> [!NOTE]
> Giảm tốc độ ~25% giúp game không còn cảm giác "bị rush" liên tục. Kết hợp với spawn xa hơn, người chơi có thêm 4-5 giây "thở" mỗi wave, đủ để nhìn challenge, quyết định ưu tiên mục tiêu, và cảm nhận nhịp chiến đấu rõ ràng hơn.

---

## 🗓️ Lộ trình triển khai

### Phase 1 — Nền tảng (Làm trước)
1. Điều chỉnh spawn offset & tốc độ quái (5 phút)
2. Thêm trường `isElite` + `EliteAffix` vào `Enemy.java` (10 phút)
3. Logic spawn Elite trong `EntityManager.spawnSafeEnemy()` (15 phút)
4. Viền vàng + stats boost cho Elite (10 phút)
5. Test cơ bản

### Phase 2 — Hoàn thiện (Làm sau)
1. Implement từng Affix (mỗi cái ~10 phút)
2. Tạo `WaveChallenge.java` + logic 8 loại challenge
3. UI hiển thị challenge trên HUD
4. Tích hợp hook vào PlayingState/EntityManager
5. Test toàn bộ + cân chỉnh

> [!IMPORTANT]
> **Khuyến nghị:** Bắt đầu Phase 1 trước, test gameplay cảm giác. Nếu nhịp game đã tốt hơn → thêm Phase 2. Tránh code hết 1 lần rồi mới test vì khó debug.
