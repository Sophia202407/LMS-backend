package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
    if (user.getId() != null) { 
        Optional<User> existingUser = userRepository.findById(user.getId());
        if (existingUser.isPresent()) {
            throw new RuntimeException("User ID already exists.");
        }

        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
