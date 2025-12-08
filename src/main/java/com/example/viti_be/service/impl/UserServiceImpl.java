package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.UserRequest;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.model.User;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.service.CloudinaryService;
import com.example.viti_be.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

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

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

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
}