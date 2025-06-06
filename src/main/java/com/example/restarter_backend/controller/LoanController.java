package com.example.restarter_backend.controller;

import com.example.restarter_backend.entity.Loan;
import com.example.restarter_backend.exception.LoanLimitExceededException;
import com.example.restarter_backend.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    @Autowired
    private LoanService loanService;

    @GetMapping
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }
    
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        Optional<Loan> loan = loanService.getLoanById(id);
        return loan.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Loan createLoan(@RequestBody Loan loan) {
        return loanService.createLoan(loan);
    }

    // PUT endpoint to update an existing loan
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PostMapping("/{id}/renew")
    public ResponseEntity<?> renewLoan(@PathVariable Long id) {
        try {
            Loan renewed = loanService.renewLoan(id);
            return ResponseEntity.ok(renewed);
        } catch (Exception ex) {
            // Return JSON object with "message" field
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
}

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(LoanLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleLoanLimit(LoanLimitExceededException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
