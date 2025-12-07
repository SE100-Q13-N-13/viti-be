package com.example.viti_be.service.impl;

import com.example.viti_be.model.Supplier;
import com.example.viti_be.repository.SupplierRepository;
import com.example.viti_be.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierServiceImpl implements SupplierService {
    @Autowired
    SupplierRepository repo;
    @Override
    public Supplier createSupplier(Supplier supplier){
        return repo.save(supplier);
    }
    @Override
    public Supplier getSupplierById(UUID id){
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Can not find Supplier by ID: " + id));
    }
    @Override
    public List<Supplier> getAllSuppliers(){
        return repo.findAll();
    }

    @Override
    public Supplier updateSupplier(UUID id, Supplier updatedSupplier){
        return repo.save(updatedSupplier);
    }

    @Override
    public void deleteSupplier(UUID id){
        repo.deleteById(id);
    }
}
