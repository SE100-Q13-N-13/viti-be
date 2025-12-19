package com.example.viti_be.model;

import com.example.viti_be.model.composite_key.UserRoleId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @EmbeddedId
    private UserRoleId id = new UserRoleId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER) // Eager => load User lấy luôn Role cho Security
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    // private LocalDateTime assignedAt;
}