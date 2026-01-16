package com.example.viti_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PartComponentRequest {
    @NotBlank(message = "Tên phụ kiện không được để trống")
    private String name;

    private String partType; // VD: "RAM", "PIN", "MÀN HÌNH"

    @NotNull(message = "Nhà cung cấp là bắt buộc")
    private UUID supplierId;

    private String unit; // VD: "Cái", "Bộ"

    private BigDecimal unitPrice; // Giá nhập

    @Min(value = 0)
    private Integer minStock; // Tồn tối thiểu
}
