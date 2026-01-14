package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.WarrantyTicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeTicketStatusRequest {

    @NotNull(message = "New status is required")
    private WarrantyTicketStatus newStatus;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
