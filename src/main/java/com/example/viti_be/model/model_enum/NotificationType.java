package com.example.viti_be.model.model_enum;

/**
 * Enum định nghĩa các loại notification trong hệ thống.
 * Dễ dàng mở rộng bằng cách thêm các giá trị mới.
 */
public enum NotificationType {
    // Order related
    ORDER_NEW,
    ORDER_CONFIRMED,
    ORDER_CANCELLED,
    ORDER_COMPLETED,
    
    // Inventory related (for future use)
    INVENTORY_LOW_STOCK,
    INVENTORY_OUT_OF_STOCK,
    
    // Customer related (for future use)
    CUSTOMER_NEW,
    
    // System related (for future use)
    SYSTEM_ALERT
}
