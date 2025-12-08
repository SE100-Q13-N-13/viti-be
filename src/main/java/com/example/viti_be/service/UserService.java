package com.example.viti_be.service;

import com.example.viti_be.dto.request.UserRequest;
import com.example.viti_be.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {
    UserResponse getUserProfile(UUID userId);
    UserResponse updateProfile(UUID userId, UserRequest request, MultipartFile avatarFile);}