package com.example.viti_be.repository.projection;

import java.util.UUID;

public interface FastMovingProductProjection {
    UUID getProductVariantId();
    String getProductName();
    String getVariantName();
    String getSku();
    Integer getQuantitySold();
    Integer getDaysSinceFirstSale();
    Double getAverageDailySales(); // quantity_sold / days_since_first_sale
}