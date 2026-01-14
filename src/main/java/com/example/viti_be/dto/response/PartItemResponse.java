package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartItemResponse {

    private UUID id;
    private UUID partComponentId;
    private String partName;
    private String partType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalCost;
    private LocalDateTime usedAt;
    private String notes;
}
