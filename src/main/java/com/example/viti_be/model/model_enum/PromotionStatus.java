package com.example.viti_be.model.model_enum;

/**
 * Trạng thái khuyến mãi
 */
public enum PromotionStatus {
    /**
     * Chưa bắt đầu (start_date > now)
     */
    SCHEDULED,

    /**
     * Đang hoạt động
     */
    ACTIVE,

    /**
     * Tạm ngưng
     */
    INACTIVE,

    /**
     * Hết hạn (end_date < now)
     */
    EXPIRED
}