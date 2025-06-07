package com.example.restarter_backend.repository;

import com.example.restarter_backend.entity.Loan;
import com.example.restarter_backend.entity.User;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId); // For updateOverdueLoansForUser
    List<Loan> findByStatus(Loan.Status status); // For updateAllOverdueLoans
    long countByUserIdAndStatus(Long userId, Loan.Status status); // For checks in createLoan
}

