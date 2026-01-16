package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * LINH KIỆN SỬ DỤNG TRONG PHIẾU
 */
@Entity
@Table(name = "warranty_ticket_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyTicketPart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private WarrantyTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_component_id", nullable = false)
    private PartComponent partComponent;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice; // Giá bán cho khách (có markup)

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost; // = quantity * unitPrice

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Tính tổng chi phí part
     */
    public void calculateTotalCost() {
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        int qty = quantity != null ? quantity : 0;
        this.totalCost = price.multiply(BigDecimal.valueOf(qty));
    }

    /**
     * Helper: Set price from PartComponent with markup
     */
    public void setPriceFromPartComponent(BigDecimal markupPercent) {
        if (this.partComponent != null && this.partComponent.getUnitPrice() != null) {
            BigDecimal costPrice = this.partComponent.getUnitPrice();

            // Apply markup
            if (markupPercent != null && markupPercent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal markup = costPrice.multiply(markupPercent)
                        .divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
                this.unitPrice = costPrice.add(markup);
            } else {
                this.unitPrice = costPrice;
            }
        }
    }
}
