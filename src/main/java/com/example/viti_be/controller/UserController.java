package com.example.viti_be.controller;

import com.example.viti_be.dto.request.AddressRequest;
import com.example.viti_be.dto.response.CustomerResponse.AddressResponse;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add address to current user's account")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        AddressResponse response = userService.addAddress(email, request);
        return ResponseEntity.ok(ApiResponse.<AddressResponse>builder()
                .code(200)
                .message("Address added successfully")
                .result(response)
                .build());
    }

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get all addresses of current user")
    public ResponseEntity<ApiResponse<java.util.List<AddressResponse>>> getAddresses(
            Authentication authentication) {
        String email = authentication.getName();
        java.util.List<AddressResponse> addresses = userService.getAddresses(email);
        return ResponseEntity.ok(ApiResponse.<java.util.List<AddressResponse>>builder()
                .code(200)
                .message("Addresses retrieved successfully")
                .result(addresses)
                .build());
    }

    @DeleteMapping("/addresses/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete address from current user's account")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID addressId,
            Authentication authentication) {
        String email = authentication.getName();
        userService.deleteAddress(email, addressId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Address deleted successfully")
                .build());
    }
}
