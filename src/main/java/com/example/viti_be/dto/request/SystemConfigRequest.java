package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemConfigRequest {

    @NotBlank(message = "Config key is required")
    private String configKey;

    private String configValue;

    private String dataType;

    private String description;

    private Boolean isEncrypted = false;
}
