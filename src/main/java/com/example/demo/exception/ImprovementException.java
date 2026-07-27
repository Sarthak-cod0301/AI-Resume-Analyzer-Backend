// exception/ImprovementException.java
package com.example.demo.exception;

public class ImprovementException extends RuntimeException {
    public ImprovementException(String message) {
        super(message);
    }
    public ImprovementException(String message, Throwable cause) {
        super(message, cause);
    }
}