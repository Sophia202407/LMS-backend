package com.example.restarter_backend.repository;

import com.example.restarter_backend.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    long countByUserIdAndStatus(Long userId, Loan.Status status);
}
