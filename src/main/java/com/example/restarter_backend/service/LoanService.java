package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Book;
import com.example.restarter_backend.entity.Loan;
import com.example.restarter_backend.exception.LoanLimitExceededException;
import com.example.restarter_backend.repository.BookRepository;
import com.example.restarter_backend.repository.LoanRepository;
import com.example.restarter_backend.repository.UserRepository;
import com.example.restarter_backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    public Loan createLoan(Loan loan) {
    long activeLoans = loanRepository.countByUserIdAndStatus(loan.getUser().getId(), Loan.Status.ACTIVE);
    if (activeLoans >= 3) {
        throw new LoanLimitExceededException("Max 3 books allowed per member.");
    }

    // Ensure User is fetched from the database
    User user = userRepository.findById(loan.getUser().getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
    loan.setUser(user); // Set the user properly

    // Retrieve book from database and set it in loan
    Book book = bookRepository.findById(loan.getBook().getId())
            .orElseThrow(() -> new RuntimeException("Book not found"));
    loan.setBook(book);


        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus(Loan.Status.ACTIVE);
        return loanRepository.save(loan);
    }

    public void deleteLoan(Long id) {
        loanRepository.deleteById(id);
    }
}
