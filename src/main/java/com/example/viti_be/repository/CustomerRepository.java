package com.example.viti_be.repository;

import com.example.viti_be.model.Customer;
import com.example.viti_be.model.CustomerTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmail(String email);
    boolean existsByPhone(String phone);
    List<Customer> findByIsDeletedFalse();

    Optional<Customer> findByIdAndIsDeletedFalse(UUID id);
    Optional<Customer> findByUserIdAndIsDeletedFalse(UUID userId);

    /**
     * Đếm số lượng customers đang ở tier này
     */
    long countByTierAndIsDeletedFalse(CustomerTier tier);

    /**
     * Tìm tất cả customers theo tier
     */
    @Query("SELECT c FROM Customer c WHERE c.tier.id = :tierId AND c.isDeleted = false")
    List<Customer> findByTierId(@Param("tierId") UUID tierId);

    /**
     * Tìm customers có total_purchase >= threshold
     */
    @Query("SELECT c FROM Customer c WHERE c.totalPurchase >= :minPurchase AND c.isDeleted = false")
    List<Customer> findCustomersWithMinPurchase(@Param("minPurchase") java.math.BigDecimal minPurchase);

    Optional<Customer> findByUser_Id(UUID userId);
}
