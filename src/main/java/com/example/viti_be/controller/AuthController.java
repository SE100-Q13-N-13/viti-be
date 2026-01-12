package com.example.viti_be.controller;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.GoogleLoginResponse;
import com.example.viti_be.dto.response.JwtResponse;
import com.example.viti_be.dto.response.UserProviderDTO;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.User;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.security.jwt.JwtUtils;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.AuthService;
import com.example.viti_be.service.RefreshTokenService;
import com.example.viti_be.service.UserProviderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Autowired
    private UserProviderService userProviderService;

    @Autowired
    private UserRepository userRepository;

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
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody PasswordRequest.ResetPasswordRequest request) {
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
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            GoogleLoginResponse jwt = authService.loginWithGoogle(request.getIdToken());
            return ResponseEntity.ok(ApiResponse.success(jwt, "Google login successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Google Auth Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/create-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Create password for Google-only user")
    public ResponseEntity<ApiResponse<Object>> createPassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreatePasswordRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        userProviderService.createPasswordForGoogleUser(userDetails.getId(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(null,
                "Password created successfully. You can now login with email/password."));
    }

    @PostMapping("/link-google")
    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Link Google account for user")
    public ResponseEntity<ApiResponse<Object>> linkGoogleAccount(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody LinkGoogleAccountRequest request) {
        try {
            userProviderService.linkGoogleAccount(userDetails.getId(), request.getIdToken());
            return ResponseEntity.ok(ApiResponse.success(null, "Google account linked successfully"));
        } catch (Exception e) {
            throw new BadRequestException("Failed to link Google account: " + e.getMessage());
        }
    }

    @PostMapping("/skip-linking")
    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Mark that user skipped linking prompt")
    public ResponseEntity<ApiResponse<Object>> skipLinking(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        userProviderService.markLinkingSkipped(userDetails.getId());

        // Update isFirstLogin = false
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsFirstLogin(false);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(null, "Linking prompt will not be shown again"));
    }

    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Get providers list for user")
    public ResponseEntity<ApiResponse<List<UserProviderDTO>>> getUserProviders(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<UserProviderDTO> providers = userProviderService.getUserProviders(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(providers, "Providers retrieved successfully"));
    }

    @PostMapping("/verify-signup")
    public ResponseEntity<ApiResponse<Object>> verifySignup(@RequestBody PasswordRequest.VerifySignupRequest request) {
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