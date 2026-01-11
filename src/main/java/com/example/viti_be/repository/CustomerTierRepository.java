package com.example.viti_be.repository;

import com.example.viti_be.model.CustomerTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerTierRepository extends JpaRepository<CustomerTier, UUID> {
    List<CustomerTier> findByIsDeletedFalse();
    Optional<CustomerTier> findByIdAndIsDeletedFalse(UUID id);
    Optional<CustomerTier> findByName(String name);

    /**
     * Tìm tier theo tên (REGULAR, LOYAL, GOLD, PLATINUM)
     */
    Optional<CustomerTier> findByNameAndIsDeletedFalse(String name);

    /**
     * Tìm tier phù hợp dựa trên số điểm
     * Lấy tier có min_point lớn nhất mà <= điểm hiện tại
     */
    @Query("SELECT t FROM CustomerTier t " +
            "WHERE t.minPoint <= :points " +
            "AND t.isDeleted = false " +
            "AND t.status = 'ACTIVE' " +
            "ORDER BY t.minPoint DESC")
    List<CustomerTier> findTiersByPoints(@Param("points") Integer points);

    /**
     * Lấy tất cả tiers đang active, sắp xếp theo minPoint
     */
    @Query("SELECT t FROM CustomerTier t " +
            "WHERE t.isDeleted = false " +
            "AND t.status = 'ACTIVE' " +
            "ORDER BY t.minPoint ASC")
    List<CustomerTier> findAllActiveTiers();

    /**
     * Tìm tier tiếp theo (tier có minPoint lớn hơn số điểm hiện tại, nhỏ nhất)
     */
    @Query("SELECT t FROM CustomerTier t " +
            "WHERE t.minPoint > :points " +
            "AND t.isDeleted = false " +
            "AND t.status = 'ACTIVE' " +
            "ORDER BY t.minPoint ASC")
    List<CustomerTier> findNextTier(@Param("points") Integer points);
}
