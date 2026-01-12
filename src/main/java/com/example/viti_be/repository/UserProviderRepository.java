package com.example.viti_be.repository;

import com.example.viti_be.model.User;
import com.example.viti_be.model.UserProvider;
import com.example.viti_be.model.model_enum.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, UUID> {

    List<UserProvider> findByUser(User user);

    Optional<UserProvider> findByUserAndProvider(User user, AuthProvider provider);

    Optional<UserProvider> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByUserAndProvider(User user, AuthProvider provider);

    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);
}