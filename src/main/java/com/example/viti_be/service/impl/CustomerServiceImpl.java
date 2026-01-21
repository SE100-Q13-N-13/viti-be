package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.request.CustomerRequest;
import com.example.viti_be.dto.response.CustomerResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerTierRepository customerTierRepository;

    @Autowired
    private LoyaltyPointRepository loyaltyPointRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already exists");
        }

        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setTotalPurchase(BigDecimal.ZERO);

        if (request.getTierId() != null) {
            CustomerTier tier = customerTierRepository.findByIdAndIsDeletedFalse(request.getTierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found"));
            customer.setTier(tier);
        }

        Customer savedCustomer = customerRepository.save(customer);

        // Create loyalty point wallet
        LoyaltyPoint loyaltyPoint = new LoyaltyPoint();
        loyaltyPoint.setCustomer(savedCustomer);
        loyaltyPoint.setTotalPoints(0);
        loyaltyPoint.setPointsAvailable(0);
        loyaltyPoint.setPointsUsed(0);
        loyaltyPoint.setPointRate(BigDecimal.ZERO);
        loyaltyPointRepository.save(loyaltyPoint);

        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (!customer.getPhone().equals(request.getPhone()) &&
            customerRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already exists");
        }

        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());

        if (request.getTierId() != null) {
            CustomerTier tier = customerTierRepository.findByIdAndIsDeletedFalse(request.getTierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found"));
            customer.setTier(tier);
        } else {
            customer.setTier(null);
        }

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToResponse(updatedCustomer);
    }

    @Override
    public CustomerResponse getCustomerById(UUID id) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return mapToResponse(customer);
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        Page<Customer> customerPage = customerRepository.findByIsDeletedFalse(pageable);
        return PageResponse.from(customerPage, this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setIsDeleted(true);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public CustomerResponse addAddress(UUID customerId, AddressRequest request) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Validate province
        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        // Validate commune
        Commune commune = communeRepository.findById(request.getCommuneCode())
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found"));

        // Validate commune belongs to province
        if (!commune.getProvince().getCode().equals(request.getProvinceCode())) {
            throw new BadRequestException("Commune does not belong to the selected province");
        }

        Address address = new Address();
        address.setCustomer(customer);
        address.setStreet(request.getStreet());
        address.setCommune(commune);
        address.setProvince(province);
        address.setType(request.getType());
        address.setIsPrimary(request.getIsPrimary());
        address.setPostalCode(request.getPostalCode());
        address.setContactName(request.getContactName());
        address.setPhoneNumber(request.getPhoneNumber());

        // If this is set as primary, remove primary from other addresses
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            customer.getAddresses().forEach(addr -> addr.setIsPrimary(false));
        }

        addressRepository.save(address);
        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateAddress(UUID customerId, UUID addressId, AddressRequest request) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getCustomer().getId().equals(customerId)) {
            throw new BadRequestException("Address does not belong to this customer");
        }

        // Validate province
        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        // Validate commune
        Commune commune = communeRepository.findById(request.getCommuneCode())
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found"));

        // Validate commune belongs to province
        if (!commune.getProvince().getCode().equals(request.getProvinceCode())) {
            throw new BadRequestException("Commune does not belong to the selected province");
        }

        // If this is set as primary, remove primary from other addresses
        if (Boolean.TRUE.equals(request.getIsPrimary()) && !Boolean.TRUE.equals(address.getIsPrimary())) {
            customer.getAddresses().forEach(addr -> addr.setIsPrimary(false));
        }

        // Update address
        address.setStreet(request.getStreet());
        address.setCommune(commune);
        address.setProvince(province);
        address.setType(request.getType());
        address.setIsPrimary(request.getIsPrimary());
        address.setPostalCode(request.getPostalCode());

        addressRepository.save(address);
        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getCustomer().getId().equals(customerId)) {
            throw new BadRequestException("Address does not belong to this customer");
        }

        address.setIsDeleted(true);
        addressRepository.save(address);
    }

    // Trong CustomerServiceImpl.java
    @Override
    @Transactional
    public Customer createCustomerForUser(User user) {
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFullName(user.getFullName());
        customer.setEmail(user.getEmail());
        customer.setPhone(user.getPhone());
        customer.setTotalPurchase(BigDecimal.ZERO);

        CustomerTier defaultTier = customerTierRepository.findByNameAndIsDeletedFalse("Bronze")
                .orElse(null);
        customer.setTier(defaultTier);

        Customer savedCustomer = customerRepository.save(customer);

        LoyaltyPoint loyaltyPoint = new LoyaltyPoint();
        loyaltyPoint.setCustomer(savedCustomer);
        loyaltyPoint.setTotalPoints(0);
        loyaltyPoint.setPointsAvailable(0);
        loyaltyPoint.setPointsUsed(0);
        loyaltyPoint.setPointRate(BigDecimal.ZERO);
        loyaltyPointRepository.save(loyaltyPoint);

        return savedCustomer;
    }

    private CustomerResponse mapToResponse(Customer customer) {
        CustomerResponse response = CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .totalPurchase(customer.getTotalPurchase())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();

        if (customer.getTier() != null) {
            response.setTier(CustomerResponse.CustomerTierResponse.builder()
                    .id(customer.getTier().getId())
                    .name(customer.getTier().getName())
                    .minPoint(customer.getTier().getMinPoint())
                    .discountRate(customer.getTier().getDiscountRate())
                    .description(customer.getTier().getDescription())
                    .status(customer.getTier().getStatus())
                    .build());
        }

        if (customer.getLoyaltyPoint() != null) {
            LoyaltyPoint lp = customer.getLoyaltyPoint();
            response.setLoyaltyPoint(CustomerResponse.LoyaltyPointResponse.builder()
                    .id(lp.getId())
                    .totalPoints(lp.getTotalPoints())
                    .pointsAvailable(lp.getPointsAvailable())
                    .pointsUsed(lp.getPointsUsed())
                    .pointRate(lp.getPointRate())
                    .lastEarnedAt(lp.getLastEarnedAt())
                    .lastUsedAt(lp.getLastUsedAt())
                    .build());
        }

        if (customer.getAddresses() != null) {
            response.setAddresses(customer.getAddresses().stream()
                    .filter(addr -> !Boolean.TRUE.equals(addr.getIsDeleted()))
                    .map(addr -> {
                        String detailAddress = addr.getStreet() + ", " +
                                              addr.getCommune().getName() + ", " +
                                              addr.getProvince().getName();
                        return CustomerResponse.AddressResponse.builder()
                                .id(addr.getId())
                                .street(addr.getStreet())
                                .commune(addr.getCommune().getName())
                                .communeCode(addr.getCommune().getCode())
                                .city(addr.getProvince().getName())
                                .provinceCode(addr.getProvince().getCode())
                                .detailAddress(detailAddress)
                                .type(addr.getType())
                                .isPrimary(addr.getIsPrimary())
                                .postalCode(addr.getPostalCode())
                                .build();
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }
}

