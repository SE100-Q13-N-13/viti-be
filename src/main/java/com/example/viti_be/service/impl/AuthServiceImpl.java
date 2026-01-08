package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.CreateEmployeeRequest;
import com.example.viti_be.dto.request.LoginRequest;
import com.example.viti_be.dto.request.SignupRequest;
import com.example.viti_be.dto.response.JwtResponse;
import com.example.viti_be.model.RefreshToken;
import com.example.viti_be.model.Role;
import com.example.viti_be.model.User;
import com.example.viti_be.model.UserRole;
import com.example.viti_be.repository.RoleRepository;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.security.jwt.JwtUtils;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.AuthService;
import com.example.viti_be.service.EmailService;
import com.example.viti_be.service.RefreshTokenService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired AuthenticationManager authenticationManager;
    @Autowired JwtUtils jwtUtils;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EmailService emailService;
    @Autowired RefreshTokenService refreshTokenService;

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

        User user = userRepository.findById(userDetails.getId()).orElseThrow();

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
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setStatus("ACTIVE");
        user.setIsFirstLogin(true);

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Error: Role not found."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        user.getUserRoles().add(userRole);

        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        user.setVerificationCode(otp);
        user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        new Thread(() -> emailService.sendOtpEmail(user.getEmail(), otp)).start();
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationExpiration() == null || user.getVerificationExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP Expired");
        }

        if (!user.getVerificationCode().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationCode(null);
        user.setVerificationExpiration(null);
        user.setIsFirstLogin(false);
        user.setStatus("ACTIVE");

        userRepository.save(user);
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(false);
        userRepository.save(user);
    }

    @Override
    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFullName(signUpRequest.getFullName());
        user.setPhone(signUpRequest.getPhone());
        user.setStatus("PENDING");

        // Gán role ROLE_CUSTOMER cho user mới
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: Role 'ROLE_CUSTOMER' is not found."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(customerRole);
        user.getUserRoles().add(userRole);

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        user.setVerificationCode(otp);
        user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        new Thread(() -> emailService.sendOtpEmail(user.getEmail(), otp)).start();
    }

    @Override
    public void verifyUser(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP Expired");
        }

        if (user.getVerificationCode().equals(otp)) {
            user.setStatus("ACTIVE");
            user.setVerificationCode(null);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Invalid OTP");
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

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(email);
                newUser.setFullName(name);
                newUser.setPassword(passwordEncoder.encode("GOOGLE_AUTH_PLACEHOLDER"));
                newUser.setStatus("ACTIVE");

                Role role = roleRepository.findByName("ROLE_CUSTOMER")
                        .orElseThrow(() -> new RuntimeException("Error: Role 'ROLE_CUSTOMER' is not found. Please contact admin to seed data."));

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
            throw new RuntimeException("Invalid ID token.");
        }
    }
}