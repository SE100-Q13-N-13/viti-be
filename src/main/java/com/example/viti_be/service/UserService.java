package com.example.viti_be.service;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.request.UserRequest;
import com.example.viti_be.dto.response.CustomerResponse.AddressResponse;
import com.example.viti_be.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse getUserProfile(UUID userId);
    UserResponse updateProfile(UUID userId, UserRequest request, MultipartFile avatarFile);

    // Address management methods
    AddressResponse addAddress(UUID userId, AddressRequest request);
    List<AddressResponse> getAddresses(UUID userId);
    List<AddressResponse> getAddressesByUserId(UUID userId);
    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);
    void deleteAddress(UUID userId, UUID addressId);
}
