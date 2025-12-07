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
        ApiResponse<CustomerResponse> response = ApiResponse.<CustomerResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Customer created successfully")
                .result(customer)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse customer = customerService.updateCustomer(id, request);
        ApiResponse<CustomerResponse> response = ApiResponse.<CustomerResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Customer updated successfully")
                .result(customer)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable UUID id) {
        CustomerResponse customer = customerService.getCustomerById(id);
        ApiResponse<CustomerResponse> response = ApiResponse.<CustomerResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Customer retrieved successfully")
                .result(customer)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.getAllCustomers();
        ApiResponse<List<CustomerResponse>> response = ApiResponse.<List<CustomerResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Customers retrieved successfully")
                .result(customers)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Customer deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}/addresses")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> addAddress(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddressRequest request) {
        CustomerResponse customer = customerService.addAddress(customerId, request);
        ApiResponse<CustomerResponse> response = ApiResponse.<CustomerResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Address added successfully")
                .result(customer)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID customerId,
            @PathVariable UUID addressId) {
        customerService.deleteAddress(customerId, addressId);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Address deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }
}
