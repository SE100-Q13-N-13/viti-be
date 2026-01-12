package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CategorySpecRequest {
    @NotNull(message = "Category ID là bắt buộc")
    private UUID categoryId;

    @NotBlank(message = "Key thông số không được để trống")
    private String specKey; // VD: screen_size

    @NotBlank(message = "Tên hiển thị thông số không được để trống")
    private String specName; // VD: Kích thước màn hình
    private Boolean isRequired;
    private String dataType;
    private List<String> options; // FE gửi lên List, BE sẽ convert sang JSON String để lưu
}