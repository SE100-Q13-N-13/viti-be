package com.example.viti_be.service;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.request.CustomerRequest;
import com.example.viti_be.dto.response.CustomerResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse updateCustomer(UUID id, CustomerRequest request);
    CustomerResponse getCustomerById(UUID id);
    List<CustomerResponse> getAllCustomers();
    void deleteCustomer(UUID id);
    CustomerResponse addAddress(UUID customerId, AddressRequest request);
    void deleteAddress(UUID customerId, UUID addressId);
}

