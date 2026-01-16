package com.example.viti_be.dto.response;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CategoryResponse {
    private UUID id;
    private String name;
    private String imageUrl;
    private String description;

    private UUID parentId;
    private String parentName;
    private List<CategorySpecResponse> specs;

    private List<UUID> relatedCategoryIds;
}