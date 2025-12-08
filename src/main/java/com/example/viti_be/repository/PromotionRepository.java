package com.example.viti_be.repository;

import com.example.viti_be.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByIdAndIsDeletedFalse(UUID id);
}
