package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@SuperBuilder
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationExpiration;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    @Access(AccessType.FIELD)
    private Boolean isActive = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "token_expiry_date")
    private Instant tokenExpiryDate;

    @Column(name = "is_first_login")
    @Builder.Default
    @Access(AccessType.FIELD)
    private Boolean isFirstLogin = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    public void setActive(Boolean active) {
        this.isActive = active;
    }

    public Boolean isActive() {
        return this.isActive;
    }

    public void setFirstLogin(Boolean firstLogin) {
        this.isFirstLogin = firstLogin;
    }

    public Boolean isFirstLogin() {
        return this.isFirstLogin;
    }


    public Set<String> getRoleNames() {
        Set<String> roles = new HashSet<>();
        for (UserRole ur : userRoles) {
            roles.add(ur.getRole().getName());
        }
        return roles;
    }
}