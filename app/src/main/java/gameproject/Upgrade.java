package gameproject;

public enum Upgrade {
    // Nâng cấp chỉ số (Normal)
    SHIELD("Thêm Trái Tim (+1 HP tối đa)", false),
    DAMAGE("Sức Mạnh (Sát thương +5)", false),
    FIRE_RATE("Nòng Súng Nhẹ (Bắn nhanh hơn)", false),
    MOVE_SPEED("Giày Nhanh Nhẹn (Tốc độ chạy +0.5)", false),
    DASH_COOLDOWN("Lõi Động Cơ (Hồi Lướt nhanh hơn)", false),
    BULLET_SPEED("Đạn Khí Động Học (Tốc độ đạn +20%)", false),

    // Nâng cấp Đột phá (Breakthrough) - Tổng cộng 5 loại
    CHAIN_LIGHTNING("Sét Dây Chuyền (Đạn nảy mục tiêu)", true),
    TRAIL_OF_FIRE("Tàn Tích Lửa (Lướt để lại lửa)", true),
    ORBITING_ORBS("Vệ Tinh Phá Hoại (Quả cầu xoay)", true),
    EXPLOSIVE_CORPSE("Bom Máu (Quái nổ khi chết)", true),
    FROST_AURA("Vùng Đất Lạnh (Làm chậm quái gần)", true);

    public final String description;
    public final boolean isBreakthrough;

    Upgrade(String description, boolean isBreakthrough) {
        this.description = description;
        this.isBreakthrough = isBreakthrough;
    }
}