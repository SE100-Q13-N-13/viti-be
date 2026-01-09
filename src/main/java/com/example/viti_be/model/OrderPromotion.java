package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "order_promotions")
@Data
public class OrderPromotion {

    // CLASS PROTOTYPE FOR COMPILING, REPLACE WITH ACTUAL CLASS

    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order; // Order sở hữu relationship này

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion; // Chỉ cần class Promotion có @Entity và @Id là được

    private Double discountAmount; // Số tiền được giảm cụ thể cho đơn này
}
