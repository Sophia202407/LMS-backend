package com.example.restarter_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.restarter_backend.entity.Book; 

@PreAuthorize("hasAuthority('LIBRARIAN')")
@RestController
@RequestMapping("/api/librarian")
public class LibrarianController {

    @PostMapping("/books")
    public ResponseEntity<?> addBook(@RequestBody Book book) {
        // Only librarians can add books
        return ResponseEntity.ok("Book added!");
    }
}