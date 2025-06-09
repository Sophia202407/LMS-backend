package com.example.restarter_backend.controller;

import com.example.restarter_backend.dto.LoanCreationRequest;
import org.springframework.http.HttpStatus;
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
    
    // AUTHENTICATED USER: Get loans for current user
    @PreAuthorize("hasRole('MEMBER') or hasRole('LIBRARIAN')")
    @GetMapping("/my-loans")
    public List<Loan> getMyLoans(Authentication authentication) {
        // Get current user's loans
        return loanService.getLoansByUsername(authentication.getName());
    }

    // --- POST Endpoints ---
    // AUTHENTICATED USER: Create a new loan
    @PostMapping
    @PreAuthorize("hasRole('MEMBER') or hasRole('LIBRARIAN')")
    public ResponseEntity<?> createLoan(@RequestBody LoanCreationRequest loanRequest, Authentication authentication) {
        try {
            // Security: ensure non-librarians can only borrow for themselves
            if (!authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_LIBRARIAN"))) {
                // Get the authenticated user's details and set it on the loan
                loanRequest.setUsername(authentication.getName());
            }
            
            Loan createdLoan = loanService.createLoan(loanRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdLoan); // Return 201 Created for successful creation
        } catch (LoanLimitExceededException ex) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage()));
        } catch (RuntimeException ex) {
            // It's good practice to log the full exception on the server side
            // logger.error("Error creating loan: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage()));
        }
    }

    // AUTHENTICATED USER: Renew a loan
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('LIBRARIAN') or @loanService.isLoanOwner(#id, authentication.name)")
    public ResponseEntity<Loan> renewLoan(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<Loan> renewedLoanOptional = loanService.renewLoan(id);
            return renewedLoanOptional.map(ResponseEntity::ok)
                                      .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build(); // Or include ex.getMessage() for more detail
        }
    }

    // AUTHENTICATED USER: Return a loan
    @PutMapping("/{id}/return") // Using PUT for updating the loan status
    @PreAuthorize("hasRole('LIBRARIAN') or @loanService.isLoanOwner(#id, authentication.name)")
    public ResponseEntity<Loan> returnLoan(@PathVariable Long id, Authentication authentication) {
        try {
            // The service method should handle finding the loan,
            // checking its status (e.g., must be 'ACTIVE'),
            // setting the return date, and updating the status to 'RETURNED'.
            Optional<Loan> returnedLoanOptional = loanService.returnLoan(id); // Assuming a returnLoan method in service
            return returnedLoanOptional.map(ResponseEntity::ok)
                                       .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (RuntimeException ex) {
            // Log the exception for debugging on the server side
            // logger.error("Error returning loan: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(null); // Or include ex.getMessage() for more detail
        }
    }

    // --- DELETE Endpoint ---
    // LIBRARIAN ONLY: Delete a loan
    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        try {
            loanService.deleteLoan(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build(); // Or include ex.getMessage() for more detail
        }
    }

    // --- Centralized Exception Handlers ---
    // Handles LoanLimitExceededException specifically
    @ExceptionHandler(LoanLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleLoanLimit(LoanLimitExceededException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    // Catches any other RuntimeException thrown from the service layer
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


//Jun.8 update with the new createLoan method, which now accepts a LoanCreationRequest DTO.
// This DTO allows for more flexible loan creation, including setting the username for non-librarians.
// - The createLoan method now checks if the user is a librarian or a member, and sets the username accordingly.
// - The method returns a ResponseEntity with a 201 Created status on success, or a 400 Bad Request on failure.
// add returnLoan method to handle returning loans, which updates the loan status and return date.
// - The returnLoan method checks if the user is the owner of the loan or a librarian before allowing the return.
