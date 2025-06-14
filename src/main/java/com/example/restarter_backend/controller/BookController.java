package com.example.restarter_backend.controller;

import com.example.restarter_backend.entity.Book;
import com.example.restarter_backend.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/books")
public class BookController {
    @Autowired
    private BookService bookService;

    // Accessible by any authenticated user (LIBRARIAN or MEMBER)
    // SecurityConfig's .anyRequest().authenticated() already covers this if no specific roles are needed
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        Optional<Book> book = bookService.getBookById(id);
        return book.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Accessible only by LIBRARIAN
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/add")
    public Book addBook(@RequestBody Book book) {
        return bookService.addBook(book);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        return bookService.updateBook(id, bookDetails);
    }

    // Accessible only by LIBRARIAN
    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String keyword) {
        return bookService.searchBooks(keyword);
    }

    public ResponseEntity<List<Book>> getBooksByStatus(@RequestParam String status) {
            try {
                // Convert the string status parameter to the Book.Status enum
                // Using toUpperCase() to make the conversion case-insensitive (e.g., "available" -> AVAILABLE)
                Book.Status bookStatus = Book.Status.valueOf(status.toUpperCase());
                List<Book> books = bookService.getAvailableBooks(bookStatus); // Call the service method
                return ResponseEntity.ok(books);
            } catch (IllegalArgumentException e) {
                // Handle cases where the provided 'status' string doesn't match any enum constant
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(null); // Or return a more informative error message
            }
        }
    }