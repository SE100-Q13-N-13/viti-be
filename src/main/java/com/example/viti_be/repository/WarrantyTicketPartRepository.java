package com.example.viti_be.repository;

import com.example.viti_be.model.WarrantyTicketPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarrantyTicketPartRepository extends JpaRepository<WarrantyTicketPart, UUID> {

    List<WarrantyTicketPart> findByTicketIdAndIsDeletedFalse(UUID ticketId);

    void deleteByTicketId(UUID ticketId);
}