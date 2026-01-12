package com.example.viti_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull(message = "Danh mục là bắt buộc")
    private UUID categoryId;

    @NotNull(message = "Nhà cung cấp là bắt buộc")
    private UUID supplierId;

    private String description;

    @Min(value = 0)
    private Integer minStockThreshold;

    @Min(value = 0)
    private Integer warrantyPeriod;
    private String commonSpecs; // JSON String từ FE gửi lên
}