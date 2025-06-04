package com.example.restarter_backend.repository;

import com.example.restarter_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username); // For authentication
    Optional<User> findByEmail(String email);       // Useful for login by email
}
