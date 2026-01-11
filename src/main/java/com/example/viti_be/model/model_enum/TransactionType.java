package com.example.viti_be.model.model_enum;

/**
 * Loại giao dịch điểm tích lũy
 */
public enum TransactionType {
    /**
     * Tích điểm từ đơn hàng
     */
    EARN,

    /**
     * Sử dụng điểm khi mua hàng
     */
    REDEEM,

    /**
     * Điều chỉnh thủ công bởi Admin
     */
    MANUAL_ADJUST,

    /**
     * Reset điểm định kỳ
     */
    RESET
}