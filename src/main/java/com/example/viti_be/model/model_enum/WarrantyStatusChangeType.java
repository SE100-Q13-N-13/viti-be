package com.example.viti_be.model.model_enum;

/**
 * Loại thay đổi trạng thái (cho audit log)
 */
public enum WarrantyStatusChangeType {
    START_REPAIR,      // Bắt đầu sửa
    WAIT_FOR_PARTS,    // Chờ linh kiện
    RESUME_REPAIR,     // Tiếp tục sửa
    COMPLETE_REPAIR,   // Hoàn thành
    RETURN_TO_CUSTOMER, // Trả khách
    CANCEL_TICKET,     // Hủy
    REASSIGN_TECHNICIAN // Đổi thợ
}
