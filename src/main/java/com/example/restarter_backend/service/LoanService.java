package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Book;
import com.example.restarter_backend.entity.Loan;
import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.exception.LoanLimitExceededException; 
import com.example.restarter_backend.repository.BookRepository;
import com.example.restarter_backend.repository.LoanRepository;
import com.example.restarter_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // it's important for data consistency

import java.time.LocalDate;
import java.time.temporal.ChronoUnit; // for ChronoUnit
import java.util.List;
import java.util.Optional;

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

    @Transactional // Ensures all operations within this method succeed or fail together
    public Loan createLoan(Loan loan) {
        // 1. Validate User and Book existence
        User user = userRepository.findById(loan.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Cannot create loan: User with ID " + loan.getUser().getId() + " not found."));

        Book book = bookRepository.findById(loan.getBook().getId())
                .orElseThrow(() -> new RuntimeException("Cannot create loan: Book with ID " + loan.getBook().getId() + " not found."));

        // Set user and book from DB to ensure they are properly managed entities
        loan.setUser(user);
        loan.setBook(book);

        // 2. Update overdue status for the user's loans before checks
        updateOverdueLoansForUser(user.getId());

        // 3. Apply Loan Business Rules
        // Check for overdue books
        long overdueLoansCount = loanRepository.countByUserIdAndStatus(user.getId(), Loan.Status.OVERDUE);
        if (overdueLoansCount > 0) {
            throw new RuntimeException("Cannot borrow: User has " + overdueLoansCount + " overdue book(s). Please return them first.");
        }

        // Check for outstanding fines
        double totalFines = calculateTotalFinesForUser(user.getId());
        if (totalFines > 10.0) {
            throw new RuntimeException("Cannot borrow: User has outstanding fines exceeding $10 (Total: $" + String.format("%.2f", totalFines) + "). Please pay your fines.");
        }

        // Check active loan limit
        long activeLoansCount = loanRepository.countByUserIdAndStatus(user.getId(), Loan.Status.ACTIVE);
        if (activeLoansCount >= 3) {
            throw new LoanLimitExceededException("Cannot borrow: Maximum 3 active loans allowed per member.");
        }

        // 4. Set Loan Details and Save
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14)); // Due in 14 days
        loan.setStatus(Loan.Status.ACTIVE);
        loan.setRenewalCount(0); // No renewals yet

        // Optional: Update book status (e.g., set to unavailable)
        // book.setAvailable(false); // If you have an 'available' field on Book
        // bookRepository.save(book); // Save the updated book status

        return loanRepository.save(loan);
    }

    @Transactional // Ensures atomicity
    public Optional<Loan> renewLoan(Long id) { // Changed return type to Optional<Loan> to match common pattern
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan with ID " + id + " not found for renewal."));

        if (loan.getStatus() != Loan.Status.ACTIVE) {
            throw new RuntimeException("Cannot renew loan " + id + ": Only active loans can be renewed. Current status: " + loan.getStatus() + ".");
        }

        if (loan.getRenewalCount() >= 2) {
            throw new RuntimeException("Cannot renew loan " + id + ": Maximum 2 renewals allowed.");
        }

        // Check if the loan is currently overdue before allowing renewal
        if (loan.getDueDate().isBefore(LocalDate.now())) {
            // If it's overdue, update its status and then prevent renewal
            if (loan.getStatus() == Loan.Status.ACTIVE) { // Ensure it was active before being marked overdue
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan); // Save the status change immediately
            }
            throw new RuntimeException("Cannot renew loan " + id + ": Loan is currently overdue. Please return the book.");
        }

        loan.setDueDate(loan.getDueDate().plusDays(14)); // Extend due date
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        return Optional.of(loanRepository.save(loan)); // Return Optional for consistency
    }

    /**
     * Calculates the fine for a single loan.
     * A fine is applied if the current date is after the due date.
     */
    public double calculateFine(Loan loan) {
        LocalDate dueDate = loan.getDueDate();
        LocalDate today = LocalDate.now();

        if (today.isAfter(dueDate)) {
            // Calculate number of full days overdue
            long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
            // Fine calculation: $0.50 per day, capped at $20.00
            return Math.min(overdueDays * 0.5, 20.0);
        }
        return 0.0; // No fine if not overdue
    }

    /**
     * Calculates the total outstanding fines for a given user across all their loans.
     */
    public double calculateTotalFinesForUser(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        double total = 0.0;
        for (Loan loan : loans) {
            total += calculateFine(loan);
        }
        return total;
    }

    @Transactional // Ensures atomicity
    public void deleteLoan(Long id) {
        // Optional: Logic to handle returning the book (e.g., update its availability)
        // You would typically fetch the loan, get the associated book, and mark it available.
        // For simplicity, we'll just delete the loan record here.
        if (!loanRepository.existsById(id)) {
            throw new RuntimeException("Loan with ID " + id + " not found for deletion.");
        }
        loanRepository.deleteById(id);
    }

    /**
     * Helper method to update the status of loans for a specific user to OVERDUE if their due date has passed.
     */
    @Transactional // Ensures updates are transactional
    private void updateOverdueLoansForUser(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        LocalDate today = LocalDate.now();
        for (Loan loan : loans) {
            // Only update ACTIVE loans that are past their due date
            if (loan.getStatus() == Loan.Status.ACTIVE && loan.getDueDate().isBefore(today)) {
                System.out.println("DEBUG: Loan " + loan.getId() + " for user " + userId + " is now OVERDUE.");
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan); // Save the updated loan status
            }
        }
    }

    /**
     * Scheduled task that runs daily at 1:00 AM to update all active loans that have become overdue.
     */
    @Scheduled(cron = "0 0 1 * * ?") // Runs every day at 1:00 AM
    @Transactional // Ensures the entire scheduled task is transactional
    public void updateAllOverdueLoans() {
        System.out.println("INFO: Starting daily scheduled overdue loan update.");
        // Find only currently ACTIVE loans to check
        List<Loan> activeLoans = loanRepository.findByStatus(Loan.Status.ACTIVE);
        LocalDate today = LocalDate.now();
        int updatedCount = 0;
        for (Loan loan : activeLoans) {
            if (loan.getDueDate().isBefore(today)) {
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan); // Save the status change
                updatedCount++;
                System.out.println("DEBUG: Scheduled update - Loan " + loan.getId() + " is now OVERDUE.");
            }
        }
        System.out.println("INFO: Scheduled overdue loan update completed. " + updatedCount + " loans updated.");
    }
}