package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.AuthProvider;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProviderDTO {
    private AuthProvider provider;
    private Boolean isPrimary;
    private LocalDateTime linkedAt;
}