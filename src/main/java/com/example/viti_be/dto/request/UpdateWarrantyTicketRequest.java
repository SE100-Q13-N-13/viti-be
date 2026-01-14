package com.example.viti_be.dto.request;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWarrantyTicketRequest {

    private String problemDescription;

    private String accessories;

    private UUID technicianId;

    private LocalDateTime expectedReturnDate;

    private String notes;
}
