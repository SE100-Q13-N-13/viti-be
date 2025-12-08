package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.UserRequest;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.model.User;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse updateProfile(UUID userId, UserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cập nhật thông tin
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());

        // Logic isFirstLogin: Nếu đang là true thì chuyển thành false sau khi update
        if (Boolean.TRUE.equals(user.getIsFirstLogin())) {
            user.setIsFirstLogin(false);
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }
}