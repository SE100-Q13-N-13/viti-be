package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPointsRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}
