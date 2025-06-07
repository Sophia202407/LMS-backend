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
import java.util.HashMap;
import java.util.Map;

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
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Username already exists.");
                
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            User createdUser = userService.createUser(request); // Pass DTO to service
            log.info("New user registered: {}", request.getUsername());
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "User registered successfully");
            successResponse.put("user", createdUser);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Registration failed: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());
        try {
            // Create an authentication token with username and password
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // If authentication is successful, set the authenticated object in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Return JSON success response
            log.info("User logged in successfully: {}", request.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("username", request.getUsername());
            
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // Handle authentication failure (e.g., bad credentials)
            log.warn("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid credentials.");
            
            // Return 401 Unauthorized for invalid credentials
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            // Catch any other unexpected errors during login
            log.error("An unexpected error occurred during login for user {}: {}", request.getUsername(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Something went wrong! Please try again later.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
        // Current User Endpoint
        // @GetMapping("/me")
        // public ResponseEntity<?> currentUser(Authentication authentication) {
        //     if (authentication != null && authentication.isAuthenticated() 
        //         && !(authentication instanceof AnonymousAuthenticationToken)) {
        //         return ResponseEntity.ok(authentication.getPrincipal());
        //     }
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        //         .body("No authenticated user.");
}


// Jun.6 use latest login version, which uses AuthenticationManager to perform the login, 
// is absolutely necessary to correctly handle user authentication 
// and avoid the "invalid credentials" error you were seeing from 
// your frontend despite a 200 OK status.

// Jun.7 key changes:
// Changed return types from ResponseEntity<String> to ResponseEntity<Map<String, Object>> for login and /me endpoints
// Added JSON response structure with consistent format
// Import added: java.util.HashMap and java.util.Map
// All endpoints now return JSON instead of plain text, which will solve your frontend parsing error
