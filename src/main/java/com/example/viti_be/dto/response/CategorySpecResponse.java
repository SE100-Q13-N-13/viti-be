package com.example.viti_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpecResponse {
    private UUID id;
    private String specKey;
    private String specName;
    private Boolean isRequired;
    private String dataType;
    private List<String> options;
    
    // Fields for inheritance
    private Boolean isInherited;
    private UUID inheritedFromCategoryId;
    private String inheritedFromCategoryName;
}