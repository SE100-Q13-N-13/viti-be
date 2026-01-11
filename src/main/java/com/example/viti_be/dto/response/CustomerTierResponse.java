package com.example.viti_be.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTierResponse {
    private UUID id;
    private String name;
    private Integer minPoint;
    private BigDecimal discountRate;
    private String description;
    private String status;
    private Integer customerCount; // Số lượng customers đang ở tier này
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}