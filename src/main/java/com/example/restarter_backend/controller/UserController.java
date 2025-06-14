package com.example.restarter_backend.controller;

import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.service.UserService;
import com.example.restarter_backend.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ---- Helper Method ----
    private boolean isOwner(Long id, Authentication auth) {
        return userService.isUserOwner(id, auth.getName());
    }

    // ==== Librarian-Only Endpoints ====
    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody RegisterRequest request) {
        return userService.createUser(request);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/search")
    public List<User> searchUsersByUsername(@RequestParam String username) {
        return userService.searchUsersByUsername(username);
    }

    // ==== Shared Endpoints (Librarian or Owner) ====
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN') or @userController.isOwner(#id, principal)")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN') or @userController.isOwner(#id, principal)")
    public User updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return userService.updateUser(id, userDetails);
    }

}