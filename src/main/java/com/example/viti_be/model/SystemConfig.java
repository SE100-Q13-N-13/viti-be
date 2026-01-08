package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig extends BaseEntity {

    @Column(name = "config_key", unique = true, length = 100, nullable = false)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "data_type", length = 20)
    private String dataType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;
}
