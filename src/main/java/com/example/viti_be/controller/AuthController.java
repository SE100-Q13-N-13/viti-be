package com.example.viti_be.controller;

import com.example.viti_be.dto.request.LoginRequest;
import com.example.viti_be.dto.request.PasswordRequest;
import com.example.viti_be.dto.request.SignupRequest;
import com.example.viti_be.dto.response.JwtResponse;
import com.example.viti_be.service.AuthService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            authService.registerUser(signUpRequest);
            return ResponseEntity.ok("User registered successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody PasswordRequest.ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("OTP sent to your email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordRequest.VerifyOtpRequest request) {
        try {
            authService.resetPasswordWithOtp(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok("Password reset successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordRequest.ChangePasswordRequest request) {
        try {
            // Lấy user hiện tại từ Token
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            authService.changePassword(currentUsername, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok("Password changed successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DTO nhận token từ frontend
    @Data
    public static class GoogleLoginRequest {
        private String idToken;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            JwtResponse jwt = authService.loginWithGoogle(request.getIdToken());
            return ResponseEntity.ok(jwt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Google Auth Failed: " + e.getMessage());
        }
    }
}