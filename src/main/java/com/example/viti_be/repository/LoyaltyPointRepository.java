package com.example.viti_be.repository;

import com.example.viti_be.model.LoyaltyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, UUID> {
    Optional<LoyaltyPoint> findByCustomerId(UUID customerId);
}
