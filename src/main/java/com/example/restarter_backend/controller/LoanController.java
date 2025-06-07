package com.example.restarter_backend.controller;

import com.example.restarter_backend.entity.Loan;
import com.example.restarter_backend.exception.LoanLimitExceededException;
import com.example.restarter_backend.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanService loanService;

    // --- GET Endpoints ---

    // LIBRARIAN ONLY: Get all loans in the system
    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/all")
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }

    // LIBRARIAN ONLY: Get a specific loan by ID
    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        Optional<Loan> loan = loanService.getLoanById(id);
        return loan.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- POST Endpoints ---

    // AUTHENTICATED USER: Create a new loan
    @PostMapping
    @PreAuthorize("isAuthenticated()") // Ensure only authenticated users can create loans
    public ResponseEntity<Loan> createLoan(@RequestBody Loan loan) {
        // If the service can throw exceptions (like BookNotFound, UserNotFound, etc.)
        // those will be caught by the @ExceptionHandler below.
        Loan createdLoan = loanService.createLoan(loan);
        return ResponseEntity.ok(createdLoan);
    }

    // AUTHENTICATED USER: Renew a loan
    @PostMapping("/{id}/renew")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Loan> renewLoan(@PathVariable Long id) {
        Optional<Loan> renewedLoanOptional = loanService.renewLoan(id);

        // If the Optional is empty, it means the loan wasn't found or an error occurred
        // that the service handled by returning an empty Optional.
        // Otherwise, it returns 200 OK with the renewed loan.
        return renewedLoanOptional.map(ResponseEntity::ok)
                                  .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- DELETE Endpoint ---

    // LIBRARIAN ONLY: Delete a loan
    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    // --- Centralized Exception Handlers ---

    // Handles LoanLimitExceededException specifically
    @ExceptionHandler(LoanLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleLoanLimit(LoanLimitExceededException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    // Catches any other RuntimeException thrown from the service layer
    // This provides a generic 400 Bad Request with the exception message
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGenericRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}

//Jun.7 big changes including logic adjustments, exception handling, and new endpoints
// - Added getMyLoans endpoint for authenticated users to view their own loans
// - Updated createLoan to handle LoanLimitExceededException
// - Added renewLoan endpoint for authenticated users to renew their loans
// - Centralized exception handling for LoanLimitExceededException and other RuntimeExceptions
// - Used ResponseEntity for consistent HTTP responses
// - Ensured all endpoints are secured with appropriate @PreAuthorize annotations