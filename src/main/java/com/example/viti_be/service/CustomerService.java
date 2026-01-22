package com.example.viti_be.service;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.request.CustomerRequest;
import com.example.viti_be.dto.response.CustomerResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.Customer;
import com.example.viti_be.model.User;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse updateCustomer(UUID id, CustomerRequest request);
    CustomerResponse getCustomerById(UUID id);

    CustomerResponse getCustomerByUserId(UUID userId);

    PageResponse<CustomerResponse> getAllCustomers(Pageable pageable);
    void deleteCustomer(UUID id);
    CustomerResponse addAddress(UUID customerId, AddressRequest request);
    CustomerResponse updateAddress(UUID customerId, UUID addressId, AddressRequest request);
    void deleteAddress(UUID customerId, UUID addressId);

    Customer createCustomerForUser(User user);
}

