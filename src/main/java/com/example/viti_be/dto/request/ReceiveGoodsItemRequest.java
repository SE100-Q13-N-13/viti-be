package com.example.viti_be.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ReceiveGoodsItemRequest {
    
    private UUID productVariantId;

    private UUID partComponentId;

    @NotNull(message = "Quantity received is required")
    @Min(value = 0, message = "Quantity received must be non-negative")
    private Integer quantityReceived;
    
    // Optional: Only for serial-managed products
    private List<String> serialNumbers = new ArrayList<>();

    @AssertTrue(message = "Must specify either Product Variant or Part Component")
    public boolean isValidItem() {
        return (productVariantId != null && partComponentId == null) ||
                (productVariantId == null && partComponentId != null);
    }
}
