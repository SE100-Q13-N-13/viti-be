package com.example.viti_be.controller;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.request.UserRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.dto.response.CustomerResponse.AddressResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Lấy thông tin cá nhân (Profile)
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UserResponse response = userService.getUserProfile(userImpl.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Get profile successfully"));
    }

    // Cập nhật thông tin cá nhân
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "data", required = false) UserRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {

        if (request == null) request = new UserRequest();

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UserResponse response = userService.updateProfile(userImpl.getId(), request, avatar);

        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    @PostMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add address to current user's account")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        AddressResponse response = userService.addAddress(email, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Address added successfully"));
    }

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get all addresses of current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            Authentication authentication) {
        String email = authentication.getName();
        List<AddressResponse> addresses = userService.getAddresses(email);
        return ResponseEntity.ok(ApiResponse.success(addresses, "Addresses retrieved successfully"));
    }

    @DeleteMapping("/addresses/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete address from current user's account")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID addressId,
            Authentication authentication) {
        String email = authentication.getName();
        userService.deleteAddress(email, addressId);
        return ResponseEntity.ok(ApiResponse.success(null, "Address deleted successfully"));
    }
}