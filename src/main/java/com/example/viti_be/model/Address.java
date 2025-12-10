package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String street;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commune_code")
    private Commune commune;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code")
    private Province province;

    @Column(length = 20)
    private String type;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "postal_code", length = 10)
    private String postalCode;
}
