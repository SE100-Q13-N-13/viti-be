package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.SupplierRequest;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.Supplier;
import com.example.viti_be.repository.SupplierRepository;
import com.example.viti_be.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
public class SupplierServiceImpl implements SupplierService {
    @Autowired
    SupplierRepository repo;
    @Override
    public Supplier createSupplier(SupplierRequest request){
        Supplier supplier = new Supplier();

        supplier.setName(request.getName());
        supplier.setContact_name(request.getContact_name());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());

        return repo.save(supplier);
    }
    @Override
    public Supplier getSupplierById(UUID id){
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Can not find Supplier by ID: " + id));
    }
    @Override
    public PageResponse<Supplier> getAllSuppliers(Pageable pageable){
        Page<Supplier> page = repo.findAll(pageable);
        return PageResponse.from(page, Function.identity());
    }

    @Override
    public Supplier updateSupplier(UUID id, SupplierRequest request){
        Supplier supplier = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not exist with ID: " + id));

        supplier.setName(request.getName());
        if (request.getContact_name() != null) { supplier.setContact_name(request.getContact_name()); }
        if (request.getPhone() != null) { supplier.setPhone(request.getPhone()); }
        if (request.getAddress() != null) { supplier.setAddress(request.getAddress()); }
        if (request.getEmail() != null) { supplier.setEmail(request.getEmail()); }

        return repo.save(supplier);
    }

    @Override
    public void deleteSupplier(UUID id){
        repo.deleteById(id);
    }
}
