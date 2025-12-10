package com.example.viti_be.repository;

import com.example.viti_be.model.CustomerTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerTierRepository extends JpaRepository<CustomerTier, UUID> {
    List<CustomerTier> findByIsDeletedFalse();
    Optional<CustomerTier> findByIdAndIsDeletedFalse(UUID id);
    Optional<CustomerTier> findByName(String name);
}
