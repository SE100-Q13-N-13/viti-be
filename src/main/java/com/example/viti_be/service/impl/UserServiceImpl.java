package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.request.UserRequest;
import com.example.viti_be.dto.response.CustomerResponse.AddressResponse;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.Address;
import com.example.viti_be.model.Commune;
import com.example.viti_be.model.Customer;
import com.example.viti_be.model.Province;
import com.example.viti_be.model.User;
import com.example.viti_be.repository.AddressRepository;
import com.example.viti_be.repository.CommuneRepository;
import com.example.viti_be.repository.CustomerRepository;
import com.example.viti_be.repository.ProvinceRepository;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.service.CloudinaryService;
import com.example.viti_be.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Override
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse updateProfile(UUID userId, UserRequest request, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request != null) {
            if (request.getFullName() != null) user.setFullName(request.getFullName());
            if (request.getPhone() != null) user.setPhone(request.getPhone());
        }

        // Logic Upload Avatar
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = cloudinaryService.uploadFile(avatarFile);
            user.setAvatar(avatarUrl);
        }

        // Tắt cờ First Login
        if (Boolean.TRUE.equals(user.getIsFirstLogin())) {
            user.setIsFirstLogin(false);
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

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

    @Override
    public List<AddressResponse> getAddresses(UUID userId) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return customer.getAddresses().stream()
                .filter(address -> !Boolean.TRUE.equals(address.getIsDeleted()))
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressResponse> getAddressesByUserId(UUID userId) {
        // Admin/Employee get addresses of any user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return customer.getAddresses().stream()
                .filter(address -> !Boolean.TRUE.equals(address.getIsDeleted()))
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Verify address belongs to customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Address does not belong to this customer");
        }

        // Validate province
        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        // Validate commune belongs to province
        Commune commune = communeRepository.findById(request.getCommuneCode())
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found"));

        if (!commune.getProvince().getCode().equals(request.getProvinceCode())) {
            throw new IllegalArgumentException("Commune does not belong to the specified province");
        }

        // If this is set as primary, remove primary from other addresses
        if (Boolean.TRUE.equals(request.getIsPrimary()) && !Boolean.TRUE.equals(address.getIsPrimary())) {
            customer.getAddresses().forEach(addr -> addr.setIsPrimary(false));
        }

        // Update address
        address.setStreet(request.getStreet());
        address.setProvince(province);
        address.setCommune(commune);
        address.setType(request.getType());
        address.setIsPrimary(request.getIsPrimary());
        address.setPostalCode(request.getPostalCode());

        Address updatedAddress = addressRepository.save(address);

        return mapToAddressResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        // Find customer by Id
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

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