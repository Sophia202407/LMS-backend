package com.example.restarter_backend.repository;

import com.example.restarter_backend.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    long countByUserIdAndStatus(Long userId, Loan.Status status);

    List<Loan> findByUserId(Long userId);
}
