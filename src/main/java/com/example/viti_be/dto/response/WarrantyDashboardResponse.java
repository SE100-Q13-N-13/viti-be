package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyDashboardResponse {

    // ========== TICKET COUNTS ==========
    private Long totalTickets;
    private Long receivedCount;
    private Long processingCount;
    private Long waitingForPartsCount;
    private Long completedCount;
    private Long returnedCount;
    private Long cancelledCount;
    private Long overdueCount;

    // ========== FINANCIAL ==========
    private BigDecimal totalRevenue;          // Tổng doanh thu từ warranty
    private BigDecimal totalServiceRevenue;   // Doanh thu từ services
    private BigDecimal totalPartRevenue;      // Doanh thu từ parts

    // ========== TIME-BASED ==========
    private Long ticketsThisMonth;
    private Long ticketsLastMonth;
    private Double avgRepairDays;             // Trung bình số ngày sửa

    // ========== TECHNICIAN STATS ==========
    private Map<String, Long> ticketsByTechnician;  // Map<TechnicianName, TicketCount>

    // ========== STATUS BREAKDOWN ==========
    private Map<String, Long> ticketsByStatus;      // Map<Status, Count>

    // ========== PARTS ALERTS ==========
    private Long lowStockPartsCount;
}