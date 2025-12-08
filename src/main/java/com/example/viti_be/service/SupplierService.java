package com.example.viti_be.service;

import com.example.viti_be.dto.request.SupplierRequest;
import com.example.viti_be.model.Supplier;

import java.util.List;
import java.util.UUID;

public interface SupplierService {
    Supplier createSupplier(SupplierRequest request);
    Supplier getSupplierById(UUID id);
    List<Supplier> getAllSuppliers();
    Supplier updateSupplier(UUID id, SupplierRequest updatedSupplier);
    void deleteSupplier(UUID id);
}
