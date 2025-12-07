package com.example.viti_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigResponse {
    private UUID id;
    private String configKey;
    private String configValue;
    private String dataType;
    private String description;
    private Boolean isEncrypted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
