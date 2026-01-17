package com.example.viti_be.controller;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.request.CustomerRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.CustomerResponse;
import com.example.viti_be.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(customer, "Customer created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable UUID id) {
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(customers, "Customers retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted successfully"));
    }

    @PostMapping("/{customerId}/addresses")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> addAddress(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddressRequest request) {
        CustomerResponse customer = customerService.addAddress(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(customer, "Address added successfully"));
    }

    @PutMapping("/{customerId}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateAddress(
            @PathVariable UUID customerId,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        CustomerResponse customer = customerService.updateAddress(customerId, addressId, request);
        return ResponseEntity.ok(ApiResponse.success(customer, "Address updated successfully"));
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID customerId,
            @PathVariable UUID addressId) {
        customerService.deleteAddress(customerId, addressId);
        return ResponseEntity.ok(ApiResponse.success(null, "Address deleted successfully"));
    }
}
