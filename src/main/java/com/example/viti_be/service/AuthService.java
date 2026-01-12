package com.example.viti_be.service;

import com.example.viti_be.dto.request.CreateEmployeeRequest;
import com.example.viti_be.dto.request.LoginRequest;
import com.example.viti_be.dto.request.SignupRequest;
import com.example.viti_be.dto.response.GoogleLoginResponse;
import com.example.viti_be.dto.response.JwtResponse;

import java.util.UUID;

public interface AuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    void createEmployee(CreateEmployeeRequest request);
    void forgotPassword(String email);
    void resetPasswordWithOtp(String email, String otp, String newPassword);
    void changePassword(String username, String oldPassword, String newPassword);
    void registerUser(SignupRequest signUpRequest);
    void verifyUser(String email, String otp);
    GoogleLoginResponse loginWithGoogle(String idTokenString) throws Exception;

    void logout(UUID userId);
}