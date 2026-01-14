package com.example.viti_be.repository;

import com.example.viti_be.model.WarrantyTicketService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarrantyTicketServiceRepository extends JpaRepository<WarrantyTicketService, UUID> {

    List<WarrantyTicketService> findByTicketIdAndIsDeletedFalse(UUID ticketId);

    void deleteByTicketId(UUID ticketId);
}