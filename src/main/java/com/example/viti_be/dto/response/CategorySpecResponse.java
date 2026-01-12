package com.example.viti_be.dto.response;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CategorySpecResponse {
    private UUID id;
    private String specKey;
    private String specName;
    private Boolean isRequired;
    private String dataType;

    private List<String> options;
}