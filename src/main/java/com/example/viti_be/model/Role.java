package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name; // ROLE_ADMIN, ROLE_USER

    @Column(columnDefinition = "TEXT")
    private String description;
}