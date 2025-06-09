package com.example.restarter_backend.repository;

import com.example.restarter_backend.entity.Book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCaseOrCategoryContainingIgnoreCase(
        String title, String author, String isbn, String category
    );
    List<Book> findByStatus(Book.Status status);
    
    // Add this method to find a book by its ISBN
    Optional<Book> findByIsbn(String isbn);
    
    // Count books by status (if needed)
    long countByStatus(Book.Status status);
}