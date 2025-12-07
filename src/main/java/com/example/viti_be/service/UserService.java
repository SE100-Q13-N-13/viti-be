package com.example.viti_be.service;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.response.CustomerResponse.AddressResponse;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.Address;
import com.example.viti_be.model.Commune;
import com.example.viti_be.model.Customer;
import com.example.viti_be.model.Province;
import com.example.viti_be.repository.AddressRepository;
import com.example.viti_be.repository.CommuneRepository;
import com.example.viti_be.repository.CustomerRepository;
import com.example.viti_be.repository.ProvinceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Transactional
    public AddressResponse addAddress(String email, AddressRequest request) {
        // Find customer by email
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

        // Validate province
        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        // Validate commune belongs to province
        Commune commune = communeRepository.findById(request.getCommuneCode())
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found"));

        if (!commune.getProvince().getCode().equals(request.getProvinceCode())) {
            throw new IllegalArgumentException("Commune does not belong to the specified province");
        }

        // If this is primary, unset other primary addresses
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            customer.getAddresses().forEach(addr -> addr.setIsPrimary(false));
        }

        // Create address
        Address address = new Address();
        address.setCustomer(customer);
        address.setStreet(request.getStreet());
        address.setProvince(province);
        address.setCommune(commune);
        address.setType(request.getType());
        address.setIsPrimary(request.getIsPrimary());
        address.setPostalCode(request.getPostalCode());

        Address savedAddress = addressRepository.save(address);

        return mapToAddressResponse(savedAddress);
    }

    public List<AddressResponse> getAddresses(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

        return customer.getAddresses().stream()
                .filter(address -> !Boolean.TRUE.equals(address.getIsDeleted()))
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAddress(String email, UUID addressId) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Verify address belongs to customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Address does not belong to this customer");
        }

        address.setIsDeleted(true);
        addressRepository.save(address);
    }

    private AddressResponse mapToAddressResponse(Address address) {
        String detailAddress = address.getStreet() + ", " 
                + address.getCommune().getName() + ", " 
                + address.getProvince().getName();

        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .commune(address.getCommune().getName())
                .city(address.getProvince().getName())
                .detailAddress(detailAddress)
                .type(address.getType())
                .isPrimary(address.getIsPrimary())
                .postalCode(address.getPostalCode())
                .build();
    }
}
