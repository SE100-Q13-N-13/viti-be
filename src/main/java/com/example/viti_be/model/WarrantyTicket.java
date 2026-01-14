package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.WarrantyTicketStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PHIẾU BẢO HÀNH / SỬA CHỮA
 */
@Entity
@Table(name = "warranty_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WarrantyTicket extends BaseEntity {

    @Column(name = "ticket_number", unique = true, nullable = false, length = 50)
    private String ticketNumber; // Format: WRT-20250113-12345

    // ========== PRODUCT INFO ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_serial_id", nullable = false)
    private ProductSerial productSerial;

    // ========== CUSTOMER INFO ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer; // Nullable nếu guest

    @Column(name = "customer_name", length = 100)
    private String customerName; // Backup nếu không có customer entity

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    // ========== TECHNICIAN INFO ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician; // Current assigned technician

    // ========== PROBLEM DESCRIPTION ==========

    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    @Column(name = "accessories", columnDefinition = "TEXT")
    private String accessories; // Phụ kiện đi kèm (sạc, chuột, ...)

    // ========== WARRANTY INFO ==========

    @Column(name = "is_under_warranty")
    private Boolean isUnderWarranty; // Còn BH hay không

    @Column(name = "warranty_expire_date")
    private LocalDateTime warrantyExpireDate; // Ngày hết BH (từ serial)

    // ========== STATUS ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WarrantyTicketStatus status;

    // ========== DATES ==========

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Column(name = "expected_return_date")
    private LocalDateTime expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDateTime actualReturnDate;

    // ========== PRICING ==========

    @Column(name = "total_service_cost", precision = 15, scale = 2)
    private BigDecimal totalServiceCost; // Tổng tiền công

    @Column(name = "total_part_cost", precision = 15, scale = 2)
    private BigDecimal totalPartCost; // Tổng tiền linh kiện

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost; // Tổng cộng

    // ========== RELATIONSHIPS ==========

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WarrantyTicketService> services = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WarrantyTicketPart> parts = new ArrayList<>();

    // ========== NOTES ==========

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // ========== HELPER METHODS ==========

    /**
     * Tính tổng chi phí
     */
    public void calculateTotalCost() {
        BigDecimal serviceCost = services.stream()
                .map(WarrantyTicketService::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal partCost = parts.stream()
                .map(WarrantyTicketPart::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalServiceCost = serviceCost;
        this.totalPartCost = partCost;
        this.totalCost = serviceCost.add(partCost);
    }

    /**
     * Check nếu có thể đổi status
     */
    public boolean canTransitionTo(WarrantyTicketStatus newStatus) {
        if (this.status == WarrantyTicketStatus.RETURNED ||
                this.status == WarrantyTicketStatus.CANCELLED) {
            return false; // Terminal states
        }
        return true;
    }

    /**
     * Add service
     */
    public void addService(WarrantyTicketService service) {
        services.add(service);
        service.setTicket(this);
    }

    /**
     * Add part
     */
    public void addPart(WarrantyTicketPart part) {
        parts.add(part);
        part.setTicket(this);
    }
}