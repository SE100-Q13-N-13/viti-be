package com.example.viti_be.repository;

import com.example.viti_be.model.WarrantyTicket;
import com.example.viti_be.model.model_enum.WarrantyTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarrantyTicketRepository extends JpaRepository<WarrantyTicket, UUID> {

    // Find by ticket number
    Optional<WarrantyTicket> findByTicketNumberAndIsDeletedFalse(String ticketNumber);

    // Find all active tickets
    List<WarrantyTicket> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    // Find by serial number
    @Query("SELECT wt FROM WarrantyTicket wt WHERE wt.productSerial.serialNumber = :serialNumber " +
            "AND wt.isDeleted = false ORDER BY wt.createdAt DESC")
    List<WarrantyTicket> findBySerialNumber(@Param("serialNumber") String serialNumber);

    // Find by customer
    @Query("SELECT wt FROM WarrantyTicket wt WHERE wt.customer.id = :customerId " +
            "AND wt.isDeleted = false ORDER BY wt.createdAt DESC")
    List<WarrantyTicket> findByCustomerId(@Param("customerId") UUID customerId);

    // Find by status
    List<WarrantyTicket> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(WarrantyTicketStatus status);

    // Find by technician
    @Query("SELECT wt FROM WarrantyTicket wt WHERE wt.technician.id = :technicianId " +
            "AND wt.isDeleted = false ORDER BY wt.createdAt DESC")
    List<WarrantyTicket> findByTechnicianId(@Param("technicianId") UUID technicianId);

    // Find tickets by date range
    @Query("SELECT wt FROM WarrantyTicket wt WHERE wt.receivedDate BETWEEN :startDate AND :endDate " +
            "AND wt.isDeleted = false ORDER BY wt.receivedDate DESC")
    List<WarrantyTicket> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Count by status (for dashboard)
    @Query("SELECT COUNT(wt) FROM WarrantyTicket wt WHERE wt.status = :status AND wt.isDeleted = false")
    long countByStatus(@Param("status") WarrantyTicketStatus status);

    // Find overdue tickets (expected return date passed but not returned)
    @Query("SELECT wt FROM WarrantyTicket wt WHERE wt.expectedReturnDate < :now " +
            "AND wt.status NOT IN ('RETURNED', 'CANCELLED') AND wt.isDeleted = false")
    List<WarrantyTicket> findOverdueTickets(@Param("now") LocalDateTime now);

    // Search tickets (by ticket number, serial, customer name/phone)
    @Query("SELECT wt FROM WarrantyTicket wt WHERE " +
            "(LOWER(wt.ticketNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(wt.productSerial.serialNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(wt.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(wt.customerPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND wt.isDeleted = false ORDER BY wt.createdAt DESC")
    List<WarrantyTicket> searchTickets(@Param("keyword") String keyword);
}
