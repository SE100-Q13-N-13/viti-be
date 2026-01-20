package com.example.viti_be.repository.projection;

import java.util.UUID;

public interface InventoryProjection {
    UUID getProductVariantId();
    String getProductName();
    String getVariantName();
    String getSku();
    Integer getQuantityPhysical();
    Integer getQuantityReserved();
    Integer getQuantityAvailable();
    Integer getMinThreshold();
    Boolean getIsLowStock(); // quantity_available < min_threshold
}