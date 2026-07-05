package com.example.document_qa.web;

/** Thrown when a request fails validation (e.g. empty upload or blank question). */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
