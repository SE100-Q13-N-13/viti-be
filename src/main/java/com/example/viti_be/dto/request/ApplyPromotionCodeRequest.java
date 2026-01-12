package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyPromotionCodeRequest {

    @NotBlank(message = "Promotion code is required")
    private String code;

    // Cart info để validate
    private UUID customerId; // Nullable cho guest
    private List<CartItemRequest> items;
}

