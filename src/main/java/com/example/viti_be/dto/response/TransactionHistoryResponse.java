package com.example.viti_be.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryResponse {
    private LoyaltyPointResponse currentStatus;
    private java.util.List<LoyaltyPointTransactionResponse> transactions;
    private Integer totalEarnedThisMonth;
    private Integer totalRedeemedThisMonth;
}