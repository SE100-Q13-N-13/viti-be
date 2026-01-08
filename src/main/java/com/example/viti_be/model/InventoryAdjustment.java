package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.InventoryAdjustmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustment extends BaseEntity {

    @Column(name = "reference_code", unique = true)
    private UUID referenceCode;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InventoryAdjustmentStatus status;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @OneToMany(mappedBy = "inventoryAdjustment", cascade = CascadeType.ALL)
    @Builder.Default
    private List<StockTransaction> stockTransactions = new ArrayList<>();
}
