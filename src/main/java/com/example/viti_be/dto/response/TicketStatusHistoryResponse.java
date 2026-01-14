package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.WarrantyTicketStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStatusHistoryResponse {

    private UUID id;
    private WarrantyTicketStatus oldStatus;
    private WarrantyTicketStatus newStatus;
    private String reason;
    private String actorName;
    private LocalDateTime changedAt;
    private String notes;
}
