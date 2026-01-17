package com.example.viti_be.repository;

import com.example.viti_be.model.AuditLog;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    List<AuditLog> findAllByModule(AuditModule module);
    
    List<AuditLog> findAllByActorId(UUID actorId);
    
    List<AuditLog> findAllByResourceId(String resourceId);
    
    @Query("SELECT a FROM AuditLog a WHERE a.module = :module AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findByModuleAndDateRange(
            @Param("module") AuditModule module,
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:#{#startDate == null} = true OR a.createdAt >= :startDate) AND " +
           "(:#{#endDate == null} = true OR a.createdAt <= :endDate) AND " +
           "(:#{#actorId == null} = true OR a.actorId = :actorId) AND " +
           "(:#{#action == null} = true OR a.action = :action) AND " +
           "(:#{#module == null} = true OR a.module = :module)")
    Page<AuditLog> findAllWithFilters(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("actorId") UUID actorId,
            @Param("action") AuditAction action,
            @Param("module") AuditModule module,
            Pageable pageable);
}
