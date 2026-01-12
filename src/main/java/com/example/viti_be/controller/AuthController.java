package com.example.viti_be.controller;

import com.example.viti_be.dto.request.LoginRequest;
import com.example.viti_be.dto.request.PasswordRequest;
import com.example.viti_be.dto.request.SignupRequest;
import com.example.viti_be.dto.request.TokenRefreshRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.JwtResponse;
import com.example.viti_be.security.jwt.JwtUtils;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.AuthService;
import com.example.viti_be.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse, "Login successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            authService.registerUser(signUpRequest);
            return ResponseEntity.ok(ApiResponse.success(null, "User registered successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@RequestBody PasswordRequest.ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null, "OTP sent to your email."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody PasswordRequest.VerifyOtpRequest request) {
        try {
            authService.resetPasswordWithOtp(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@RequestBody PasswordRequest.ChangePasswordRequest request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            authService.changePassword(currentUsername, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @Data
    public static class GoogleLoginRequest {
        private String idToken;
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<JwtResponse>> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            JwtResponse jwt = authService.loginWithGoogle(request.getIdToken());
            return ResponseEntity.ok(ApiResponse.success(jwt, "Google login successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Google Auth Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-signup")
    public ResponseEntity<ApiResponse<Object>> verifySignup(@RequestBody PasswordRequest.VerifyOtpRequest request) {
        try {
            authService.verifyUser(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(ApiResponse.success(null, "Account verified successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    String accessToken = jwtUtils.generateTokenFromEmail(token.getUser().getEmail(), 360000);
                    JwtResponse response = new JwtResponse(accessToken, requestRefreshToken);
                    return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Refresh token is not in database!")));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        authService.logout(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}