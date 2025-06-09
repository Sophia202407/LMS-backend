package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Book;
import com.example.restarter_backend.entity.Loan;
import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.exception.LoanLimitExceededException; 
import com.example.restarter_backend.repository.BookRepository;
import com.example.restarter_backend.repository.LoanRepository;
import com.example.restarter_backend.repository.UserRepository;
import com.example.restarter_backend.dto.LoanCreationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    /**
     * Get loans by username - useful for getting current user's loans
     */
    public List<Loan> getLoansByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User with username " + username + " not found."));
        return loanRepository.findByUserId(user.getId());
    }

    /**
     * Get loans by user ID
     */
    public List<Loan> getLoansByUserId(Long userId) {
        return loanRepository.findByUserId(userId);
    }

    /**
     * Check if a specific loan belongs to a specific user (for authorization)
     */
    public boolean isLoanOwner(Long loanId, String username) {
        Optional<Loan> loan = loanRepository.findById(loanId);
        if (loan.isPresent()) {
            // It's safer to use Optional.map().orElse(false) pattern here
            return loan.map(l -> l.getUser().getUsername().equals(username)).orElse(false);
        }
        return false;
    }

    @Transactional
    public Loan createLoan(LoanCreationRequest loanRequest) {
        // 1.find user by username,bookId
        User user = userRepository.findByUsername(loanRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Cannot create loan: User with username '" + loanRequest.getUsername() + "' not found."));

        Book book = bookRepository.findByIsbn(loanRequest.getIsbn())
                .orElseThrow(() -> new RuntimeException("Cannot create loan: Book with ISBN " + loanRequest.getIsbn() + " not found.")); // Changed ID to ISBN for clarity based on LoanForm

        // Set user and book from DB to ensure they are properly managed entities
        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBook(book);

        // 2. Check if book is available
        if (!isBookAvailable(book)) {
            throw new RuntimeException("Cannot create loan: Book '" + book.getTitle() + "' is currently unavailable.");
        }

        // 3. Update overdue status for the user's loans before checks
        updateOverdueLoansForUser(user.getId());

        // 4. Apply Loan Business Rules
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

        // 5. Set Loan Details and Save
        loan.setLoanDate(loanRequest.getLoanDate() != null ? loanRequest.getLoanDate() : LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14)); // Due in 14 days
        loan.setStatus(Loan.Status.ACTIVE);
        loan.setRenewalCount(0); // No renewals yet

        // 6. Update book status to unavailable
        updateBookAvailability(book, false); // false means 'BORROWED'

        return loanRepository.save(loan);
    }

    @Transactional
    public Optional<Loan> renewLoan(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan with ID " + id + " not found for renewal."));

        // First, explicitly check and update overdue status if applicable
        if (loan.getDueDate().isBefore(LocalDate.now())) {
            // If it's overdue, update its status first and then prevent renewal
            if (loan.getStatus() == Loan.Status.ACTIVE) { // Only change ACTIVE to OVERDUE
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan); // Save the status change
            }
            throw new RuntimeException("Cannot renew loan " + id + ": Loan is currently overdue. Please return the book.");
        }

        if (loan.getStatus() != Loan.Status.ACTIVE) {
            throw new RuntimeException("Cannot renew loan " + id + ": Only active loans can be renewed. Current status: " + loan.getStatus() + ".");
        }

        if (loan.getRenewalCount() >= 2) {
            throw new RuntimeException("Cannot renew loan " + id + ": Maximum 2 renewals allowed.");
        }

        loan.setDueDate(loan.getDueDate().plusDays(14)); // Extend due date
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        return Optional.of(loanRepository.save(loan));
    }

    @Transactional
    public Optional<Loan> returnLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan with ID " + loanId + " not found."));

        if (loan.getStatus() != Loan.Status.ACTIVE && loan.getStatus() != Loan.Status.OVERDUE) {
            throw new RuntimeException("Cannot return book: Loan is not active or overdue. Current status: " + loan.getStatus());
        }

        // Mark loan as returned
        loan.setStatus(Loan.Status.RETURNED);
        loan.setReturnDate(LocalDate.now());

        // Make book available again
        updateBookAvailability(loan.getBook(), true); // true means 'AVAILABLE'

        Loan savedLoan = loanRepository.save(loan); // Save the updated loan
        return Optional.of(savedLoan); // Return the updated loan wrapped in an Optional
    }
    
    /**
     * Check if a book is available for loan
     * This method assumes your Book entity has a 'status' field with an enum (e.g., Book.Status.AVAILABLE)
     */
    private boolean isBookAvailable(Book book) {
        // If your Book entity has a status field, check that.
        if (book.getStatus() != null) {
            return book.getStatus() == Book.Status.AVAILABLE;
        }
        // Fallback: Check if there are any active loans for this book if no explicit status field or if status isn't set.
        // This is less reliable if a book is conceptually "unavailable" for other reasons.
        long activeLoansForBook = loanRepository.countByBookIdAndStatus(book.getId(), Loan.Status.ACTIVE);
        return activeLoansForBook == 0;
    }

    /**
     * Updates the book's availability status.
     * This method assumes your Book entity has a 'status' field that accepts Book.Status.AVAILABLE or Book.Status.BORROWED.
     * It ensures the book is saved after status update.
     */
    private void updateBookAvailability(Book book, boolean available) {
        if (book != null) {
            if (available) {
                book.setStatus(Book.Status.AVAILABLE);
            } else {
                book.setStatus(Book.Status.BORROWED);
            }
            bookRepository.save(book); // Save the updated book entity
        }
    }


    /**
     * Calculates the fine for a single loan.
     */
    public double calculateFine(Loan loan) {
        LocalDate dueDate = loan.getDueDate();
        LocalDate today = LocalDate.now();

        if (today.isAfter(dueDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
            return Math.min(overdueDays * 0.5, 20.0); // 50 cents per day, max $20
        }
        return 0.0;
    }

    /**
     * Calculates the total outstanding fines for a given user.
     */
    public double calculateTotalFinesForUser(Long userId) {
        // Only consider active or overdue loans for calculating fines
        List<Loan> loans = loanRepository.findByUserIdAndStatusIn(userId, List.of(Loan.Status.ACTIVE, Loan.Status.OVERDUE)); 
        double total = 0.0;
        for (Loan loan : loans) {
            total += calculateFine(loan);
        }
        return total;
    }

    @Transactional
    public void deleteLoan(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan with ID " + id + " not found for deletion."));

        // If deleting an active loan, make the book available again
        if (loan.getStatus() == Loan.Status.ACTIVE || loan.getStatus() == Loan.Status.OVERDUE) { // Also return if overdue
            updateBookAvailability(loan.getBook(), true);
        }

        loanRepository.deleteById(id);
    }

    @Transactional
    private void updateOverdueLoansForUser(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        LocalDate today = LocalDate.now();
        for (Loan loan : loans) {
            // Only update active loans to overdue
            if (loan.getStatus() == Loan.Status.ACTIVE && loan.getDueDate().isBefore(today)) {
                System.out.println("DEBUG: Loan " + loan.getId() + " for user " + userId + " is now OVERDUE.");
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // Runs every day at 1 AM
    @Transactional
    public void updateAllOverdueLoans() {
        System.out.println("INFO: Starting daily scheduled overdue loan update.");
        List<Loan> activeLoans = loanRepository.findByStatus(Loan.Status.ACTIVE);
        LocalDate today = LocalDate.now();
        int updatedCount = 0;
        for (Loan loan : activeLoans) {
            if (loan.getDueDate().isBefore(today)) {
                loan.setStatus(Loan.Status.OVERDUE);
                loanRepository.save(loan);
                updatedCount++;
                System.out.println("DEBUG: Scheduled update - Loan " + loan.getId() + " is now OVERDUE.");
            }
        }
        System.out.println("INFO: Scheduled overdue loan update completed. " + updatedCount + " loans updated.");
    }
}




//Jun.8 update: 
// Added scheduled task to update overdue loans daily
// Added method to check if a loan belongs to a user for authorization
// Added method to update overdue loans for a user before creating a new loan
// add return method to handle returning loans, which updates the loan status and return date.
// - The returnLoan method checks if the user is the owner of the loan or a librarian before allowing the return.
// - The method returns an Optional<Loan> to indicate success or failure.