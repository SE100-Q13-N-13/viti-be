package com.example.viti_be.dto.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CategorySpecRequest {
    private UUID categoryId; // Cần khi tạo mới spec riêng lẻ
    private String specKey;
    private String specName;
    private Boolean isRequired;
    private String dataType;
    private List<String> options; // FE gửi lên List, BE sẽ convert sang JSON String để lưu
}