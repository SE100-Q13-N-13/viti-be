package com.example.viti_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response chứa tất cả filter options cho variants
 * Dùng để FE tạo UI filter động
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantFilterOptionsResponse {

    /**
     * Map của spec_key -> danh sách giá trị unique
     * VD: {"color": ["Đỏ", "Xanh"], "ram": ["8GB", "16GB"]}
     */
    private Map<String, List<String>> filters;

    /**
     * Metadata chi tiết của từng spec (optional, nếu FE cần)
     */
    private Map<String, SpecMetadata> metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecMetadata {
        private String specName;  // "Màu sắc"
        private String dataType;  // "SELECT", "TEXT", "NUMBER"
        private Boolean isRequired;
    }
}