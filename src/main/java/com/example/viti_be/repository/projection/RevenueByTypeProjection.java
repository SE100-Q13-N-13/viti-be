package com.example.viti_be.repository.projection;

import com.example.viti_be.model.model_enum.OrderType;
import java.math.BigDecimal;

public interface RevenueByTypeProjection {
    OrderType getOrderType();
    BigDecimal getTotalRevenue();
    Long getOrderCount();
}