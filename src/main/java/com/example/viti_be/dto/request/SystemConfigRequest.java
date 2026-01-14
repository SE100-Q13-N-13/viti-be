package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SystemConfigRequest {

    @NotBlank(message = "Config key is required")
    private String configKey;

    @NotBlank(message = "Config value is required")
    private String configValue;

    @Builder.Default
    private String dataType = "STRING";

    private String description;

    @Builder.Default
    private Boolean isEncrypted = false;
}
