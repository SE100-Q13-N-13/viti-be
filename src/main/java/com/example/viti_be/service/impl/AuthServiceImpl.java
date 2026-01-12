package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.CreateEmployeeRequest;
import com.example.viti_be.dto.request.LoginRequest;
import com.example.viti_be.dto.request.SignupRequest;
import com.example.viti_be.dto.response.JwtResponse;
import com.example.viti_be.exception.AuthenticationException;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.model.model_enum.UserStatus;
import com.example.viti_be.repository.CustomerRepository;
import com.example.viti_be.repository.RoleRepository;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.security.jwt.JwtUtils;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired AuthenticationManager authenticationManager;
    @Autowired JwtUtils jwtUtils;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EmailService emailService;
    @Autowired RefreshTokenService refreshTokenService;
    @Autowired AuditLogService auditLogService;
    @Autowired CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Value("${viti.app.googleClientId}")
    String googleClientId;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        // 1. Xác thực
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Sinh Access Token
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 3. Sinh Refresh Token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 4. Trả về JwtResponse
        return new JwtResponse(
                jwt,
                refreshToken.getToken(),
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles,
                user.getIsFirstLogin()
        );
    }

    @Override
    public void createEmployee(CreateEmployeeRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        String temporaryPassword = request.getPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        user.setIsFirstLogin(true);
        user.setIsActive(true);

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role not found."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        user.getUserRoles().add(userRole);

        User savedUser = userRepository.save(user);
        new Thread(() -> {
            try {
                emailService.sendEmployeeCredentials(
                        savedUser.getEmail(),
                        savedUser.getUsername(),
                        temporaryPassword,
                        savedUser.getFullName()
                );
                log.info("Sent credentials email to new employee: {}", savedUser.getEmail());
            } catch (Exception e) {
                log.error("Failed to send credentials email to {}: {}", savedUser.getEmail(), e.getMessage());
            }
        }).start();
        auditLogService.logSuccess(
                getCurrentUserId(),
                AuditModule.STAFF,
                AuditAction.CREATE,
                savedUser.getId().toString(),
                "USER",
                null,
                null
        );
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        user.setVerificationCode(otp);
        user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        new Thread(() -> emailService.sendOtpEmail(user.getEmail(), otp)).start();
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getVerificationExpiration() == null || user.getVerificationExpiration().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }

        if (!user.getVerificationCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationCode(null);
        user.setVerificationExpiration(null);
        user.setIsFirstLogin(false);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        auditLogService.logSuccess(
                user.getId(),
                AuditModule.STAFF,
                AuditAction.UPDATE,
                user.getId().toString(),
                "USER",
                null,
                null
        );
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new BadRequestException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFullName(signUpRequest.getFullName());
        user.setPhone(signUpRequest.getPhone());
        user.setStatus(UserStatus.PENDING);
        user.setIsActive(false);
        user.setIsFirstLogin(true);

        // Gán role ROLE_CUSTOMER cho user mới
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role 'ROLE_CUSTOMER' is not found."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(customerRole);
        user.getUserRoles().add(userRole);

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        user.setVerificationCode(otp);
        user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));

        User savedUser = userRepository.save(user);
        // 1. TÌM KIẾM ỨNG VIÊN (CANDIDATES)
        Set<Customer> foundCustomers = new HashSet<>();

        // Check Phone (nếu có)
        if (signUpRequest.getPhone() != null && !signUpRequest.getPhone().isBlank()) {
            customerRepository.findByPhone(signUpRequest.getPhone())
                    .ifPresent(foundCustomers::add);
        }

        // Check Email (nếu có)
        if (signUpRequest.getEmail() != null && !signUpRequest.getEmail().isBlank()) {
            customerRepository.findByEmail(signUpRequest.getEmail())
                    .ifPresent(foundCustomers::add);
        }

        // 2. XỬ LÝ KẾT QUẢ
        if (foundCustomers.isEmpty()) {
            // CASE A: KHÔNG TÌM THẤY AI -> TẠO MỚI HOÀN TOÀN
            customerService.createCustomerForUser(savedUser);

        } else if (foundCustomers.size() == 1) {
            // CASE B: TÌM THẤY ĐÚNG 1 NGƯỜI (Match Phone, hoặc Match Email, hoặc Match cả 2 cùng 1 người)
            // -> TIẾN HÀNH LIÊN KẾT (LINKING)
            Customer existingCustomer = foundCustomers.iterator().next();

            // Validate: Khách này đã có User khác chưa?
            if (existingCustomer.getUser() != null) {
                throw new BadRequestException("Thông tin khách hàng này đã được liên kết với tài khoản khác.");
            }

            // Link User vào Customer
            existingCustomer.setUser(savedUser);

            // Cập nhật thông tin mới nhất (Ưu tiên thông tin từ User vừa đăng ký)
            existingCustomer.setFullName(savedUser.getFullName());
            if (savedUser.getPhone() != null) existingCustomer.setPhone(savedUser.getPhone());
            if (savedUser.getEmail() != null) existingCustomer.setEmail(savedUser.getEmail());

            customerRepository.save(existingCustomer);

        } else {
            // CASE C: CONFLICT (TÌM THẤY > 1 NGƯỜI)
            // Ví dụ: Phone khớp Ông A, nhưng Email khớp Bà B.
            // Hệ thống không biết nên link vào ông A hay bà B.
            throw new BadRequestException(
                    "Dữ liệu mâu thuẫn: Số điện thoại và Email thuộc về 2 khách hàng khác nhau. Vui lòng liên hệ CSKH.");
        }

        new Thread(() -> emailService.sendOtpEmail(user.getEmail(), otp)).start();
    }

    @Override
    public void verifyUser(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getVerificationExpiration().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP Expired");
        }

        if (user.getVerificationCode().equals(otp)) {
            user.setStatus(UserStatus.ACTIVE);
            user.setIsActive(true);
            user.setVerificationCode(null);
            user.setVerificationExpiration(null);
            userRepository.save(user);
        } else {
            throw new BadRequestException("Invalid OTP");
        }
    }

    @Override
    public JwtResponse loginWithGoogle(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            Boolean emailVerified = payload.getEmailVerified();

            if (emailVerified == null || !emailVerified) {
                throw new BadRequestException("Google email is not verified");
            }

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(email);
                newUser.setFullName(name);
                newUser.setPassword(passwordEncoder.encode("GOOGLE_AUTH_PLACEHOLDER"));
                newUser.setStatus(UserStatus.ACTIVE);
                newUser.setIsActive(true);
                newUser.setIsFirstLogin(true);

                Role role = roleRepository.findByName("ROLE_CUSTOMER")
                        .orElseThrow(() -> new ResourceNotFoundException("Error: Role 'ROLE_CUSTOMER' is not found. Please contact admin to seed data."));

                UserRole ur = new UserRole();
                ur.setUser(newUser);
                ur.setRole(role);
                newUser.getUserRoles().add(ur);
                return userRepository.save(newUser);
            });

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    UserDetailsImpl.build(user), null, UserDetailsImpl.build(user).getAuthorities());

            String jwt = jwtUtils.generateJwtToken(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
            List<String> roles = user.getRoleNames().stream().toList();

            return new JwtResponse(
                    jwt,
                    refreshToken.getToken(),
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    roles,
                    user.getIsFirstLogin()
            );
        } else {
            throw new AuthenticationException("Invalid ID token.");
        }
    }

    @Override
    public void logout(UUID userId) {
        refreshTokenService.deleteByUserId(userId);
        // Note: Access token vẫn valid cho đến khi hết hạn (stateless JWT)
        // Nếu cần revoke ngay, phải implement token blacklist
    }

    /**
     * Helper method để lấy ID của user đang login
     * Dùng cho audit log khi cần biết "ai đang thực hiện hành động này"
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }
}