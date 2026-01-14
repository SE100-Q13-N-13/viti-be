package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignTechnicianRequest {

    @NotNull(message = "New technician ID is required")
    private UUID newTechnicianId;

    @NotBlank(message = "Reason for reassignment is required")
    private String reason;
}
