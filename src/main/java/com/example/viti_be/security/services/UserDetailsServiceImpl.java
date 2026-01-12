package com.example.viti_be.security.services;

import com.example.viti_be.model.User;
import com.example.viti_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is not verified. Please check your email for verification code.");
        }

        if ("SUSPENDED".equals(user.getStatus())) {
            throw new RuntimeException("Account has been suspended. Please contact support.");
        }

        if ("TERMINATED".equals(user.getStatus())) {
            throw new RuntimeException("Account has been terminated.");
        }

        return UserDetailsImpl.build(user);
    }
}