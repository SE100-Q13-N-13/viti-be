package com.example.viti_be.model.model_enum;

/**
 * Trạng thái serial (update existing enum nếu chưa có DEFECTIVE)
 */
public enum ProductSerialStatus {
    AVAILABLE,   // Sẵn bán
    SOLD,        // Đã bán
    DISPLAY,     // Trưng bày
    WARRANTY,    // Đang bảo hành/sửa chữa
    DEFECTIVE    // Hỏng không sửa được (cho replacement)
}
