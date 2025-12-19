package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.ProductSerialStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_serials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductSerialStatus status;

    @Column(name = "purchase_order_id")
    private UUID purchaseOrderId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "sold_date")
    private LocalDateTime soldDate;

    @Column(name = "warranty_expire_date")
    private LocalDateTime warrantyExpireDate;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ProductSerialStatus.AVAILABLE;
        }
    }
}
