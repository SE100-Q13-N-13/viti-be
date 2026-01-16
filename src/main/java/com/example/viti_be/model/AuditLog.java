package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false)
    private AuditModule module; // Module name: PURCHASE_ORDER, INVENTORY, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action; // Action: CREATE, UPDATE, DELETE, RECEIVE, etc.

    @Column(name = "resource_id")
    private String resourceId; // ID of the affected resource

    @Column(name = "resource_type")
    private String resourceType;

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
