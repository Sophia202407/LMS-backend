package com.example.restarter_backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import com.example.restarter_backend.dto.LoginResponse;
import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.service.UserService;
import com.example.restarter_backend.repository.UserRepository;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

    // Inject UserRepository to fetch user details from DB
    private final UserRepository userRepository; 

    // Constructor to inject dependencies
    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                 SecurityContextHolder.getContext());

            // Get the username from the authenticated principal
            String username = authentication.getName();

            // Retrieve the full User entity from the database using the username
            User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB!"));

            log.info("User logged in successfully: {}", username);

            // Construct the LoginResponse DTO with actual user data
            LoginResponse response = new LoginResponse(
                true,
                "Login successful",
                user.getId(),
                user.getUsername(),
                user.getRole().name() // Use .name() on the enum to get its String value (e.g., "MEMBER", "LIBRARIAN")
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            log.warn("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            // Return LoginResponse for authentication failure
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new LoginResponse(false, "Invalid credentials.", null, request.getUsername(), null)
            );
        } catch (Exception e) {
            log.error("An unexpected error occurred during login for user {}: {}", request.getUsername(), e.getMessage(), e);
            // Return LoginResponse for general server errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new LoginResponse(false, "Something went wrong! Please try again later.", null, request.getUsername(), null)
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        log.info("User logout requested");
        try {
            // Invalidate the session
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            
            // Clear the SecurityContext
            SecurityContextHolder.clearContext();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Logout failed");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Current User Endpoint
    @GetMapping("/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() 
            && !authentication.getName().equals("anonymousUser")) {
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("username", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
            
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "No authenticated user");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
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

//Jun.8 update with the modified login method,your backend will send the correct id and role in the login response, 
//and your frontend's AuthContext will be able to populate the user object properly, resolving the undefined issues.

