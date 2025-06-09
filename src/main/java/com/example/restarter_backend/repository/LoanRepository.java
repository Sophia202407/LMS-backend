package com.example.restarter_backend.repository;

import com.example.restarter_backend.entity.Loan;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    // For updateOverdueLoansForUser
    List<Loan> findByUserId(Long userId); 
    // For checks in createLoan
    long countByUserIdAndStatus(Long userId, Loan.Status status); 
    // Required for LoanService.calculateTotalFinesForUser
    List<Loan> findByUserIdAndStatusIn(Long userId, List<Loan.Status> statuses);
    // for isBookAvailable
    long countByBookIdAndStatus(Long bookId, Loan.Status status); 
    // For updateAllOverdueLoans
    List<Loan> findByStatus(Loan.Status status); 
   




}

