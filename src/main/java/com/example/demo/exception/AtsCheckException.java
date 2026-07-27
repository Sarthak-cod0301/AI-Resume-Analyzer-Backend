// exception/AtsCheckException.java
package com.example.demo.exception;

public class AtsCheckException extends RuntimeException {
    public AtsCheckException(String message) {
        super(message);
    }
    public AtsCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}