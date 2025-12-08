package com.example.viti_be.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CategoryRequest {
    private String name;
    private String description;
    private UUID parentId; // Có thể null nếu là danh mục gốc
    private List<UUID> relatedCategoryIds;
}