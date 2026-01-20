package com.example.viti_be.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopProductProjection {
    UUID getProductVariantId();
    String getProductName();
    String getVariantName();
    String getSku();
    Integer getQuantitySold();
    BigDecimal getTotalRevenue();
}