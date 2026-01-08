package com.example.viti_be.repository;

import com.example.viti_be.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {
    Optional<SystemConfig> findByConfigKey(String configKey);
    List<SystemConfig> findByIsDeletedFalse();
    Optional<SystemConfig> findByIdAndIsDeletedFalse(UUID id);
    boolean existsByConfigKey(String configKey);
}
