package com.example.restarter_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String category;
    private Integer publicationYear;
    private Integer copiesAvailable;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        AVAILABLE,
        BORROWED,
        RESERVED
    }

    @Override
    public String toString() {
        return "Book{" +
               "id=" + id +
               ", isbn='" + isbn + '\'' +
               ", title='" + title + '\'' +
               ", author='" + author + '\'' +
               ", category='" + category + '\'' +
               ", publicationYear=" + publicationYear +
               ", copiesAvailable=" + copiesAvailable +
               ", status=" + status +
               '}';
    }
}