package com.example.restarter_backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.restarter_backend.dto.LoginRequest;
import com.example.restarter_backend.dto.RegisterRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j // Lombok handles the 'log' field creation
@RestController
@RequestMapping("/api/auth")
// Allow requests from http://localhost:3000 to all methods in this controller
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") 
public class AuthController {
     
    @Autowired
    private UserService userService;

    // Inject AuthenticationManager to handle authentication
    @Autowired
    private AuthenticationManager authenticationManager; // Inject AuthenticationManager

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Check if user already exists
            if (userService.findByUsername(request.getUsername()).isPresent()) {
                log.warn("Registration failed: Username already exists - {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists.");
            }
            User createdUser = userService.createUser(request); // Pass DTO to service
            log.info("New user registered: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());
        try {
            // Create an authentication token with username and password
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // If authentication is successful, set the authenticated object in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // You can also generate and return a JWT here if you're using token-based authentication
            // For now, we'll return a success message
            log.info("User logged in successfully: {}", request.getUsername());
            return ResponseEntity.ok("Login successful for user: " + request.getUsername());

        } catch (AuthenticationException e) {
            // Handle authentication failure (e.g., bad credentials)
            log.warn("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            // Return 401 Unauthorized for invalid credentials
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        } catch (Exception e) {
            // Catch any other unexpected errors during login
            log.error("An unexpected error occurred during login for user {}: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong! Please try again later.");
        }
    }


    // Check current user
    @GetMapping("/me")
    public ResponseEntity<String> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return ResponseEntity.ok("Current user: " + auth.getName());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authenticated user.");
    }
}

// Jun.6 use latest login version, which uses AuthenticationManager to perform the login, 
// is absolutely necessary to correctly handle user authentication 
// and avoid the "invalid credentials" error you were seeing from 
// your frontend despite a 200 OK status.