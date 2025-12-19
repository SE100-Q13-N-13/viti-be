package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true)
    private String email;

    private String phone;

    private String avatar;

    @Column(name = "status")
    private String status; // ACTIVE, SUSPENDED...

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationExpiration;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "token_expiry_date")
    private Instant tokenExpiryDate;

    @Column(name = "is_first_login")
    private Boolean isFirstLogin = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserRole> userRoles = new HashSet<>();

    public Set<String> getRoleNames() {
        Set<String> roles = new HashSet<>();
        for (UserRole ur : userRoles) {
            roles.add(ur.getRole().getName());
        }
        return roles;
    }
}