package com.example.viti_be.repository;

import com.example.viti_be.model.Order;
import com.example.viti_be.model.model_enum.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.status = :status " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByStatusAndDateRange(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Count orders by customer for tier calculation
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId " +
            "AND o.status = 'COMPLETED'")
    Long countCompletedOrdersByCustomer(@Param("customerId") UUID customerId);
}
