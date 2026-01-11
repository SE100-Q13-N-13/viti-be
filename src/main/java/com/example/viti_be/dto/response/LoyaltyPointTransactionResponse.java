package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.TransactionType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPointTransactionResponse {
    private UUID id;
    private TransactionType transactionType;
    private Integer pointsChange;
    private Integer pointsTotalAfter;
    private Integer pointsAvailableAfter;
    private String reason;
    private UUID orderId;
    private String orderNumber;
    private String performedByName;
    private LocalDateTime createdAt;
}
