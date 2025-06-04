package com.example.restarter_backend.controller;

import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    // Only librarians can view all users
    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Librarian or the user themselves can view a user by ID
    @PreAuthorize("hasRole('LIBRARIAN') or #id == principal.id")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, Authentication authentication) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
            boolean isSelf = user.get().getUsername().equals(authentication.getName());
            if (isLibrarian || isSelf) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    // Only librarians can create users (for admin purposes)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // Librarian or the user themselves can update user info
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails, Authentication authentication) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
            boolean isSelf = user.get().getUsername().equals(authentication.getName());
            if (isLibrarian || isSelf) {
                User updated = userService.updateUser(id, userDetails, authentication.getName(), isLibrarian);
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    // Librarian or the user themselves can delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
            boolean isSelf = user.get().getUsername().equals(authentication.getName());
            if (isLibrarian || isSelf) {
                userService.deleteUser(id, authentication.getName(), isLibrarian);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    // Only librarians can search users by name
    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/search")
    public List<User> searchUsersByName(@RequestParam String name) {
        return userService.searchUsersByName(name);
    }
}
