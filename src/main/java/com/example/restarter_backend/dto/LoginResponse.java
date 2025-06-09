package com.example.restarter_backend.dto;

public class LoginResponse {
    private boolean success;
    private String message;
    private Long id; // User's ID
    private String username;
    private String role; // Or List<String> roles if a user can have multiple

    // Constructor to easily create the response object
    public LoginResponse(boolean success, String message, Long id, String username, String role) {
        this.success = success;
        this.message = message;
        this.id = id;
        this.username = username;
        this.role = role;
    }

    // Getters (and optionally setters, though for a response DTO, getters are usually enough)
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}