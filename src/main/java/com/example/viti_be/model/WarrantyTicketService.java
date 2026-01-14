package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.WarrantyServiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DỊCH VỤ SỬA CHỮA TRONG PHIẾU
 */
@Entity
@Table(name = "warranty_ticket_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyTicketService extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private WarrantyTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_service_id", nullable = false)
    private RepairService repairService;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WarrantyServiceStatus status = WarrantyServiceStatus.PENDING;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice; // Giá áp dụng (copy từ repair_service)

    @Column(name = "additional_cost", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal additionalCost = BigDecimal.ZERO; // Phí phát sinh

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost; // = unitPrice + additionalCost

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Tính tổng chi phí service
     */
    public void calculateTotalCost() {
        BigDecimal base = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        BigDecimal additional = additionalCost != null ? additionalCost : BigDecimal.ZERO;
        this.totalCost = base.add(additional);
    }

    /**
     * Helper: Set price from RepairService
     */
    public void setPriceFromRepairService() {
        if (this.repairService != null) {
            this.unitPrice = this.repairService.getStandardPrice();
        }
    }
}