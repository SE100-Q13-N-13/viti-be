package com.example.viti_be.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VariantRequest {
    @NotNull(message = "Product ID là bắt buộc khi tạo variant")
    private UUID productId;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotBlank(message = "Tên biến thể không được để trống")
    private String variantName;

    @DecimalMin(value = "0.0", message = "Giá bán phải lớn hơn 0")
    private BigDecimal sellingPrice;

    // JSON String chứa thuộc tính riêng (Màu, Size...)
    private String variantSpecs;
}