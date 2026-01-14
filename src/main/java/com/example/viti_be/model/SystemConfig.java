package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig extends BaseEntity {

    @Column(name = "config_key", unique = true, length = 100, nullable = false)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;

    @Column(name = "data_type", length = 20)
    @Builder.Default
    private String dataType = "STRING"; // STRING, INTEGER, DECIMAL, BOOLEAN

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_encrypted")
    @Builder.Default
    private Boolean isEncrypted = false;
}
