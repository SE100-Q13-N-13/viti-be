package com.example.viti_be.repository;

import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    Optional<User> findByIdAndIsDeletedFalse(UUID id);

    List<User> findByStatusAndCreatedAtBefore(UserStatus status, LocalDateTime createdAt);
}