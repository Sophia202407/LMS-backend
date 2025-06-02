package com.example.restarter_backend.exception;

public class LoanLimitExceededException extends RuntimeException {
    public LoanLimitExceededException(String message) {
        super(message);
    }
}
