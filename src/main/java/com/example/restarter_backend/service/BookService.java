package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Book;
import com.example.restarter_backend.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book bookDetails) {
        Book book = bookRepository.findById(id).orElseThrow();
        book.setIsbn(bookDetails.getIsbn());
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setCategory(bookDetails.getCategory());
        book.setPublicationYear(bookDetails.getPublicationYear());
        book.setCopiesAvailable(bookDetails.getCopiesAvailable());
        book.setStatus(bookDetails.getStatus());
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    public List<Book> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            keyword, keyword, keyword, keyword
        );
    }

    public List<Book> filterByStatus(String status) {
        return bookRepository.findByStatus(status);
    }
}