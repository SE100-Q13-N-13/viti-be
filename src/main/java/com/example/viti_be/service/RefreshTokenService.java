package com.example.viti_be.service;

import com.example.viti_be.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {
    Optional<RefreshToken> findByToken(String token);
    RefreshToken createRefreshToken(UUID userId);
    RefreshToken verifyExpiration(RefreshToken token);
    int deleteByUserId(UUID userId);
}
