package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.UserProviderDTO;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.User;
import com.example.viti_be.model.UserProvider;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.model.model_enum.AuthProvider;
import com.example.viti_be.repository.UserProviderRepository;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.UserProviderService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserProviderServiceImpl implements UserProviderService {

    @Autowired
    private UserProviderRepository userProviderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    @Value("${viti.app.googleClientId}")
    private String googleClientId;

    @Override
    @Transactional
    public void addProvider(User user, AuthProvider provider, String providerId, Boolean isPrimary) {
        // Check đã tồn tại chưa
        if (userProviderRepository.existsByUserAndProvider(user, provider)) {
            throw new BadRequestException("Provider already linked to this account");
        }

        // Nếu provider là GOOGLE, check providerId đã được link với user khác chưa
        if (provider == AuthProvider.GOOGLE && providerId != null) {
            if (userProviderRepository.existsByProviderAndProviderId(provider, providerId)) {
                throw new BadRequestException("This Google account is already linked to another user");
            }
        }

        UserProvider userProvider = UserProvider.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .isPrimary(isPrimary)
                .build();

        userProviderRepository.save(userProvider);

        log.info("Added provider {} for user {}", provider, user.getEmail());
    }

    @Override
    public boolean hasProvider(User user, AuthProvider provider) {
        return userProviderRepository.existsByUserAndProvider(user, provider);
    }

    @Override
    public List<UserProviderDTO> getUserProviders(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userProviderRepository.findByUser(user).stream()
                .map(up -> UserProviderDTO.builder()
                        .provider(up.getProvider())
                        .isPrimary(up.getIsPrimary())
                        .linkedAt(up.getLinkedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void linkGoogleAccount(UUID userId, String googleIdToken) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify Google token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(googleIdToken);
        if (idToken == null) {
            throw new BadRequestException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleSub = payload.getSubject(); // Google's unique user ID
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();

        if (emailVerified == null || !emailVerified) {
            throw new BadRequestException("Google email is not verified");
        }

        // Check email match
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Google account email does not match your account email");
        }

        // Add Google provider
        addProvider(user, AuthProvider.GOOGLE, googleSub, false);

        // Audit log
        auditLogService.logSuccess(
                userId,
                AuditModule.STAFF,
                AuditAction.UPDATE,
                userId.toString(),
                "USER",
                null,
                "Linked Google account"
        );
    }

    @Override
    @Transactional
    public void createPasswordForGoogleUser(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check: User phải có GOOGLE provider
        if (!hasProvider(user, AuthProvider.GOOGLE)) {
            throw new BadRequestException("This feature is only for Google login users");
        }

        // Check: User chưa có EMAIL provider
        if (hasProvider(user, AuthProvider.EMAIL)) {
            throw new BadRequestException("You already have email/password login enabled");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(password));
        user.setIsFirstLogin(false);
        userRepository.save(user);

        // Add EMAIL provider
        addProvider(user, AuthProvider.EMAIL, null, true); // EMAIL là primary

        // Audit log
        auditLogService.logSuccess(
                userId,
                AuditModule.STAFF,
                AuditAction.UPDATE,
                userId.toString(),
                "USER",
                null,
                "Created password for Google user"
        );

        log.info("Google user {} created password for email/password login", user.getEmail());
    }

    @Override
    @Transactional
    public void markLinkingSkipped(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Lưu vào một field mới trong User entity
        // Hoặc tạo bảng riêng user_preferences
        // Tạm thời dùng cách đơn giản: check xem đã có EMAIL provider chưa
        // Nếu login Google mà không tạo EMAIL provider → coi như đã skip

        log.info("User {} skipped account linking prompt", user.getEmail());
    }

    @Override
    public boolean hasSkippedLinking(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // User đã skip nếu:
        // 1. Có GOOGLE provider
        // 2. KHÔNG có EMAIL provider
        // 3. isFirstLogin = false (đã login ít nhất 1 lần và không tạo password)

        boolean hasGoogle = hasProvider(user, AuthProvider.GOOGLE);
        boolean hasEmail = hasProvider(user, AuthProvider.EMAIL);
        boolean notFirstLogin = !user.getIsFirstLogin();

        return hasGoogle && !hasEmail && notFirstLogin;
    }
}