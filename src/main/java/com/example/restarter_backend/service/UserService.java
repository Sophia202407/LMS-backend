package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Role;
import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        // Check for duplicate username or email
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists.");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists.");
        }

        // Set default role if not provided
        if (user.getRole() == null) {
            user.setRole(Role.MEMBER);
        }

        // Set registration and expiry dates if not provided
        if (user.getRegistrationDate() == null) {
            user.setRegistrationDate(LocalDate.now());
        }
        if (user.getMembershipExpiryDate() == null) {
            user.setMembershipExpiryDate(user.getRegistrationDate().plusYears(1));
        }

        // Hash the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails, String currentUsername, boolean isLibrarian) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Security check: only librarian or the user themselves
        if (!isLibrarian && !existingUser.getUsername().equals(currentUsername)) {
            throw new SecurityException("Access denied: not allowed to update this user.");
        }

        existingUser.setName(userDetails.getName());
        existingUser.setAddress(userDetails.getAddress());
        existingUser.setContactInfo(userDetails.getContactInfo());

        if (userDetails.getRegistrationDate() != null) {
            existingUser.setRegistrationDate(userDetails.getRegistrationDate());
            existingUser.setMembershipExpiryDate(userDetails.getRegistrationDate().plusYears(1));
        }

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id, String currentUsername, boolean isLibrarian) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Security check: only librarian or the user themselves
        if (!isLibrarian && !existingUser.getUsername().equals(currentUsername)) {
            throw new SecurityException("Access denied: not allowed to delete this user.");
        }

        userRepository.deleteById(id);
    }

    public List<User> searchUsersByName(String name) {
        return userRepository.findAll().stream()
            .filter(user -> user.getName() != null && user.getName().toLowerCase().contains(name.toLowerCase()))
            .collect(Collectors.toList());
    }
}
