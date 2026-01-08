package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId; // User ID who performed the action

    @Column(name = "module", nullable = false)
    private String module; // Module name: PURCHASE_ORDER, INVENTORY, etc.

    @Column(name = "action", nullable = false)
    private String action; // Action: CREATE, UPDATE, DELETE, RECEIVE, etc.

    @Column(name = "resource_id")
    private String resourceId; // ID of the affected resource

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // JSON representation of old value

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // JSON representation of new value

    @Column(name = "status")
    private String status; // SUCCESS, FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
