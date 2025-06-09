package com.example.restarter_backend.dto;

import java.time.LocalDate;

public class LoanCreationRequest {
    private String username;
    private String isbn; // 
    private LocalDate loanDate; // Or String borrowDate and parse it

    // Getters and Setters for all fields
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }


    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }
}