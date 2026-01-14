package com.example.viti_be.model.model_enum;

/**
 * Trạng thái phiếu bảo hành/sửa chữa
 */
public enum WarrantyTicketStatus {
    /**
     * Tiếp nhận - Vừa tạo ticket, chưa bắt đầu xử lý
     */
    RECEIVED,

    /**
     * Đang xử lý - Technician đang sửa chữa
     */
    PROCESSING,

    /**
     * Chờ linh kiện - Thiếu parts, đang chờ nhập hàng
     */
    WAITING_FOR_PARTS,

    /**
     * Hoàn thành - Sửa xong, chờ khách đến lấy
     */
    COMPLETED,

    /**
     * Đã trả khách - Khách đã nhận máy
     */
    RETURNED,

    /**
     * Đã hủy - Khách hủy sửa hoặc không sửa được
     */
    CANCELLED
}