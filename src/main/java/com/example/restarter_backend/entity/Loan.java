package com.example.restarter_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;// or LocalDateTime

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "loan_date")
    private LocalDate loanDate;
    // getter and setter
    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }

    private LocalDate dueDate;
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    private int renewalCount; // Number of times this loan has been renewed

    public enum Status {
        ACTIVE,
        RETURNED,
        OVERDUE,
        BORROWED
    }

}
