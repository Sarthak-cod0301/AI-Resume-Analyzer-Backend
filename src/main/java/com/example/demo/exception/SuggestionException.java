// exception/SuggestionException.java
package com.example.demo.exception;

public class SuggestionException extends RuntimeException {
    public SuggestionException(String message) {
        super(message);
    }
    public SuggestionException(String message, Throwable cause) {
        super(message, cause);
    }
}