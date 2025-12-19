package com.example.viti_be.controller;

import com.example.viti_be.dto.request.UserRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}