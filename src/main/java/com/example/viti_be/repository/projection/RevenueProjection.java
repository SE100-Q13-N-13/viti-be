package com.example.viti_be.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RevenueProjection {
    LocalDate getPeriod();
    BigDecimal getTotalRevenue();
    Long getOrderCount();
}