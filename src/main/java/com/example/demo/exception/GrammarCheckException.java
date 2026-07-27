// exception/GrammarCheckException.java
package com.example.demo.exception;

public class GrammarCheckException extends RuntimeException {
    public GrammarCheckException(String message) {
        super(message);
    }
    public GrammarCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}