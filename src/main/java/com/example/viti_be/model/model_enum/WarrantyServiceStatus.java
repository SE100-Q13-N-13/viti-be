package com.example.viti_be.model.model_enum;

/**
 * Trạng thái dịch vụ trong ticket
 */
public enum WarrantyServiceStatus {
    /**
     * Chờ xử lý
     */
    PENDING,

    /**
     * Đang thực hiện
     */
    IN_PROGRESS,

    /**
     * Hoàn thành
     */
    COMPLETED,

    /**
     * Đã hủy
     */
    CANCELLED
}
