package com.example.viti_be.security.services;

import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.UserStatus;
import com.example.viti_be.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;

@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

//        try {
//            Field field = User.class.getDeclaredField("isActive");
//            field.setAccessible(true);
//            Object realValue = field.get(user);
//            log.info("🔍 REFLECTION CHECK - Field 'isActive' actual value: {}", realValue);
//        } catch (Exception e) {
//            log.error("Reflection failed", e);
//        }

        if (UserStatus.PENDING.equals(user.getStatus())) {
            log.warn("⚠️ User {} is PENDING", email);
            throw new RuntimeException("Account is not verified. Please check your email for verification code.");
        }

        if (UserStatus.SUSPENDED.equals(user.getStatus())) {
            log.warn("⚠️ User {} is SUSPENDED", email);
            throw new RuntimeException("Account has been suspended. Please contact support.");
        }

        if (UserStatus.TERMINATED.equals(user.getStatus())) {
            log.warn("⚠️ User {} is TERMINATED", email);
            throw new RuntimeException("Account has been terminated.");
        }

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        log.info("✅ UserDetails created - isEnabled(): {}", userDetails.isEnabled());

        return userDetails;
    }
}