package com.example.itqgroupttask.exception;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}

