package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Book;
import com.example.restarter_backend.entity.Loan;
import com.example.restarter_backend.exception.LoanLimitExceededException;
import com.example.restarter_backend.repository.BookRepository;
import com.example.restarter_backend.repository.LoanRepository;
import com.example.restarter_backend.repository.UserRepository;
import com.example.restarter_backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate; // or java.time.LocalDateTime

@Service
public class LoanService {
    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository; 

    @Autowired
    private UserRepository userRepository; 

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }

    public Loan createLoan(Loan loan) {
        // Check if user exists
        User user = userRepository.findById(loan.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update overdue loans for this user before checking
        updateOverdueLoansForUser(user.getId());

        // Check for overdue books
        long overdueLoans = loanRepository.countByUserIdAndStatus(user.getId(), Loan.Status.OVERDUE);
        if (overdueLoans > 0) {
            throw new RuntimeException("Cannot borrow: user has overdue books.");
        }

        // Calculate total fines for user
        double totalFines = calculateTotalFinesForUser(user.getId());
        if (totalFines > 10.0) {
            throw new RuntimeException("Cannot borrow: outstanding fines exceed $10.");
        }

        // Check active loan count
        long activeLoans = loanRepository.countByUserIdAndStatus(user.getId(), Loan.Status.ACTIVE);
        if (activeLoans >= 3) {
            throw new LoanLimitExceededException("Max 3 books allowed per member.");
        }

        // Set user and book from DB
        loan.setUser(user);
        Book book = bookRepository.findById(loan.getBook().getId())
                .orElseThrow(() -> new RuntimeException("Book not found"));
        loan.setBook(book);

        // Set loan dates and status
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus(Loan.Status.ACTIVE);
        loan.setRenewalCount(0); // Track renewals

        loan.setLoanDate(LocalDate.now());
        return loanRepository.save(loan);
    }

    // Renew loan with max 2 renewals
    public Loan renewLoan(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.Status.ACTIVE) {
            throw new RuntimeException("Only active loans can be renewed.");
        }

        if (loan.getRenewalCount() >= 2) {
            throw new RuntimeException("Maximum 2 renewals allowed.");
        }

        // Check for overdue before renewal
        if (loan.getDueDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot renew overdue loan.");
        }

        loan.setDueDate(loan.getDueDate().plusDays(14));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        return loanRepository.save(loan);
    }

    // Calculate fine for a loan
    public double calculateFine(Loan loan) {
        LocalDate dueDate = loan.getDueDate();
        LocalDate today = LocalDate.now();
        if (today.isAfter(dueDate)) {
            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(dueDate.plusDays(1), today.plusDays(1));
            double fine = Math.min(overdueDays * 0.5, 20.0);
            return fine;
        }
        return 0.0;
    }

    // Calculate total fines for a user
    public double calculateTotalFinesForUser(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        double total = 0.0;
        for (Loan loan : loans) {
            total += calculateFine(loan);
        }
        return total;
    }

    public void deleteLoan(Long id) {
        loanRepository.deleteById(id);
    }

    // Add this helper method:
    private void updateOverdueLoansForUser(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        LocalDate today = LocalDate.now();
        for (Loan loan : loans) {
            if (loan.getStatus() == Loan.Status.ACTIVE && loan.getDueDate().isBefore(today)) {
                System.out.println("Updating loan " + loan.getId() + " to OVERDUE");
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // Runs every day at 1:00 AM
    public void updateAllOverdueLoans() {
        List<Loan> loans = loanRepository.findAll();
        LocalDate today = LocalDate.now();
        for (Loan loan : loans) {
            if (loan.getStatus() == Loan.Status.ACTIVE && loan.getDueDate().isBefore(today)) {
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan);
            }
        }
        System.out.println("Scheduled overdue loan update completed.");
    }
}
