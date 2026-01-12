package com.example.viti_be.service;

import com.example.viti_be.dto.response.UserProviderDTO;
import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.AuthProvider;

import java.util.List;
import java.util.UUID;

public interface UserProviderService {

    /**
     * Thêm provider cho user (khi register hoặc link account)
     */
    void addProvider(User user, AuthProvider provider, String providerId, Boolean isPrimary);

    /**
     * Check user đã có provider chưa
     */
    boolean hasProvider(User user, AuthProvider provider);

    /**
     * Lấy danh sách providers của user
     */
    List<UserProviderDTO> getUserProviders(UUID userId);

    /**
     * Link Google account với user hiện tại
     */
    void linkGoogleAccount(UUID userId, String googleIdToken) throws Exception;

    /**
     * Tạo password cho Google-only user (chuyển thành dual-login)
     */
    void createPasswordForGoogleUser(UUID userId, String password);

    /**
     * Đánh dấu user đã skip prompt linking
     */
    void markLinkingSkipped(UUID userId);

    /**
     * Check user đã skip linking chưa
     */
    boolean hasSkippedLinking(UUID userId);
}