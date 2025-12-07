package com.example.viti_be.repository;

import com.example.viti_be.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);
    List<Customer> findByIsDeletedFalse();
    Optional<Customer> findByIdAndIsDeletedFalse(UUID id);
}
