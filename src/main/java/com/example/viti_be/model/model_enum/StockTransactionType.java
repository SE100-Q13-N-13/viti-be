package com.example.viti_be.model.model_enum;

public enum StockTransactionType {
    STOCK_IN,       // Nhập kho từ PO
    STOCK_OUT,      // Xuất kho (bán hàng)
    ADJUSTMENT,     // Điều chỉnh kho
    RETURN,         // Trả hàng
    TRANSFER        // Chuyển kho
}
