package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.StockTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "stock_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @Column(name = "part_component_id")
    private UUID partComponentId; // For part components

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_adjustment_id")
    private InventoryAdjustment inventoryAdjustment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private StockTransactionType type;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "quantity_before")
    private Integer quantityBefore;

    @Column(name = "quantity_after")
    private Integer quantityAfter;

    @Column(name = "reason")
    private String reason;

    @Column(name = "reference_id")
    private String referenceId; // PO Number, Order Number, etc.

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
