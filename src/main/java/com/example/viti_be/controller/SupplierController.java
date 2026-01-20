package com.example.viti_be.controller;

import com.example.viti_be.dto.request.SupplierRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.Supplier;
import com.example.viti_be.service.SupplierService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    // 1. CREATE (Tạo mới Supplier)
    @PostMapping
    public ResponseEntity<ApiResponse<Supplier>> createSupplier(@RequestBody SupplierRequest request) {
        Supplier newSupplier = supplierService.createSupplier(request);
        return ResponseEntity.ok(ApiResponse.success(newSupplier, "Supplier created successfully"));
    }

    // 2. READ (Lấy Supplier theo ID)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> getSupplierById(@PathVariable("id") UUID id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success(supplier, "Supplier fetched successfully"));
        // Lưu ý: Trong implementation Service cần xử lý NotFoundException
    }

    // 3. READ ALL (Lấy tất cả Suppliers)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Supplier>>> getAllSuppliers(
            @ParameterObject Pageable pageable
    ) {
        PageResponse<Supplier> suppliers = supplierService.getAllSuppliers(pageable);
        return ResponseEntity.ok(ApiResponse.success(suppliers, "Suppliers fetched successfully"));
    }

    // 4. UPDATE (Cập nhật Supplier)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> updateSupplier(@PathVariable("id") UUID id, @RequestBody SupplierRequest request) {
        Supplier updatedSupplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedSupplier, "Supplier updated successfully"));
    }

    // 5. DELETE (Xóa Supplier)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable("id") UUID id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted supplier"));
    }
}