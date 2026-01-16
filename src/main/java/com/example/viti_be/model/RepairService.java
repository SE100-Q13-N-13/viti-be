package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * LOẠI DỊCH VỤ SỬA CHỮA (Master data)
 */
@Entity
@Table(name = "repair_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairService extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name; // VD: "Thay màn hình", "Sửa nguồn"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "standard_price", precision = 15, scale = 2)
    private BigDecimal standardPrice; // Giá công tiêu chuẩn

    @Column(name = "estimated_duration", length = 50)
    private String estimatedDuration; // VD: "2-3 giờ"

    @Column(name = "category", length = 100)
    private String category; // VD: "Screen", "Battery", "Motherboard"

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}